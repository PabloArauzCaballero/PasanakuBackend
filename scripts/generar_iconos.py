#!/usr/bin/env python3
"""
Genera TODOS los íconos de AportaYa desde el símbolo de la marca.

    python3 scripts/generar_iconos.py

Una fuente —el símbolo de landing/assets/img/simbolo.svg y los colores de
packages/tokens/tokens.json— y todas las salidas: la app en iOS y Android, la app en la
web, y los favicons del sitio y del backoffice. Hasta que existió, la app se instalaba
con el logo de ejemplo de Flutter.

Necesita rsvg-convert (librsvg) y magick (ImageMagick).

Decisiones que no son de gusto:
- iOS: cuadrado completo y SIN canal alfa. El sistema pone la máscara redondeada, y App
  Store rechaza el ícono de 1024 si trae transparencia.
- Android y la web «maskable»: el símbolo dentro de la zona segura (80 % del lado), para
  que ninguna máscara circular o de gota le corte las patas.
- Favicons: el cuadro verde y no el símbolo suelto, porque el trazo fino se pierde a 16 px
  sobre una pestaña clara u oscura.
"""
import json
import pathlib
import subprocess
import tempfile

R = pathlib.Path(__file__).resolve().parent.parent
color = json.loads((R / "packages/tokens/tokens.json").read_text())["primitivas"]["color"]
FONDO, TRAZO, ACENTO = color["g700"], color["crema"], color["o500"]

# Los trazos del símbolo, tal cual la landing (viewBox 0 0 200 200).
PATAS = ["M92 30 C74 66 52 108 40 168", "M108 30 C126 66 148 108 160 168"]
BASE = "M46 158 C86 140 114 140 154 158"
AGUAYO = ["M100 58 C84 92 68 130 62 170", "M100 58 C116 92 132 130 138 170"]
ARCO = "M70 150 C100 134 100 134 130 150"


def svg(lado_simbolo: float, radio: float) -> str:
    """El ícono en un lienzo de 200: fondo con `radio` y el símbolo ocupando `lado_simbolo`."""
    escala = lado_simbolo / 200
    corrimiento = (200 - lado_simbolo) / 2
    trazos = "".join(
        f'<path d="{d}" stroke="{TRAZO}" stroke-width="13"/>' for d in PATAS
    ) + f'<path d="{BASE}" stroke="{TRAZO}" stroke-width="12"/>' + "".join(
        f'<path d="{d}" stroke="{ACENTO}" stroke-width="12"/>' for d in AGUAYO
    ) + f'<path d="{ARCO}" stroke="{ACENTO}" stroke-width="10"/>'
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">'
        f'<rect width="200" height="200" rx="{radio}" fill="{FONDO}"/>'
        f'<g fill="none" stroke-linecap="round" transform="translate({corrimiento} {corrimiento}) scale({escala})">{trazos}</g>'
        f"</svg>"
    )


def png(fuente: str, salida: pathlib.Path, lado: int, sin_alfa: bool = False) -> None:
    salida.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile("w", suffix=".svg", delete=False) as f:
        f.write(fuente)
    subprocess.run(["rsvg-convert", "-w", str(lado), "-h", str(lado), "-o", str(salida), f.name], check=True)
    if sin_alfa:
        subprocess.run(["magick", str(salida), "-background", FONDO, "-alpha", "remove", "-alpha", "off", str(salida)], check=True)


COMPLETO = svg(lado_simbolo=150, radio=0)       # iOS y web «any»: el sistema redondea
REDONDEADO = svg(lado_simbolo=150, radio=44)    # Android clásico y favicons
SEGURO = svg(lado_simbolo=112, radio=0)         # maskable: el símbolo dentro del 80 %

hechos = 0

# iOS: los tamaños salen del Contents.json del propio proyecto, no de una lista a mano.
ios = R / "apps/movil/ios/Runner/Assets.xcassets/AppIcon.appiconset"
for imagen in json.loads((ios / "Contents.json").read_text())["images"]:
    lado = round(float(imagen["size"].split("x")[0]) * int(imagen["scale"].rstrip("x")))
    png(COMPLETO, ios / imagen["filename"], lado, sin_alfa=True)
    hechos += 1

# Android: el lanzador clásico por densidad.
for carpeta, lado in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    png(REDONDEADO, R / f"apps/movil/android/app/src/main/res/mipmap-{carpeta}/ic_launcher.png", lado)
    hechos += 1

# La app en la web (manifest.json).
web = R / "apps/movil/web"
for lado in (192, 512):
    png(COMPLETO, web / f"icons/Icon-{lado}.png", lado)
    png(SEGURO, web / f"icons/Icon-maskable-{lado}.png", lado)
    hechos += 2
png(REDONDEADO, web / "favicon.png", 64)
hechos += 1

# Favicons del sitio y del backoffice: un .ico con 16, 32 y 48.
with tempfile.TemporaryDirectory() as tmp:
    capas = []
    for lado in (16, 32, 48):
        capa = pathlib.Path(tmp) / f"{lado}.png"
        png(REDONDEADO, capa, lado)
        capas.append(str(capa))
    for app in ("web", "backoffice"):
        subprocess.run(["magick", *capas, str(R / f"apps/{app}/public/favicon.ico")], check=True)
        hechos += 1

print(f"íconos generados: {hechos} · fondo {FONDO}, trazo {TRAZO}, acento {ACENTO}")
