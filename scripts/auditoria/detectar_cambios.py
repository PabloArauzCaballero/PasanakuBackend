#!/usr/bin/env python3
"""Clasifica rutas cambiadas según el mapa de impacto de CI."""

from __future__ import annotations

import argparse
import fnmatch
import subprocess
import sys
from pathlib import Path

try:
    import yaml
except ImportError as error:  # pragma: no cover - mensaje de instalación
    raise SystemExit("Instala PyYAML con `python -m pip install pyyaml`.") from error


ROOT = Path(__file__).resolve().parents[2]
MAPA = ROOT / "docs/Arquitectura/dependency-impact.yaml"
TODAS = (
    "backend-core",
    "backend-service",
    "database",
    "contracts",
    "frontend-web",
    "frontend-backoffice",
    "frontend-mobile",
    "shared-packages",
    "infra",
    "docs",
    "dependencies",
)


def rutas_cambiadas(base: str, head: str) -> list[str]:
    salida = subprocess.check_output(
        ["git", "-C", str(ROOT), "diff", "--name-only", f"{base}...{head}"],
        text=True,
    )
    return [line.replace("\\", "/") for line in salida.splitlines() if line]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base")
    parser.add_argument("--head", default="HEAD")
    parser.add_argument("--all", action="store_true", help="Activa todos los grupos")
    parser.add_argument("--output", help="Archivo GITHUB_OUTPUT (opcional)")
    args = parser.parse_args()

    if args.all:
        paths: list[str] = []
        categories = set(TODAS)
    elif args.base:
        paths = rutas_cambiadas(args.base, args.head)
        mapa = yaml.safe_load(MAPA.read_text(encoding="utf-8"))["categories"]
        categories = {
            category
            for path in paths
            for category, patterns in mapa.items()
            if any(fnmatch.fnmatchcase(path, pattern) for pattern in patterns)
        }
    else:
        parser.error("usa --all o define --base y --head")

    backend = bool(categories & {"backend-core", "backend-service", "database", "contracts", "infra"})
    frontend = bool(categories & {
        "frontend-web", "frontend-backoffice", "frontend-mobile", "shared-packages", "contracts"
    }) or any(
        path.startswith("despliegue/coolify/") or path == ".github/workflows/ci.yml" for path in paths
    )
    base = bool(categories & {"database", "backend-core", "backend-service", "infra"})
    contracts = "contracts" in categories
    mobile = "frontend-mobile" in categories or any(
        path.startswith(("packages/diseno_flutter/", "packages/tokens/")) for path in paths
    )
    images = bool(categories & {"backend-core", "backend-service", "database", "infra"})
    security = bool(categories - {"docs"})
    outputs = {
        "backend": str(backend).lower(),
        "frontend": str(frontend).lower(),
        "base": str(base).lower(),
        "contracts": str(contracts).lower(),
        "mobile": str(mobile).lower(),
        "images": str(images).lower(),
        "security": str(security).lower(),
        "dependencies": str("dependencies" in categories).lower(),
        "categories": ",".join(sorted(categories)),
    }
    summary = "\n".join(f"{key}={value}" for key, value in outputs.items())
    if args.output:
        with open(args.output, "a", encoding="utf-8") as target:
            target.write(summary + "\n")
    else:
        print(summary)
    print(f"Archivos: {len(paths)}; categorías: {outputs['categories'] or 'ninguna'}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
