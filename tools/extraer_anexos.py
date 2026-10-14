#!/usr/bin/env python3
"""Extrae los archivos pegados en los anexos del PLAN.md y verifica su integridad."""
from __future__ import annotations

import argparse
import hashlib
import pathlib
import re
import sys

INICIO = re.compile(
    r'^<!-- ARCHIVO-INICIO ruta="(?P<ruta>[^"]+)" lineas="(?P<lineas>\d+)" sha256="(?P<sha>[0-9a-f]{64})" -->$'
)
FIN = '<!-- ARCHIVO-FIN ruta="{ruta}" -->'
VALLA = re.compile(r"^(`{3,})[A-Za-z0-9_+-]*$")


def leer_bloques(texto: str):
    lineas = texto.split("\n")
    i = 0
    while i < len(lineas):
        m = INICIO.match(lineas[i])
        if not m:
            i += 1
            continue
        ruta, esperadas, sha = m["ruta"], int(m["lineas"]), m["sha"]
        apertura = VALLA.match(lineas[i + 1])
        if not apertura:
            raise SystemExit(f"[{ruta}] falta la valla de apertura en la línea {i + 2}")
        valla = apertura.group(1)
        j = i + 2
        contenido: list[str] = []
        while j < len(lineas) and lineas[j] != valla:
            contenido.append(lineas[j])
            j += 1
        if j >= len(lineas) or lineas[j + 1] != FIN.format(ruta=ruta):
            raise SystemExit(f"[{ruta}] el bloque no cierra donde debe (línea {j + 1})")
        yield ruta, esperadas, sha, "\n".join(contenido) + "\n"
        i = j + 2


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("plan")
    p.add_argument("--prefijo", default="")
    p.add_argument("--destino", default=".")
    p.add_argument("--quitar-prefijo", default="")
    p.add_argument("--listar", action="store_true")
    a = p.parse_args()

    texto = pathlib.Path(a.plan).read_text(encoding="utf-8")
    errores = escritos = 0
    for ruta, esperadas, sha, contenido in leer_bloques(texto):
        if not ruta.startswith(a.prefijo):
            continue
        reales = contenido.count("\n")
        digest = hashlib.sha256(contenido.encode("utf-8")).hexdigest()
        ok = reales == esperadas and digest == sha
        if a.listar:
            print(f"{'OK ' if ok else 'MAL'} {esperadas:>6}  {ruta}")
            errores += 0 if ok else 1
            continue
        if not ok:
            print(f"MAL {ruta}: líneas {reales}/{esperadas}, sha {digest[:12]}/{sha[:12]}", file=sys.stderr)
            errores += 1
            continue
        relativa = ruta[len(a.quitar_prefijo):] if ruta.startswith(a.quitar_prefijo) else ruta
        salida = pathlib.Path(a.destino) / relativa
        salida.parent.mkdir(parents=True, exist_ok=True)
        salida.write_text(contenido, encoding="utf-8", newline="\n")
        escritos += 1
    if not a.listar:
        print(f"escritos: {escritos} · con error: {errores}")
    return 1 if errores else 0


if __name__ == "__main__":
    sys.exit(main())
