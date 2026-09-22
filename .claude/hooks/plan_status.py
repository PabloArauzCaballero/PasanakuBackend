"""Estado de los PLAN.md: conteos por estado y porcentaje CALCULADO.

No es un hook: es la herramienta que usan las personas y los otros scripts para saber
como viene un trabajo sin estimar a ojo. La regla 20 prohibe los porcentajes inventados;
la unica cifra valida es `microtareas HECHO / total`, y sale de aca.

Uso:
    python plan_status.py                  # todos los trabajos bajo docs/trabajo/
    python plan_status.py --json           # misma informacion para consumo programatico
    python plan_status.py --path docs/trabajo/2026-01-01-x/PLAN.md
    python plan_status.py --root <carpeta> # raiz distinta (util en pruebas)
    python plan_status.py --self-test
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from plan_lib import (  # noqa: E402
    ESTADOS,
    ESTADO_DESCONOCIDO,
    GLOB_PLANES,
    buscar_planes,
    leer,
    parse_plan,
    resumen,
    secciones_faltantes,
)


def _raiz(valor: str | None) -> Path:
    bruto = valor or os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()
    return Path(bruto).resolve()


def recolectar(raiz: Path, rutas: list[Path]) -> dict:
    trabajos = []
    for ruta in rutas:
        plan = parse_plan(leer(ruta))
        res = resumen(plan)
        reporte = ruta.parent / "REPORTE.md"
        existe = reporte.is_file()
        try:
            relativa = ruta.relative_to(raiz).as_posix()
        except ValueError:
            relativa = ruta.as_posix()
        trabajos.append(
            {
                "plan": relativa,
                "total": res["total"],
                "hechas": res["hechas"],
                "porcentaje": res["porcentaje"],
                "por_estado": {k: v for k, v in res["por_estado"].items() if v},
                "activas": [{"id": m["id"], "estado": m["estado"]} for m in res["activas"]],
                "incompletas": [{"id": m["id"], "estado": m["estado"]} for m in res["incompletas"]],
                "reporte_existe": existe,
                "reporte_faltantes": secciones_faltantes(leer(reporte)) if existe else None,
                "malformado": plan["malformado"],
                "razones": plan["razones"],
            }
        )

    total = sum(t["total"] for t in trabajos)
    hechas = sum(t["hechas"] for t in trabajos)
    return {
        "raiz": raiz.as_posix(),
        "trabajos": trabajos,
        "global": {
            "total": total,
            "hechas": hechas,
            "porcentaje": round(100.0 * hechas / total, 1) if total else 0.0,
        },
    }


def imprimir_humano(datos: dict) -> None:
    trabajos = datos["trabajos"]
    if not trabajos:
        print("No hay ningun PLAN.md bajo docs/trabajo/.")
        print("La regla 20 exige crear el plan antes de escribir codigo.")
        return

    for t in trabajos:
        print(f"\n{t['plan']}")
        # Si hubo filas que no se pudieron parsear, el denominador esta incompleto y el
        # porcentaje miente. La regla 50 exige que salga de HECHO/total: si el total no es
        # confiable, hay que decirlo en la misma linea, no en una nota al pie que se pasa por alto.
        aviso = ""
        if t["malformado"]:
            ignoradas = sum(1 for r in t["razones"] if r.startswith("fila ignorada"))
            if ignoradas:
                aviso = f"  <-- NO CONFIABLE: {ignoradas} fila(s) sin contar"
        print(f"  Avance: {t['hechas']}/{t['total']} microtareas HECHO  ({t['porcentaje']}%){aviso}")
        if t["por_estado"]:
            detalle = "  ".join(f"{k}={v}" for k, v in t["por_estado"].items())
            print(f"  Estados: {detalle}")
        if t["incompletas"]:
            print("  Sin cerrar:")
            for m in t["incompletas"]:
                print(f"    - {m['id']}: {m['estado']}")
        if t["reporte_existe"] is False:
            print("  REPORTE.md: FALTA")
        elif t["reporte_faltantes"]:
            print(f"  REPORTE.md: incompleto, faltan {', '.join(t['reporte_faltantes'])}")
        else:
            print("  REPORTE.md: completo")
        if t["malformado"]:
            print("  Avisos de formato:")
            for r in t["razones"]:
                print(f"    ! {r}")

    g = datos["global"]
    print(f"\nTOTAL: {g['hechas']}/{g['total']} microtareas HECHO ({g['porcentaje']}%)")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="plan_status.py",
        description="Estado de los PLAN.md con porcentaje calculado (regla 20).",
    )
    parser.add_argument("--json", action="store_true", help="salida JSON")
    parser.add_argument("--path", help="un PLAN.md puntual en vez de todos")
    parser.add_argument("--root", help="raiz del proyecto (por defecto CLAUDE_PROJECT_DIR o cwd)")
    parser.add_argument("--self-test", action="store_true", help="corre las pruebas internas")
    args = parser.parse_args(argv)

    if args.self_test:
        return self_test()

    try:
        raiz = _raiz(args.root)
        if args.path:
            ruta = Path(args.path)
            if not ruta.is_absolute():
                ruta = raiz / ruta
            rutas = [ruta] if ruta.is_file() else []
        else:
            rutas = buscar_planes(raiz, GLOB_PLANES)

        datos = recolectar(raiz, rutas)
        if args.json:
            print(json.dumps(datos, ensure_ascii=False, indent=1))
        else:
            imprimir_humano(datos)
        return 0
    except Exception as exc:
        print(f"plan_status: error ({exc})", file=sys.stderr)
        return 1


# --- Autoprueba -----------------------------------------------------------------

PLAN_MIXTO = """# Plan - demo

