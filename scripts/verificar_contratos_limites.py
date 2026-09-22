#!/usr/bin/env python3
"""
Todo schema de REQUEST tiene `additionalProperties: false` y limites explicitos
(H2.S2.M1 del carril PR4-seguridad).

    python3 scripts/verificar_contratos_limites.py             (desde la raiz)
    python3 scripts/verificar_contratos_limites.py --self-test

Por que importa: un objeto sin `additionalProperties: false` acepta cualquier campo
que el cliente mande (mass assignment, OWASP API3:2023); un `string`/`integer`/
`array` sin limite acepta un payload de cualquier tamano hasta que algo mas abajo
—memoria, la base, el proveedor de pagos— se queja primero, y para entonces ya gasto
CPU y ancho de banda del servidor.

Alcance de este barrido: solo los esquemas que un REQUEST puede llegar a construir
—el cuerpo de `requestBody` y, transitivamente, todo lo que referencia por `$ref`—,
no las respuestas. Un `SalidaCobro` sin `maxLength` en un campo de solo lectura no es
una superficie de ataque: nadie lo escribe.

Este turno (H2.S2.M1) corrige `aportes` cuando falla; en el resto de los servicios,
un hallazgo NO hace fallar el script — se imprime como pendiente para su dueno
(regla del carril: "para otros servicios, hallazgo al dueño con ruta"). Con
`--estricto` (para cuando el resto de los carriles adopten el gate), cualquier
hallazgo en cualquier servicio hace fallar.

Devuelve 1 si `aportes` (o, con `--estricto`, cualquier servicio) tiene hallazgos.
"""

from __future__ import annotations

import argparse
import sys
from dataclasses import dataclass
from pathlib import Path

import yaml

RAIZ = Path(__file__).resolve().parent.parent
SERVICIOS = RAIZ / "servicios"

# Los `format` de OpenAPI que ya acotan el tamano por si solos: un UUID no necesita
# `maxLength: 36` escrito a mano, y pedirselo solo agrega ruido que nadie lee.
FORMATOS_YA_ACOTADOS = {"uuid", "date", "date-time", "duration"}

# Servicios cuyo contrato este carril corrige de verdad. El resto es hallazgo.
CORRIGE_ESTE_CARRIL = {"aportes"}


@dataclass
class Hallazgo:
    servicio: str
    ruta: str
    mensaje: str

    def __str__(self) -> str:
        return f"{self.servicio} · {self.ruta}: {self.mensaje}"


def _schemas_referenciados_desde(nodo, spec: dict, vistos: set[str]) -> None:
    """Sigue los `$ref` de un nodo (recursivo) y acumula nombres de schema en `vistos`."""
    if isinstance(nodo, dict):
        ref = nodo.get("$ref")
        if isinstance(ref, str) and ref.startswith("#/components/schemas/"):
            nombre = ref.rsplit("/", 1)[-1]
            if nombre not in vistos:
                vistos.add(nombre)
                definicion = (spec.get("components", {}).get("schemas", {}) or {}).get(nombre, {})
                _schemas_referenciados_desde(definicion, spec, vistos)
        for valor in nodo.values():
            _schemas_referenciados_desde(valor, spec, vistos)
    elif isinstance(nodo, list):
        for item in nodo:
            _schemas_referenciados_desde(item, spec, vistos)


def _schemas_de_request(spec: dict) -> set[str]:
    """Todo schema que un `requestBody` puede llegar a construir, transitivamente."""
    vistos: set[str] = set()
    for verbos in (spec.get("paths") or {}).values():
        if not isinstance(verbos, dict):
            continue
        for op in verbos.values():
            if not isinstance(op, dict):
                continue
            cuerpo = op.get("requestBody")
            if cuerpo:
                _schemas_referenciados_desde(cuerpo, spec, vistos)
    return vistos


def _tiene_limite(propiedad: dict) -> bool:
    tipos = propiedad.get("type")
    tipos = tipos if isinstance(tipos, list) else [tipos]
    if "$ref" in propiedad or "enum" in propiedad:
        return True
    if propiedad.get("format") in FORMATOS_YA_ACOTADOS:
        return True
    if "string" in tipos:
        return "maxLength" in propiedad or "pattern" in propiedad
    if "integer" in tipos or "number" in tipos:
        return "maximum" in propiedad
    if "array" in tipos:
        return "maxItems" in propiedad
    # object, boolean, null: sin limite que declarar.
    return True


def _revisar_schema(servicio: str, nombre: str, definicion: dict) -> list[Hallazgo]:
    hallazgos = []
    ruta = f"openapi/*.yaml#/components/schemas/{nombre}"
    if definicion.get("type") == "object" and definicion.get("properties"):
        if definicion.get("additionalProperties") is not False:
            hallazgos.append(Hallazgo(servicio, ruta, "sin `additionalProperties: false` (mass assignment)"))
        for prop_nombre, prop in (definicion.get("properties") or {}).items():
            if isinstance(prop, dict) and not _tiene_limite(prop):
                hallazgos.append(
                    Hallazgo(servicio, f"{ruta}/{prop_nombre}", "sin limite (`maxLength`/`maximum`/`maxItems`)")
                )
    return hallazgos


