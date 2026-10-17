#!/usr/bin/env python3
"""
Valida el catálogo de flujos: cada recorrido es un árbol de decisión que cierra contra el sistema real.

    python3 scripts/validar_flujos.py                 (todos los de docs/catalogo-de-flujos/)
    python3 scripts/validar_flujos.py ruta/a/flujo.yaml
    python3 scripts/validar_flujos.py --self-test      (un flujo bueno y uno malo, sin tocar el repo)

Un flujo (`docs/catalogo-de-flujos/<id>.yaml`) declara sus pasos. Cada paso es una operación HTTP del contrato
(`operacion: <operationId>`) o un disparo asíncrono (`disparador: evento | programado`, con el `componente` Java que lo
atiende; `sin-cablear` si el código existe pero nada lo dispara en producción, con su `hallazgo`, y se avisa en cada
corrida), con su transición de éxito, sus errores (código del contrato, HTTP y a dónde lleva) y sus condiciones. Al
final, los casos válidos, límite e inválidos, cada uno con la prueba que lo ejercita.

Devuelve 1 si:
  - falta un campo obligatorio o un id se repite;
  - una transición lleva a un paso que no existe (o a algo distinto de FIN);
  - un paso no es alcanzable desde el primero;
  - un `operationId` no existe en el contrato de su servicio (el paso huérfano; mismo relevamiento que
    `inventario_endpoints.py`, que ya cruza contrato e implementación);
  - un código de error no está declarado en el contrato de su servicio;
  - un componente asíncrono no existe en el servicio;
  - falta alguno de los tres niveles de casos, o un caso inválido no dice qué código, HTTP o resultado espera;
  - la prueba de un caso no existe, o el método nombrado no está en ese archivo.
"""

from __future__ import annotations

import argparse
import pathlib
import re
import sys

import yaml

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

RAIZ = pathlib.Path(__file__).resolve().parent.parent
CARPETA = RAIZ / "docs" / "catalogo-de-flujos"
FIN = "FIN"
NIVELES = ("validos", "limite", "invalidos")
AVISOS: list[str] = []


def operaciones_por_servicio() -> dict[str, set[str]]:
    from inventario_endpoints import relevar

    endpoints, _ = relevar()
    salida: dict[str, set[str]] = {}
    for e in endpoints:
        salida.setdefault(e.servicio, set()).add(e.operation_id)
    return salida


def contrato_de(servicio: str) -> str:
    candidatos = sorted((RAIZ / "servicios" / servicio / "src/main/resources/openapi").glob("*.yaml"))
    return candidatos[0].read_text(encoding="utf-8") if candidatos else ""


def componente_existe(servicio: str, componente: str) -> bool:
    return any((RAIZ / "servicios" / servicio / "src/main/java").rglob(f"{componente}.java"))


def validar(flujo: dict, origen: str, operaciones: dict[str, set[str]]) -> list[str]:
    fallas: list[str] = []

    def falla(msg: str) -> None:
        fallas.append(f"{origen}: {msg}")

    for campo in ("id", "nombre", "objetivo", "pasos", "casos"):
        if not flujo.get(campo):
            falla(f"falta `{campo}`")
    if fallas:
        return fallas

    pasos = flujo["pasos"]
    ids = [p.get("id") for p in pasos]
    for repetido in {i for i in ids if ids.count(i) > 1}:
        falla(f"paso repetido `{repetido}`")
    conocidos = set(ids)

    def destino(paso_id: str, d: object, de_donde: str) -> None:
        if d != FIN and d not in conocidos:
            falla(f"paso `{paso_id}`: {de_donde} lleva a `{d}`, que no es un paso ni FIN")

    for p in pasos:
        pid = p.get("id", "?")
        for campo in ("id", "actor", "servicio", "precondicion", "exito"):
            if not p.get(campo):
                falla(f"paso `{pid}`: falta `{campo}`")
        servicio = p.get("servicio", "")
        if p.get("operacion"):
            if p["operacion"] not in operaciones.get(servicio, set()):
                falla(f"paso `{pid}`: `{p['operacion']}` no existe en el contrato de `{servicio}` (paso huérfano)")
        elif p.get("disparador") in ("evento", "programado", "sin-cablear"):
            if not p.get("componente") or not componente_existe(servicio, p["componente"]):
                falla(f"paso `{pid}`: el componente `{p.get('componente')}` no existe en `{servicio}`")
            # El código existe pero nada lo dispara en producción: se declara con su hallazgo, no se disimula.
            if p.get("disparador") == "sin-cablear":
                if not p.get("hallazgo"):
                    falla(f"paso `{pid}`: un paso `sin-cablear` necesita su `hallazgo` escrito")
                else:
                    AVISOS.append(f"{origen}: paso `{pid}` SIN CABLEAR — {p['hallazgo']}")
        else:
            falla(f"paso `{pid}`: necesita `operacion` o `disparador: evento|programado` con `componente`")
        destino(pid, p.get("exito"), "el éxito")
        contrato = contrato_de(servicio)
        for e in p.get("errores") or []:
            if not e.get("codigo") or not e.get("http") or not e.get("siguiente"):
                falla(f"paso `{pid}`: un error necesita `codigo`, `http` y `siguiente`")
                continue
            if e["codigo"] not in contrato:
                falla(f"paso `{pid}`: el código `{e['codigo']}` no está declarado en el contrato de `{servicio}`")
            destino(pid, e["siguiente"], f"el error {e['codigo']}")
        for c in p.get("condiciones") or []:
            if not c.get("si") or not c.get("siguiente"):
                falla(f"paso `{pid}`: una condición necesita `si` y `siguiente`")
                continue
            destino(pid, c["siguiente"], f"la condición «{c['si']}»")

    alcanzados, pendientes = set(), [ids[0]]
    por_id = {p.get("id"): p for p in pasos}
    while pendientes:
        actual = pendientes.pop()
        if actual in alcanzados or actual not in por_id:
            continue
        alcanzados.add(actual)
        p = por_id[actual]
        pendientes += [p.get("exito")] + [e.get("siguiente") for e in p.get("errores") or []]
        pendientes += [c.get("siguiente") for c in p.get("condiciones") or []]
    for inalcanzable in conocidos - alcanzados:
        falla(f"paso `{inalcanzable}` no es alcanzable desde `{ids[0]}`")

    casos = flujo["casos"]
    for nivel in NIVELES:
        if not casos.get(nivel):
            falla(f"faltan los casos `{nivel}`: los tres niveles son obligatorios (regla 65)")
            continue
        for caso in casos[nivel]:
            nombre = caso.get("nombre", "?")
            for campo in ("nombre", "dado", "entonces", "prueba"):
                if not caso.get(campo):
                    falla(f"caso `{nombre}`: falta `{campo}`")
            # Un rechazo sin código de contrato (un trabajo que no hace nada, una decisión de dominio) declara el
            # resultado observable; nunca un código inventado.
            if nivel == "invalidos" and not (caso.get("codigo") or caso.get("http") or caso.get("resultado")):
                falla(f"caso inválido `{nombre}`: tiene que decir qué código, HTTP o resultado observable espera")
            if caso.get("prueba"):
                fallas += [f"{origen}: caso `{nombre}`: {m}" for m in _prueba(caso["prueba"])]
    return fallas


