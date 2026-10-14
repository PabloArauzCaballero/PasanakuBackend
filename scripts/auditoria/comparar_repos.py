#!/usr/bin/env python3
"""Compara archivos versionados de PasanakuBackend y PasanakuFrontend."""

from __future__ import annotations

import argparse
import hashlib
import subprocess
from pathlib import Path


def tracked_files(root: Path) -> dict[str, tuple[str, int]]:
    raw = subprocess.check_output(
        ["git", "-C", str(root), "ls-files", "-z"], stderr=subprocess.STDOUT
    )
    files: dict[str, tuple[str, int]] = {}
    for item in raw.decode("utf-8", errors="surrogateescape").split("\0"):
        if not item:
            continue
        path = root / item
        if not path.is_file():
            continue
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        files[item.replace("\\", "/")] = (digest, path.stat().st_size)
    return files


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("backend", type=Path, help="Ruta al checkout Backend")
    parser.add_argument("frontend", type=Path, help="Ruta al checkout Frontend")
    parser.add_argument("--output", type=Path, help="Archivo Markdown de salida")
    args = parser.parse_args()

    backend_root = args.backend.resolve()
    frontend_root = args.frontend.resolve()
    backend = tracked_files(backend_root)
    frontend = tracked_files(frontend_root)
    lines = [
        "# Comparación de archivos versionados",
        "",
        f"- Backend: `{backend_root}` ({len(backend)} archivos)",
        f"- Frontend: `{frontend_root}` ({len(frontend)} archivos)",
        "",
        "| Estado | Ruta | SHA-256 Backend | SHA-256 Frontend | Bytes Backend | Bytes Frontend |",
        "|---|---|---|---|---:|---:|",
    ]
    for name in sorted(backend.keys() | frontend.keys()):
        left = backend.get(name)
        right = frontend.get(name)
        if left and right:
            state = "idéntico" if left[0] == right[0] else "modificado"
        elif left:
            state = "solo Backend"
        else:
            state = "solo Frontend"
        lines.append(
            f"| {state} | `{name}` | {left[0] if left else '—'} | "
            f"{right[0] if right else '—'} | {left[1] if left else '—'} | "
            f"{right[1] if right else '—'} |"
        )
    content = "\n".join(lines) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(content, encoding="utf-8")
    else:
        print(content, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