def _revisar_paginacion(servicio: str, spec: dict) -> list[Hallazgo]:
    """Un parametro de paginacion (`pagina`, `tamano`, `limite`, `page`, `size`, `limit`)
    de tipo entero necesita un techo: sin el, pedir la pagina -1 con tamano
    2000000000 es una forma barata de tirar abajo la consulta."""
    hallazgos = []
    claves_paginacion = {"pagina", "tamano", "limite", "page", "size", "limit", "pagesize"}
    for ruta, verbos in (spec.get("paths") or {}).items():
        if not isinstance(verbos, dict):
            continue
        for verbo, op in verbos.items():
            if verbo not in ("get", "post", "put", "patch", "delete") or not isinstance(op, dict):
                continue
            for parametro in op.get("parameters") or []:
                if not isinstance(parametro, dict):
                    continue
                nombre = str(parametro.get("name", "")).lower()
                esquema = parametro.get("schema") or {}
                if nombre in claves_paginacion and esquema.get("type") in ("integer", "number"):
                    if "maximum" not in esquema:
                        hallazgos.append(
                            Hallazgo(
                                servicio,
                                f"{ruta}#{verbo}?{parametro.get('name')}",
                                "parametro de paginacion sin `maximum`",
                            )
                        )
    return hallazgos


def revisar_servicio(carpeta: Path) -> list[Hallazgo]:
    yamls = sorted(carpeta.glob("src/main/resources/openapi/*.yaml"))
    if not yamls:
        return []
    spec = yaml.safe_load(yamls[0].read_text(encoding="utf-8")) or {}
    schemas = spec.get("components", {}).get("schemas", {}) or {}

    hallazgos: list[Hallazgo] = []
    for nombre in sorted(_schemas_de_request(spec)):
        definicion = schemas.get(nombre)
        if isinstance(definicion, dict):
            hallazgos.extend(_revisar_schema(carpeta.name, nombre, definicion))
    hallazgos.extend(_revisar_paginacion(carpeta.name, spec))
    return hallazgos


def relevar() -> dict[str, list[Hallazgo]]:
    por_servicio: dict[str, list[Hallazgo]] = {}
    for carpeta in sorted(p for p in SERVICIOS.iterdir() if (p / "build.gradle.kts").is_file()):
        hallazgos = revisar_servicio(carpeta)
        if hallazgos:
            por_servicio[carpeta.name] = hallazgos
    return por_servicio


def main(argv: list[str] | None = None) -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--estricto", action="store_true", help="cualquier servicio con hallazgos hace fallar")
    args = parser.parse_args(argv)

    if args.self_test:
        return _self_test()

    por_servicio = relevar()
    total = sum(len(v) for v in por_servicio.values())
    print(f"servicios revisados con contrato OpenAPI · hallazgos totales: {total}")

    bloquean = []
    pendientes = []
    for servicio, hallazgos in por_servicio.items():
        destino = bloquean if (servicio in CORRIGE_ESTE_CARRIL or args.estricto) else pendientes
        destino.extend(hallazgos)

    if pendientes:
        print(f"\n{len(pendientes)} hallazgos en servicios sin dueño de este gate todavia (no bloquean):")
        for h in pendientes:
            print(f"  - {h}")

    if bloquean:
        print(f"\n{len(bloquean)} FALLAS (bloquean):")
        for h in bloquean:
            print(f"  - {h}")
        return 1

    print("Los contratos de request en el alcance de este gate declaran limites y additionalProperties: false.")
    return 0


def _self_test() -> int:
    fallas = []

    # additionalProperties ausente -> hallazgo
    sin_additional = {
        "type": "object",
        "properties": {"nombre": {"type": "string", "maxLength": 10}},
    }
    h = _revisar_schema("demo", "X", sin_additional)
    if not any("additionalProperties" in x.mensaje for x in h):
        fallas.append("no detecto additionalProperties ausente")

    # string sin maxLength -> hallazgo
    sin_limite = {
        "type": "object",
        "additionalProperties": False,
        "properties": {"nombre": {"type": "string"}},
    }
    h = _revisar_schema("demo", "Y", sin_limite)
    if not any("nombre" in x.ruta and "limite" in x.mensaje for x in h):
        fallas.append("no detecto string sin maxLength")

    # un schema completo y correcto -> sin hallazgos
    correcto = {
        "type": "object",
        "additionalProperties": False,
        "properties": {
            "nombre": {"type": "string", "maxLength": 10},
            "edad": {"type": "integer", "maximum": 130},
            "tags": {"type": "array", "maxItems": 5, "items": {"type": "string", "maxLength": 5}},
            "id": {"type": "string", "format": "uuid"},
        },
    }
    h = _revisar_schema("demo", "Z", correcto)
    if h:
        fallas.append(f"falso positivo en un schema correcto: {[str(x) for x in h]}")

    # paginacion sin maximum -> hallazgo
    spec = {
        "paths": {
            "/algo": {
                "get": {
                    "parameters": [{"name": "tamano", "in": "query", "schema": {"type": "integer"}}],
                }
            }
        }
    }
    h = _revisar_paginacion("demo", spec)
    if not h:
        fallas.append("no detecto parametro de paginacion sin maximum")

    if fallas:
        print(f"{len(fallas)} FALLAS en el self-test:")
        for f in fallas:
            print(f"  - {f}")
        return 1
    print("self-test: 4 PASS (additionalProperties ausente; string sin limite; schema correcto; paginacion sin techo)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
