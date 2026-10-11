#!/usr/bin/env python3
"""Regresión contractual del frontend (H14.S1.M4): cada ruta de red que usan `apps/web` y `apps/backoffice` contra los OpenAPI.

Qué hace: lee las fuentes (no las pruebas), extrae las plantillas `${gateway}/...` (y `${this.gateway}`, `${base}`), las normaliza
(`${x}` -> `{x}`) y busca cada una —con su método— entre las rutas de `servicios/*/src/main/resources/openapi/*.yaml`.

Qué prueba: que el cliente no llama a una ruta que ningún contrato declara. Hoy hay huecos CONOCIDOS (pedidos de contrato al backend,
declarados abajo con su motivo). El gate falla si:
  · aparece una ruta SIN contrato que no está en `CONOCIDAS_SIN_CONTRATO` (un cliente nuevo llamando a algo que nadie publicó), o
  · una de las conocidas ya tiene contrato (la lista no se pudre: hay que sacarla de acá).

Uso:  python scripts/verificar_rutas_frontend.py [--tabla]     (sin Docker, sin red; lee archivos)
"""
from __future__ import annotations

import pathlib
import re
import sys

R = pathlib.Path(__file__).resolve().parent.parent
METODOS = ("get", "post", "put", "patch", "delete")

# ruta usada -> por qué no tiene contrato todavía (a quién se le pide). Clave: "MÉTODO /ruta/{x}".
CONOCIDAS_SIN_CONTRATO: dict[str, str] = {
    "GET /erp/activos": "listado de activos fijos: erp.yaml solo publica la depreciación (pedido a erp)",
    "GET /erp/cuentas-por-cobrar": "listado de cuentas por cobrar: erp.yaml solo publica abrir y cobrar (pedido a erp)",
    "GET /erp/facturas-de-proveedor": "listado de facturas: erp.yaml solo publica registrar y pagar (pedido a erp)",
    "GET /erp/ordenes-de-compra": "listado de órdenes: erp.yaml solo publica crear y aprobar (pedido a erp)",
    "GET /erp/partidas-presupuestarias": "listado de partidas: sin operación en erp.yaml (pedido a erp)",
    "GET /erp/ejercicios/{x}/periodos": "períodos de un ejercicio: sin operación en erp.yaml (pedido a erp)",
    "GET /erp/estados-financieros/{x}/documento": "descarga del documento generado: pedido pendiente al backend (cu106-estados-financieros.ts)",
    "POST /extraccion/accesos": "registro de lecturas del backoffice: sin servicio `extraccion` en este repositorio",
    "POST /extraccion/exportaciones": "exportaciones: sin servicio `extraccion` en este repositorio",
    "GET /grupos/solicitudes-escaladas": "cola de solicitudes escaladas: CU-68 no publica el listado (d15-solicitudes-escaladas.ts)",
    "GET /identidad/tutoriales/progreso": "progreso de tutoriales en servidor: sin operación en identidad.yaml (ADR-045)",
    "PUT /identidad/tutoriales/progreso/{x}": "progreso de tutoriales en servidor: sin operación en identidad.yaml (ADR-045)",
    "DELETE /identidad/tutoriales/progreso/{x}": "progreso de tutoriales en servidor: sin operación en identidad.yaml (ADR-045)",
    "GET /publicidad/socios-comerciales/{x}": "consulta de un socio: publicidad.yaml solo publica postular y verificar",
    "GET /publicidad/anunciantes": "listado de anunciantes: publicidad.yaml solo publica el alta",
    "GET /publicidad/campanas": "listado de campañas: publicidad.yaml solo publica crear, aprobar y rechazar",
    "GET /publicidad/piezas-creativas": "cola de piezas por moderar: publicidad.yaml solo publica la revisión",
    "GET /reclamos": "bandeja de reclamos: cumplimiento no publica el listado (cu52-reclamos.ts)",
    "POST /sesion/refrescar": "refresco de sesión por cookie: sin operación en identidad.yaml (refresco-de-sesion.ts)",
}

PLANTILLA = re.compile(r"`\$\{(?:this\.)?(gateway|base|GW|api)\}(/[^`?\s]*)")
LLAMADA = re.compile(r"\.(get|post|put|patch|delete)\b(?:<[^>]*>)?\(")


