"""Cuantiza las capturas de evidencia a 256 colores (sin tramado) para que el repo no pese
tres veces más. En capturas de interfaz no se nota a simple vista; los originales se
regeneran con el arnés (`apps/movil/test/capturas/rediseno`).

Uso: python3 comprimir_capturas.py <carpeta>   (recorre la carpeta entera)
"""
import sys
from pathlib import Path

from PIL import Image


def comprimir(png: Path) -> tuple[int, int]:
    antes = png.stat().st_size
    with Image.open(png) as im:
        if im.mode == "P":
            return antes, antes
        cuantizada = im.convert("RGB").quantize(
            colors=256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE
        )
    cuantizada.save(png, optimize=True)
    return antes, png.stat().st_size


if __name__ == "__main__":
    total_antes = total_despues = n = 0
    for png in sorted(Path(sys.argv[1]).rglob("*.png")):
        a, d = comprimir(png)
        total_antes += a
        total_despues += d
        n += 1
    print(f"{n} capturas · {total_antes / 1e6:.1f} MB → {total_despues / 1e6:.1f} MB")