def _prueba(referencia: str) -> list[str]:
    archivo, _, metodo = referencia.partition("#")
    ruta = RAIZ / archivo
    if not ruta.is_file():
        return [f"la prueba `{archivo}` no existe"]
    if metodo and not re.search(rf"\b{re.escape(metodo)}\s*\(", ruta.read_text(encoding="utf-8")):
        return [f"el método `{metodo}` no está en `{archivo}`"]
    return []


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser()
    p.add_argument("archivos", nargs="*")
    p.add_argument("--self-test", action="store_true")
    a = p.parse_args(argv)
    if a.self_test:
        return _self_test()
    archivos = [pathlib.Path(x) for x in a.archivos] or sorted(CARPETA.glob("*.yaml"))
    if not archivos:
        print(f"  FALLA · no hay flujos en {CARPETA.relative_to(RAIZ)}")
        return 1
    operaciones = operaciones_por_servicio()
    fallas: list[str] = []
    for archivo in archivos:
        flujo = yaml.safe_load(archivo.read_text(encoding="utf-8")) or {}
        propias = validar(flujo, archivo.name, operaciones)
        fallas += propias
        print(f"  {'OK   ' if not propias else 'FALLA'} · {archivo.name}: {len(flujo.get('pasos') or [])} pasos, "
              + ", ".join(f"{len((flujo.get('casos') or {}).get(n) or [])} {n}" for n in NIVELES))
    for a in AVISOS:
        print(f"  AVISO · {a}")
    for f in fallas:
        print(f"  FALLA · {f}")
    print(f"\n{len(fallas)} FALLAS" if fallas else "\nSin fallas.")
    return 1 if fallas else 0


def _self_test() -> int:
    prueba = "scripts/validar_flujos.py#_self_test"
    bueno = {
        "id": "prueba", "nombre": "Flujo de prueba", "objetivo": "Probar el validador",
        "pasos": [
            {"id": "a", "actor": "SISTEMA", "servicio": "auditoria", "precondicion": "ninguna",
             "operacion": "publicarTablero", "exito": "b",
             "errores": [{"codigo": "AP-CU98-01", "http": 400, "siguiente": FIN}]},
            {"id": "b", "actor": "SISTEMA", "servicio": "auditoria", "precondicion": "a terminó",
             "disparador": "programado", "componente": "CatalogoDeDatosVigente", "exito": FIN},
        ],
        "casos": {n: [{"nombre": n, "dado": "x", "entonces": "y", "prueba": prueba, "codigo": "AP-CU98-01"}]
                  for n in NIVELES},
    }
    malo = {**bueno, "pasos": [
        {**bueno["pasos"][0], "operacion": "operacionQueNoExiste", "exito": "z"},
        {**bueno["pasos"][1], "componente": "ClaseQueNoExiste"},
    ], "casos": {"validos": bueno["casos"]["validos"]}}
    operaciones = operaciones_por_servicio()
    fallas_bueno = validar(bueno, "bueno.yaml", operaciones)
    fallas_malo = validar(malo, "malo.yaml", operaciones)
    esperadas = ("paso huérfano", "lleva a `z`", "ClaseQueNoExiste", "no es alcanzable", "faltan los casos `limite`")
    ok = not fallas_bueno and all(any(e in f for f in fallas_malo) for e in esperadas)
    print("bueno:", fallas_bueno or "sin fallas")
    print("malo:", *fallas_malo, sep="\n  ")
    print("self-test", "OK" if ok else "FALLA")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
