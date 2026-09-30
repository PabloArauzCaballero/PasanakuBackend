#!/usr/bin/env python3
"""Patrol usa OS=latest; un iPhone del primer runtime puede ser demasiado viejo."""

import json
import re
import sys


def seleccionar(datos: dict) -> tuple[str, str, str]:
    candidatos = []
    for runtime, dispositivos in datos.get("devices", {}).items():
        version = re.search(r"\.iOS-(\d+)-(\d+)$", runtime)
        if version is None:
            continue
        for dispositivo in dispositivos:
            nombre = dispositivo.get("name", "")
            if not nombre.startswith("iPhone") or not dispositivo.get("isAvailable", True):
                continue
            candidatos.append(
                (
                    (int(version[1]), int(version[2])),
                    nombre == "iPhone 17 Pro",
                    dispositivo["udid"],
                    nombre,
                    runtime,
                )
            )
    if not candidatos:
        raise ValueError("No hay un simulador de iPhone disponible")
    _, _, udid, nombre, runtime = max(candidatos, key=lambda item: item[:2])
    return udid, nombre, runtime


if __name__ == "__main__":
    try:
        udid, nombre, runtime = seleccionar(json.load(sys.stdin))
    except (KeyError, ValueError) as error:
        raise SystemExit(str(error)) from error
    print(f"Simulador elegido: {nombre} en {runtime} ({udid})", file=sys.stderr)
    print(udid)
