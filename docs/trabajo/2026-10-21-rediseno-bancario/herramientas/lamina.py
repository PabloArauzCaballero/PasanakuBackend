"""Genera evidencia/direccion/lamina.html (H1.S2.M1): la plantilla con el teléfono de Inicio
en claro y oscuro, los íconos Lucide incrustados y las fuentes reales de la app en base64.
Además mide los contrastes de los pares de color de la propuesta (WCAG 2.2).

Uso: python3 -I lamina.py <carpeta_de_iconos_lucide>
"""
import base64
import re
import sys
from pathlib import Path

AQUI = Path(__file__).resolve().parent
REPO = AQUI.parents[3]
ICONOS = Path(sys.argv[1])
FUENTES = REPO / "packages/diseno_flutter/lib/fuentes"


def icono(nombre: str, clase: str = "i") -> str:
    svg = (ICONOS / f"{nombre}.svg").read_text()
    svg = re.sub(r"<!--.*?-->", "", svg, flags=re.S).strip()
    svg = re.sub(r'class="[^"]*"', "", svg)
    return svg.replace("<svg", f'<svg class="{clase}" aria-hidden="true" focusable="false"', 1)


def fuentes() -> str:
    caras = []
    for familia, pesos in (("Bricolage", (600, 700)), ("Instrument", (400, 500, 600))):
        for peso in pesos:
            datos = base64.b64encode((FUENTES / f"{familia}-{peso}.ttf").read_bytes()).decode()
            caras.append(
                f'@font-face{{font-family:"{familia}";font-weight:{peso};font-display:block;'
                f'src:url(data:font/ttf;base64,{datos}) format("truetype")}}'
            )
    return "\n".join(caras)


AV = [("A", "#3c9366"), ("R", "#b85b13"), ("M", "#2e7fb8"), ("+6", "#5f6f65")]


def apilados() -> str:
    return '<div class="apilados">' + "".join(
        f'<span style="background:{c}">{t}</span>' for t, c in AV
    ) + "</div>"


def rueda(turno: int, total: int) -> str:
    """Rueda de turnos en miniatura: segmentos del círculo, los pasados en marca."""
    import math

    r, cx, cy, partes = 17, 21, 21, []
    for k in range(total):
        a0 = -math.pi / 2 + k * 2 * math.pi / total + 0.06
        a1 = -math.pi / 2 + (k + 1) * 2 * math.pi / total - 0.06
        x0, y0 = cx + r * math.cos(a0), cy + r * math.sin(a0)
        x1, y1 = cx + r * math.cos(a1), cy + r * math.sin(a1)
        color = "var(--brand)" if k < turno else "var(--surface-2)"
        partes.append(
            f'<path d="M{x0:.2f} {y0:.2f} A{r} {r} 0 0 1 {x1:.2f} {y1:.2f}" stroke="{color}" '
            f'stroke-width="5" fill="none" stroke-linecap="round"/>'
        )
    return f'<svg width="42" height="42" viewBox="0 0 42 42" aria-hidden="true">{"".join(partes)}</svg>'


