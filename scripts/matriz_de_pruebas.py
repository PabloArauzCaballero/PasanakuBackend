#!/usr/bin/env python3
"""
Matriz de pruebas: cada endpoint del contrato con la prueba que lo ejercita y los flujos que lo usan.

    python3 scripts/matriz_de_pruebas.py                         (todo; escribe docs/catalogo-de-flujos/matriz.md)
    python3 scripts/matriz_de_pruebas.py --servicio aportes      (solo imprime el resumen y los huecos de ese servicio)

No releva endpoints por su cuenta: usa `inventario_endpoints.relevar()`, el mismo relevamiento que el CI vigila con
`--check` (contrato ↔ implementación). Agrega tres columnas:

  - **Prueba web**: algún `*WebTest.java` del servicio pide esa ruta con ese verbo (`get("/grupos/" + id)` cuenta).
    La sábana de seguridad (`SeguridadWebTest`, 401/403 de todas las rutas) no cuenta: prueba el borde, no el contrato.
  - **CU · integración**: el CU que declara el contrato (o, si no, el que el método del controlador marca en la traza
    con `Traza.marcarCasoDeUso`) y si existe su prueba `CU<nn>*Test.java` contra Postgres real.
  - **Flujos**: los recorridos de `docs/catalogo-de-flujos/*.yaml` que lo usan como paso.

Es un tablero, no un gate: los huecos se listan para cerrarlos, no hacen fallar el build.
"""

from __future__ import annotations

import argparse
import pathlib
import re
import sys
from collections import defaultdict

import yaml

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from inventario_endpoints import relevar  # noqa: E402

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

RAIZ = pathlib.Path(__file__).resolve().parent.parent
SALIDA = RAIZ / "docs" / "catalogo-de-flujos" / "matriz.md"


VERBOS = ("get", "post", "put", "patch", "delete")
RE_LLAMADA = re.compile(r"\b(get|post|put|patch|delete)\(")
RE_CONSTANTE = re.compile(r"\bString\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*\"([^\"]*)\"\s*;")
RE_ID = re.compile(r"^(\{[^}]*\}|[0-9a-fA-F-]{8,}|\d+)$")


def normalizar_ruta(ruta: str) -> str:
    """`/grupos/{grupoId}/cupos` y `/grupos/3f2a…/cupos` son la misma ruta: todo identificador pasa a `{}`."""
    ruta = ruta.split("?", 1)[0]
    return "/".join("{}" if RE_ID.match(seg) else seg for seg in ruta.split("/"))


def _argumento(texto: str, desde: int) -> str:
    """El primer argumento de la llamada que abre en `desde`, hasta la coma o el paréntesis del mismo nivel."""
    nivel, i = 0, desde
    while i < len(texto):
        c = texto[i]
        if c == '"':
            i = texto.index('"', i + 1)
        elif c == "(":
            nivel += 1
        elif c == ")":
            if nivel == 0:
                break
            nivel -= 1
        elif c == "," and nivel == 0:
            break
        i += 1
    return texto[desde:i]


def rutas_pedidas(texto: str) -> set[tuple[str, str]]:
    """(verbo, ruta normalizada) de cada `get("…")`, `post(RUTA, id)` o `get("/grupos/" + id + "/cupos")` del archivo."""
    constantes = dict(RE_CONSTANTE.findall(texto))
    salida = set()
    for m in RE_LLAMADA.finditer(texto):
        partes = []
        for token in _argumento(texto, m.end()).split("+"):
            token = token.strip()
            if token.startswith('"') and token.endswith('"'):
                partes.append(token[1:-1])
            elif token in constantes:
                partes.append(constantes[token])
            else:
                partes.append("{}")
        ruta = "".join(partes)
        if ruta.startswith("/"):
            salida.add((m.group(1), normalizar_ruta(ruta)))
    return salida


def _pruebas_web(servicio: str) -> set[tuple[str, str]]:
    pedidas: set[tuple[str, str]] = set()
    for p in (RAIZ / "servicios" / servicio / "src/test").rglob("*WebTest.java"):
        if p.name != "SeguridadWebTest.java":
            pedidas |= rutas_pedidas(p.read_text(encoding="utf-8"))
    return pedidas


def tiene_prueba_web(pedidas: set[tuple[str, str]], metodo: str, ruta: str) -> bool:
    return (metodo.lower(), normalizar_ruta(ruta)) in pedidas


RE_METODO = re.compile(r"public\s+ResponseEntity\s*<.*?>\s+(\w+)\s*\(")
RE_CU_EN_TRAZA = re.compile(r"marcarCasoDeUso\(\s*\"(CU-\d+)")


