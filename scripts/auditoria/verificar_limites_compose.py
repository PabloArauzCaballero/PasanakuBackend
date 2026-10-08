#!/usr/bin/env python3
"""Comprueba que los compose TEST no permitan contenedores sin límites."""

from __future__ import annotations

import sys
from pathlib import Path

try:
    import yaml
except ImportError as error:  # pragma: no cover - mensaje de instalación
    raise SystemExit("Instala PyYAML con `python -m pip install pyyaml`.") from error


ROOT = Path(__file__).resolve().parents[2]
COMPOSES = {
    "docker-compose.coolify.yml": None,
    "despliegue/coolify/backoffice.yml": "backoffice",
    "despliegue/coolify/web.yml": "web",
    "despliegue/coolify/movil.yml": "movil",
}
FALLOS: list[str] = []


def exigir(condicion: bool, mensaje: str) -> None:
    if not condicion:
        FALLOS.append(mensaje)


def revisar_servicio(archivo: str, nombre: str, config: dict, usa_hikari: bool) -> None:
    ruta = f"{archivo}:{nombre}"
    deploy = config.get("deploy", {})
    limites = deploy.get("resources", {}).get("limits", {})
    memoria = config.get("mem_limit")
    cpus = config.get("cpus")
    exigir(bool(memoria), f"{ruta}: falta mem_limit explícito")
    exigir(bool(cpus), f"{ruta}: falta cpus explícito")
    exigir(str(memoria) == str(limites.get("memory")), f"{ruta}: mem_limit y deploy.resources.limits.memory difieren")
    exigir(str(cpus) == str(limites.get("cpus")), f"{ruta}: cpus y deploy.resources.limits.cpus difieren")
    if nombre != "esquema":
        exigir(deploy.get("replicas") == 1, f"{ruta}: TEST debe fijar una réplica")
    if usa_hikari:
        ambiente = config.get("environment", {})
        exigir(ambiente.get("SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE") == "5",
               f"{ruta}: Hikari TEST debe limitar maximumPoolSize a 5")
        exigir(ambiente.get("SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE") == "0",
               f"{ruta}: Hikari TEST debe limitar minimumIdle a 0")


def main() -> int:
    for archivo, servicio_esperado in COMPOSES.items():
        ruta = ROOT / archivo
        try:
            documento = yaml.safe_load(ruta.read_text(encoding="utf-8")) or {}
        except (OSError, yaml.YAMLError) as error:
            FALLOS.append(f"{archivo}: no se pudo leer YAML: {error}")
            continue
        servicios = documento.get("services", {})
        if servicio_esperado:
            config = servicios.get(servicio_esperado)
            exigir(config is not None, f"{archivo}: falta el servicio {servicio_esperado}")
            if config:
                revisar_servicio(archivo, servicio_esperado, config, usa_hikari=False)
            continue

        exigir(len(servicios) == 16, f"{archivo}: se esperaban 16 servicios, hay {len(servicios)}")
        for nombre, config in servicios.items():
            revisar_servicio(archivo, nombre, config, usa_hikari=nombre not in {"esquema", "gateway"})

    if FALLOS:
        print("Límites de Compose inválidos:")
        for fallo in FALLOS:
            print(f"- {fallo}")
        return 1
    print("Límites de memoria/CPU, una réplica Java y Hikari TEST verificados en los cuatro compose.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