def rutas_del_contrato() -> list[tuple[str, str, re.Pattern[str]]]:
    """(método, ruta, regex) de cada operación de cada OpenAPI. Lee el YAML a mano: sin dependencias."""
    salida = []
    for y in sorted((R / "servicios").glob("*/src/main/resources/openapi/*.yaml")):
        en_paths = False
        ruta = None
        for linea in y.read_text(encoding="utf8").splitlines():
            if re.match(r"^paths:\s*$", linea):
                en_paths = True
                continue
            if en_paths and re.match(r"^\S", linea):
                en_paths = False
            if not en_paths:
                continue
            m = re.match(r"^  (/\S*):\s*$", linea)
            if m:
                ruta = m.group(1)
                continue
            m = re.match(r"^    (get|post|put|patch|delete):\s*$", linea)
            if m and ruta:
                rx = "^" + re.sub(r"\\\{[^}]+\\\}", "[^/]+", re.escape(ruta)) + "$"
                salida.append((m.group(1).upper(), ruta, re.compile(rx)))
    return salida


def usos_del_frontend() -> dict[tuple[str, str], list[str]]:
    usos: dict[tuple[str, str], list[str]] = {}
    for app in ("backoffice", "web"):
        for p in (R / "apps" / app / "src").rglob("*.ts"):
            if p.name.endswith(".spec.ts") or "node_modules" in p.parts or p.name.endswith(".apoyo.ts"):
                continue
            lineas = p.read_text(encoding="utf8").splitlines()
            for n, linea in enumerate(lineas, 1):
                for m in PLANTILLA.finditer(linea):
                    ruta = re.sub(r"\$\{[^}]+\}", "{x}", m.group(2))
                    antes = " ".join(lineas[max(0, n - 4) : n])
                    despues = " ".join(lineas[n - 1 : min(len(lineas), n + 4)])
                    metodo = "GET"
                    explicito = re.search(r"method:\s*'(GET|POST|PUT|PATCH|DELETE)'", despues)
                    llamadas = list(LLAMADA.finditer(antes))
                    if explicito:
                        metodo = explicito.group(1)
                    elif llamadas:
                        metodo = llamadas[-1].group(1).upper()
                    usos.setdefault((metodo, ruta), []).append(f"{p.relative_to(R).as_posix()}:{n}")
    return usos


def principal() -> int:
    contrato = rutas_del_contrato()
    usos = usos_del_frontend()
    # Una plantilla de tutoriales usa `${this.base}/${id}`: se resuelve a mano a su ruta real.
    reescritas: dict[tuple[str, str], list[str]] = {}
    for (metodo, ruta), donde in usos.items():
        if ruta == "/{x}" and any("almacen-remoto" in d for d in donde):
            ruta = "/identidad/tutoriales/progreso/{x}"
        reescritas.setdefault((metodo, ruta), []).extend(donde)
    sin = {}
    for (metodo, ruta), donde in sorted(reescritas.items(), key=lambda kv: kv[0][1]):
        ejemplo = re.sub(r"\{x\}", "x", ruta)
        if not any(m == metodo and rx.match(ejemplo) for m, _, rx in contrato):
            sin[f"{metodo} {ruta}"] = donde

    if "--tabla" in sys.argv:
        print("| método | ruta | contrato | dónde |")
        print("|---|---|---|---|")
        for (metodo, ruta), donde in sorted(reescritas.items(), key=lambda kv: kv[0][1]):
            ok = f"{metodo} {ruta}" not in sin
            print(f"| {metodo} | `{ruta}` | {'sí' if ok else '**NO**'} | {', '.join(sorted(set(donde)))[:150]} |")
        print()

    nuevas = sorted(k for k in sin if k not in CONOCIDAS_SIN_CONTRATO)
    podridas = sorted(k for k in CONOCIDAS_SIN_CONTRATO if k not in sin)
    print(f"rutas distintas que usa el frontend: {len(reescritas)} · con contrato: {len(reescritas) - len(sin)} · sin contrato: {len(sin)}")
    for k in sorted(sin):
        etiqueta = "conocida" if k in CONOCIDAS_SIN_CONTRATO else "NUEVA"
        print(f"  [{etiqueta}] {k}  ({CONOCIDAS_SIN_CONTRATO.get(k, ', '.join(sorted(set(sin[k])))[:100])})")
    fallo = False
    if nuevas:
        fallo = True
        print("FALLA · rutas que el frontend llama y ningún contrato declara (y no están en CONOCIDAS_SIN_CONTRATO):")
        for k in nuevas:
            print(f"   - {k}   {', '.join(sorted(set(sin[k])))[:120]}")
    if podridas:
        fallo = True
        print("FALLA · la lista de conocidas se pudrió: estas rutas YA tienen contrato (sacalas de CONOCIDAS_SIN_CONTRATO):")
        for k in podridas:
            print(f"   - {k}")
    print("TODO OK" if not fallo else "FALLAS")
    return 1 if fallo else 0


if __name__ == "__main__":
    sys.exit(principal())