def _cu_de_controladores(servicio: str) -> dict[str, str]:
    """operationId → CU que el método del controlador marca en la traza (`Traza.marcarCasoDeUso("CU-21", …)`)."""
    salida: dict[str, str] = {}
    for p in (RAIZ / "servicios" / servicio / "src/main/java").rglob("*Controller.java"):
        texto = p.read_text(encoding="utf-8")
        metodos = list(RE_METODO.finditer(texto))
        for i, m in enumerate(metodos):
            cuerpo = texto[m.end(): metodos[i + 1].start() if i + 1 < len(metodos) else len(texto)]
            if cu := RE_CU_EN_TRAZA.search(cuerpo):
                salida.setdefault(m.group(1), cu.group(1))
    return salida


def _pruebas_de_cu(servicio: str) -> set[str]:
    return {m.group(1) for p in (RAIZ / "servicios" / servicio / "src/test").rglob("CU*Test.java")
            if (m := re.match(r"CU(\d+)", p.name))}


def _flujos() -> dict[tuple[str, str], list[str]]:
    usos: dict[tuple[str, str], list[str]] = defaultdict(list)
    for archivo in sorted((RAIZ / "docs" / "catalogo-de-flujos").glob("*.yaml")):
        flujo = yaml.safe_load(archivo.read_text(encoding="utf-8")) or {}
        for paso in flujo.get("pasos") or []:
            if paso.get("operacion"):
                usos[(paso.get("servicio"), paso["operacion"])].append(flujo.get("id", archivo.stem))
    return usos


def construir(servicio: str | None = None) -> list[dict]:
    endpoints, _ = relevar()
    flujos = _flujos()
    web, cus, trazas = {}, {}, {}
    filas = []
    for e in sorted(endpoints, key=lambda x: (x.servicio, x.ruta, x.metodo)):
        if servicio and e.servicio != servicio:
            continue
        web.setdefault(e.servicio, _pruebas_web(e.servicio))
        cus.setdefault(e.servicio, _pruebas_de_cu(e.servicio))
        trazas.setdefault(e.servicio, _cu_de_controladores(e.servicio))
        cu = e.cu or trazas[e.servicio].get(e.operation_id)
        numero = cu.split("-")[1].lstrip("0") if cu else None
        filas.append({
            "servicio": e.servicio, "metodo": e.metodo, "ruta": e.ruta, "operacion": e.operation_id,
            "web": tiene_prueba_web(web[e.servicio], e.metodo, e.ruta), "cu": cu,
            "integracion": bool(numero) and any(c.lstrip("0") == numero for c in cus[e.servicio]),
            "flujos": sorted(set(flujos.get((e.servicio, e.operation_id), []))),
        })
    return filas


def resumen(filas: list[dict]) -> list[str]:
    por_servicio: dict[str, list[dict]] = defaultdict(list)
    for f in filas:
        por_servicio[f["servicio"]].append(f)
    L = ["| Servicio | Endpoints | Con prueba web | Con CU | CU con prueba de integración | En algún flujo |",
         "|---|---:|---:|---:|---:|---:|"]
    for s, fs in sorted(por_servicio.items()) + [("**Total**", filas)]:
        L.append(f"| {s} | {len(fs)} | {sum(f['web'] for f in fs)} | {sum(bool(f['cu']) for f in fs)} | "
                 f"{sum(f['integracion'] for f in fs)} | {sum(bool(f['flujos']) for f in fs)} |")
    return L


def documento(filas: list[dict]) -> str:
    L = ["# Matriz de pruebas por endpoint", "",
         "> Generada por `scripts/matriz_de_pruebas.py` desde el relevamiento de `inventario_endpoints.py`. No se edita a",
         "> mano. «Prueba web» = un `*WebTest` pide esa ruta con ese verbo (la sábana de seguridad no cuenta).", "",
         "## Resumen", "", *resumen(filas), "", "## Por endpoint", "",
         "| Servicio | Método | Ruta | operationId | Prueba web | CU | Integración del CU | Flujos |",
         "|---|---|---|---|---|---|---|---|"]
    for f in filas:
        L.append(f"| {f['servicio']} | {f['metodo']} | `{f['ruta']}` | `{f['operacion']}` | {'sí' if f['web'] else '**no**'} | "
                 f"{f['cu'] or '—'} | {'sí' if f['integracion'] else ('**no**' if f['cu'] else '—')} | "
                 f"{', '.join(f['flujos']) or '—'} |")
    return "\n".join(L) + "\n"


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--servicio")
    a = p.parse_args(argv)
    filas = construir(a.servicio)
    print("\n".join(resumen(filas)))
    if a.servicio:
        huecos = [f for f in filas if not f["web"]]
        print(f"\nSin prueba web ({len(huecos)}):")
        for f in huecos:
            print(f"  {f['metodo']} {f['ruta']}  ({f['operacion']})")
    else:
        SALIDA.parent.mkdir(parents=True, exist_ok=True)
        SALIDA.write_text(documento(filas), encoding="utf-8")
        print(f"\n{len(filas)} endpoints → {SALIDA.relative_to(RAIZ)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