## H1 - hito uno
**Estado:** EN CURSO

### H1.S1 - subtarea
**Estado:** EN CURSO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | a | ... | `cmd` | HECHO |
| H1.S1.M2 | b | ... | `cmd` | A MEDIAS |
| H1.S1.M3 | c | ... | `cmd` | BLOQUEADO |
| H1.S1.M4 | d | ... | `cmd` | TODO |
| H1.S1.M5 | e | ... | `cmd` | DESCARTADO |
"""


def self_test() -> int:
    import tempfile

    ok: list[str] = []
    fallos: list[str] = []

    def chequear(nombre, obtenido, esperado):
        if obtenido == esperado:
            ok.append(nombre)
        else:
            fallos.append(f"{nombre}: esperado {esperado!r}, obtenido {obtenido!r}")

    # --- Parser ---
    plan = parse_plan(PLAN_MIXTO)
    res = resumen(plan)
    chequear("detecta las 5 microtareas", res["total"], 5)
    chequear("cuenta 1 HECHO", res["por_estado"]["HECHO"], 1)
    chequear("porcentaje calculado 1/5", res["porcentaje"], 20.0)
    chequear("activas = solo TODO", [m["id"] for m in res["activas"]], ["H1.S1.M4"])
    chequear(
        "incompletas = EN CURSO/A MEDIAS/BLOQUEADO",
        [m["id"] for m in res["incompletas"]],
        ["H1.S1.M2", "H1.S1.M3"],
    )
    chequear("lee el estado del hito", plan["hitos"][0]["estado"], "EN CURSO")
    chequear("lee el estado de la subtarea", plan["subtareas"][0]["estado"], "EN CURSO")

    # --- Normalizacion de estados escritos de cualquier forma ---
    variantes = parse_plan(
        "| ID | t | ca | dod | Estado |\n|---|---|---|---|---|\n"
        "| H1.S1.M1 | a | x | y | `a medias` |\n"
        "| H1.S1.M2 | b | x | y | **Hecho** |\n"
        "| H1.S1.M3 | c | x | y | Bloqueado |\n"
    )
    chequear(
        "normaliza acentos, backticks y mayusculas",
        [m["estado"] for m in variantes["microtareas"]],
        ["A MEDIAS", "HECHO", "BLOQUEADO"],
    )

    # --- Tolerancia a basura ---
    malo = parse_plan("esto no es un plan")
    chequear("plan sin microtareas se marca malformado", malo["malformado"], True)
    chequear("plan sin microtareas no lanza", malo["microtareas"], [])
    chequear("resumen de plan vacio no divide por cero", resumen(malo)["porcentaje"], 0.0)

    sin_estado = parse_plan(
        "| ID | t | ca | dod | Estado |\n|---|---|---|---|---|\n| H1.S1.M1 | a | x | y | ??? |\n"
    )
    chequear("fila sin estado valido -> DESCONOCIDO",
             sin_estado["microtareas"][0]["estado"], ESTADO_DESCONOCIDO)
    chequear("fila sin estado valido deja razon", len(sin_estado["razones"]) > 0, True)

    # --- Secciones del reporte ---
    chequear(
        "detecta las tres secciones",
        secciones_faltantes("## Completado\n## A medias\n## Pendiente\n"),
        [],
    )
    chequear(
        "detecta seccion faltante",
        secciones_faltantes("## Completado\n## Pendiente\n"),
        ["A MEDIAS"],
    )
    chequear(
        "acepta encabezados con acento y mayusculas",
        secciones_faltantes("# COMPLETADO\n### a medias\n## Pendientes varios\n"),
        [],
    )

    # --- Recoleccion sobre disco ---
    with tempfile.TemporaryDirectory() as tmp:
        raiz = Path(tmp).resolve()
        carpeta = raiz / "docs" / "trabajo" / "2026-01-01-demo"
        carpeta.mkdir(parents=True, exist_ok=True)
        (carpeta / "PLAN.md").write_text(PLAN_MIXTO, encoding="utf-8")
        datos = recolectar(raiz, buscar_planes(raiz, GLOB_PLANES))
        chequear("encuentra el plan en disco", len(datos["trabajos"]), 1)
        chequear("porcentaje global", datos["global"]["porcentaje"], 20.0)
        chequear("detecta reporte ausente", datos["trabajos"][0]["reporte_existe"], False)
        chequear("salida JSON serializa", isinstance(json.dumps(datos), str), True)

        vacio = recolectar(raiz, [])
        chequear("sin planes: global en cero", vacio["global"]["total"], 0)

    chequear("vocabulario de estados cerrado", len(ESTADOS), 6)

    # --- Lectura del evento del hook (regresion: BOM de PowerShell) -------------
    # PowerShell antepone \xef\xbb\xbf y json.load falla. Si esto se rompe, los dos
    # candados quedan INERTES en Windows sin aviso. No borrar estas pruebas.
    import io as _io

    from plan_lib import leer_evento

    def _stdin(datos: bytes):
        envoltura = _io.TextIOWrapper(_io.BytesIO(datos), encoding="utf-8", errors="replace")
        return envoltura

    ev, err = leer_evento(_stdin(b'{"tool_name":"Write"}'))
    chequear("evento JSON limpio", (ev or {}).get("tool_name"), "Write")
    chequear("evento limpio sin error", err, "")

    ev, err = leer_evento(_stdin(b'\xef\xbb\xbf{"tool_name":"Write"}'))
    chequear("evento con BOM UTF-8 (PowerShell)", (ev or {}).get("tool_name"), "Write")

    ev, err = leer_evento(_stdin(b'\xef\xbb\xbf{"tool_name":"Write"}\r\n'))
    chequear("evento con BOM + CRLF", (ev or {}).get("tool_name"), "Write")

    ev, err = leer_evento(_stdin(b"no soy json"))
    chequear("basura -> evento None", ev, None)
    chequear("basura -> deja motivo para stderr", bool(err), True)

    ev, err = leer_evento(_stdin(b""))
    chequear("stdin vacio -> motivo explicito", err, "stdin vacio")

    ev, err = leer_evento(_stdin(b"[1,2,3]"))
    chequear("JSON que no es objeto -> rechazado", ev, None)

    for nombre in ok:
        print(f"PASS  {nombre}")
    for f in fallos:
        print(f"FAIL  {f}")
    print(f"\nplan_status self-test: {len(ok)} PASS, {len(fallos)} FAIL")
    return 1 if fallos else 0


if __name__ == "__main__":
    sys.exit(main())