def telefono(tema: str) -> str:
    oscuro = tema == "oscuro"
    return f"""
<figure style="margin:0">
<div class="telefono{' oscuro' if oscuro else ''}" role="img" aria-label="Inicio rediseñado, tema {tema}">
  <div class="desplazable">
    <div class="cabecera">
      <div class="avatar">AP</div>
      <div class="saludo"><span>Buenas tardes,</span><b>Ana</b></div>
      <span class="btn-ico">{icono('qr-code')}</span>
      <span class="btn-ico">{icono('bell')}<span class="punto"></span></span>
    </div>
    <div class="heroe">
      <div class="fila"><span class="etiqueta">Saldo disponible</span><span class="ojo">{icono('eye')}</span></div>
      <div class="monto"><span class="moneda">Bs</span>1.250<span class="dec">,00</span></div>
      <div class="chips">
        <span class="chip-h">{icono('trending-up')} +Bs 150 este mes</span>
        <span class="chip-h">{icono('shield-check')} En custodia</span>
      </div>
    </div>
    <div class="acciones">
      <div class="accion"><span class="circ">{icono('plus')}</span>Recargar</div>
      <div class="accion"><span class="circ">{icono('arrow-up-right')}</span>Enviar</div>
      <div class="accion principal"><span class="circ">{icono('hand-coins')}</span>Aportar</div>
      <div class="accion"><span class="circ">{icono('landmark')}</span>Retirar</div>
    </div>
    <div class="urgente">
      <div class="arriba">
        <span class="ic-tinta t-urg">{icono('calendar-clock')}</span>
        <div><h3>Tu próximo aporte</h3><p>Las Comadres · vence el jueves 24</p></div>
        <span class="cifra">Bs 250</span>
      </div>
      <button class="boton b-urg" type="button">{icono('hand-coins')} Aportar Bs 250</button>
    </div>
    <div class="sec"><h2>Tus grupos</h2><a href="#">Ver todos</a></div>
    <div class="riel">
      <div class="grupo">
        <div class="top">{rueda(4, 10)}<div><h3>Las Comadres</h3><span class="sub">Bs 250 · mensual</span></div></div>
        <div class="barra"><i style="width:40%"></i></div>
        <div class="pie"><span>Te toca en 2 turnos</span>{apilados()}</div>
      </div>
      <div class="grupo">
        <div class="top">{rueda(7, 8)}<div><h3>Oficina 2026</h3><span class="sub">Bs 500 · quincenal</span></div></div>
        <div class="barra"><i style="width:87%"></i></div>
        <div class="pie"><span>Ya cobraste</span>{apilados()}</div>
      </div>
    </div>
    <div class="sec"><h2>Movimientos</h2><a href="#">Ver todos</a></div>
    <div class="lista">
      <div class="dia">Hoy</div>
      <div class="mov"><span class="ic-tinta t-marca">{icono('users')}</span><div class="txt"><b>Aporte a Las Comadres</b><small>10:30 · confirmado</small></div><span class="imp">−Bs 250,00</span></div>
      <div class="mov"><span class="ic-tinta t-ok">{icono('arrow-down-left')}</span><div class="txt"><b>Recarga por QR</b><small>09:12 · confirmada</small></div><span class="imp entra">+Bs 500,00</span></div>
    </div>
  </div>
  <nav class="pestanas" aria-label="Pestañas">
    <span class="tab activa"><span class="ind">{icono('house')}</span>Inicio</span>
    <span class="tab"><span class="ind">{icono('users')}</span>Grupos</span>
    <span class="tab qr"><span class="circ">{icono('scan-line')}</span></span>
    <span class="tab"><span class="ind">{icono('arrow-left-right')}</span>Movimientos</span>
    <span class="tab"><span class="ind">{icono('user')}</span>Perfil</span>
  </nav>
</div>
<figcaption class="leyenda">Tema {tema} · 390 × 844</figcaption>
</figure>"""


def luminancia(hexa: str) -> float:
    r, g, b = (int(hexa[i : i + 2], 16) / 255 for i in (1, 3, 5))
    lin = lambda c: c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4  # noqa: E731
    return 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b)


def contraste(a: str, b: str) -> float:
    la, lb = sorted((luminancia(a), luminancia(b)), reverse=True)
    return (la + 0.05) / (lb + 0.05)


PARES = [
    ("claro · texto sobre héroe (g600)", "#F4FBF6", "#1C5A3A", 4.5),
    ("claro · texto 2 sobre héroe (g600)", "#B9D6C5", "#1C5A3A", 4.5),
    ("claro · botón principal", "#F4FBF6", "#1C5A3A", 4.5),
    ("claro · botón urgencia", "#FFFFFF", "#B85B13", 4.5),
    ("claro · texto 3 sobre fondo", "#5F6F65", "#F0F4F1", 4.5),
    ("claro · texto 3 sobre superficie", "#5F6F65", "#FFFFFF", 4.5),
    ("claro · urgencia-texto sobre urgencia-bg", "#8F4510", "#FDF0DF", 4.5),
    ("claro · ok sobre superficie", "#197C45", "#FFFFFF", 4.5),
    ("claro · marca sobre fondo (enlaces)", "#1C5A3A", "#F0F4F1", 4.5),
    ("oscuro · texto sobre fondo", "#EAF3ED", "#07130D", 4.5),
    ("oscuro · texto 3 sobre superficie", "#93A89B", "#10221A", 4.5),
    ("oscuro · texto 2 sobre héroe (g600)", "#B9D6C5", "#1C5A3A", 4.5),
    ("oscuro · botón principal", "#0B1F15", "#7CBE9C", 4.5),
    ("oscuro · botón urgencia", "#2A1503", "#EF9E4E", 4.5),
    ("oscuro · marca sobre fondo (enlaces)", "#7CBE9C", "#07130D", 4.5),
]

if __name__ == "__main__":
    html = (AQUI / "lamina.plantilla.html").read_text(encoding="utf-8")
    html = html.replace("/*FUENTES*/", fuentes())
    html = html.replace("<!--TELEFONO:claro-->", telefono("claro"))
    html = html.replace("<!--TELEFONO:oscuro-->", telefono("oscuro"))
    html = re.sub(r"\{\{i:([a-z-]+)\}\}", lambda m: icono(m.group(1)), html)
    salida = AQUI.parent / "evidencia/direccion/lamina.html"
    salida.parent.mkdir(parents=True, exist_ok=True)
    salida.write_text(html, encoding="utf-8")
    print(f"{salida.relative_to(REPO)} · {len(html) / 1e3:.0f} KB")
    for nombre, a, b, minimo in PARES:
        c = contraste(a, b)
        print(f"{'PASS' if c >= minimo else 'FAIL'}  {c:5.2f}:1  {nombre}")
