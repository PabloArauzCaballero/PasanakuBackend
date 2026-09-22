#!/usr/bin/env python3
"""Verifica que el frontend planificado coincide con la maqueta.

    python3 scripts/verificar_maqueta.py

Cuatro comprobaciones, todas contra texto que ya existe:

  1 · PANTALLAS   toda ruta que docs/Views/AportaYa-Maqueta.md lista en sus tablas
                  aparece en planes/22 (normalizada a :param), o esta en el mapa de
                  renombres de abajo, y el destino del renombre aparece en planes/22.
  2 · PIEZAS      toda pieza de planes/20 §2 esta en planes/22 §6, y toda pieza de
                  planes/22 §6 esta en planes/11 F1.9 (el alcance de F1-W / F1-M).
  3 · MUNDOS      cada pieza de planes/22 §6 tiene al menos un mundo marcado.
  4 · SIN RESTOS  los planes de frontend no nombran el stack anterior fuera de las
                  secciones que hablan de la transicion.

Sale con 1 si algo falla. Es un barrido: enumera, no revisa.
"""
import pathlib
import re
import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

R = pathlib.Path(__file__).resolve().parent.parent
MAQUETA = R / "docs/Views/AportaYa-Maqueta.md"
P11 = R / "planes/11 Fases F0 y F1 · Cimientos y sistema de diseño.md"
P20 = R / "planes/20 Maqueta de referencia · deltas del frontend.md"
P22 = R / "planes/22 Mapa de la maqueta · pantalla, carril y mundo.md"
PLANES_FRONT = [R / "planes" / n for n in (
    "10 Plan maestro del frontend.md",
    "10b Estándar de ejecución del frontend.md",
    "11 Fases F0 y F1 · Cimientos y sistema de diseño.md",
    "12 Fases F2 a F5 · App móvil.md",
    "13 Fases F6 a F8 · Backoffice.md",
    "14 Fases F9 a F11 · Sitio público, SEO y GEO.md",
    "15 Fase F12 · Endurecimiento, E2E y publicación.md",
    "22 Mapa de la maqueta · pantalla, carril y mundo.md",
)]

# Rutas de la maqueta que planes/22 escribe con otro nombre, y por que.
# La clave es la ruta de la maqueta normalizada; el valor, la ruta del plan.
RENOMBRES = {
    "aportes/mis-obligaciones": "/billetera/aportes",                       # el dominio es billetera (M2)
    "billetera/pagar-aporte": "/billetera/aportes/:id",                     # pagar es el detalle de la cuota (D-4)
    "aportes/:id/no-puedo-pagar": "/billetera/aportes/:id/no-puedo-pagar",  # bajo billetera
    "entregas/mi-turno/cobro": "/billetera/entregas/mi-turno/cobro",        # bajo billetera
    "publicidad/espacio": "/billetera/publicidad/espacio",                  # el banner vive en la portada
    "pasanaku/unirse": "/pasanaku/unirse",
    "pasanaku/reseñar": "/pasanaku/resenar/:id",                             # sin ñ en la ruta
    "pasanaku/reclamo": "/soporte/reclamos/nuevo",                           # soporte es carpeta propia (D-18)
    "pasanaku/denunciar": "/soporte/denuncias/nueva",
}

fallas = []


def check(ok, msg):
    print(f"  {'OK   ' if ok else 'FALLA'} · {msg}")
    if not ok:
        fallas.append(msg)


def normalizar(ruta):
    ruta = ruta.strip("`/ ")
    ruta = re.sub(r"\[([a-z]+)\]", r":\1", ruta)
    return ruta


def rutas_de_la_maqueta():
    t = MAQUETA.read_text(encoding="utf-8")
    rutas = set()
    for linea in t.splitlines():
        m = re.match(r"\|\s*[^|]+\|\s*`([a-z][a-z0-9/\[\]:\-ñ]+)`\s*\|", linea)
        if m and "/" in m.group(1):
            rutas.add(normalizar(m.group(1)))
    return rutas


def piezas_de(texto, inicio, fin):
    seccion = texto[texto.index(inicio):]
    if fin and fin in seccion:
        seccion = seccion[:seccion.index(fin)]
    return seccion


def main():
    print("=== 1 · PANTALLAS DE LA MAQUETA EN planes/22 ===")
    p22 = P22.read_text(encoding="utf-8")
    p22_rutas = set()
    for r in re.findall(r"`(/[a-z][a-z0-9/:\-,{} ]*)`", p22):
        r = normalizar(r)
        # `/a/{b,c}/d` en el plan cubre /a/b/d y /a/c/d
        m = re.match(r"(.*?)\{([^}]+)\}(.*)", r)
        if m:
            for alt in m.group(2).split(","):
                p22_rutas.add((m.group(1) + alt.strip() + m.group(3)).strip("/"))
        else:
            p22_rutas.add(r)
    faltan = []
    for ruta in sorted(rutas_de_la_maqueta()):
        destino = RENOMBRES.get(ruta, ruta)
        destino = normalizar(destino)
        # una ruta con llaves `{a,b}` en el plan cubre cada alternativa
        if destino in p22_rutas:
            continue
        faltan.append(f"{ruta} → {destino}")
    check(not faltan, f"toda ruta de la maqueta tiene su fila en planes/22 {faltan[:6] or ''}")

    print("=== 2 · PIEZAS ===")
    p20 = P20.read_text(encoding="utf-8")
    p11 = P11.read_text(encoding="utf-8")
    tabla20 = piezas_de(p20, "## 2 · Componentes que suma", "Y dos reglas de estilo")
    piezas20 = set(re.findall(r"^\|\s*`([A-Z][A-Za-z]+)`\s*\|", tabla20, re.M))
    tabla22 = piezas_de(p22, "## 6 · Las piezas que la maqueta fija", "Y las cuatro reglas de estilo")
    filas22 = re.findall(r"^\|\s*`([A-Z][A-Za-z]+)`[^|]*\|[^|]*\|([^|]*)\|([^|]*)\|", tabla22, re.M)
    piezas22 = {f[0] for f in filas22}
    f19 = piezas_de(p11, "## F1.9", "**Entregable F1:**")
    piezas11 = set(re.findall(r"`([A-Z][A-Za-z]+)`", f19))
    no_en_22 = sorted(piezas20 - piezas22)
    check(not no_en_22, f"toda pieza de planes/20 §2 está en planes/22 §6 {no_en_22 or ''}")
    no_en_11 = sorted(piezas22 - piezas11)
    check(not no_en_11, f"toda pieza de planes/22 §6 está en el alcance F1.9 de planes/11 {no_en_11 or ''}")

    print("=== 3 · MUNDOS ===")
    sin_mundo = [f[0] for f in filas22 if "✔" not in f[1] and "✔" not in f[2]]
    check(not sin_mundo, f"cada pieza de planes/22 §6 tiene al menos un mundo {sin_mundo or ''}")
    print(f"        {len(filas22)} piezas · {sum(1 for f in filas22 if '✔' in f[1])} Flutter · "
          f"{sum(1 for f in filas22 if '✔' in f[2])} Angular")

    print("=== 4 · SIN RESTOS DEL STACK ANTERIOR ===")
    patron = re.compile(r"\b(Expo Router|Expo Go|React Native|TanStack|Astro 5|jest-axe|expo-[a-z-]+|MSW)\b")
    permitido = re.compile(r"anterior|se rehace|se borra|Lo que había|había|Reemplaza a|pedía de Astro|Por qué Prism y no MSW|transici|F0\.0 —|Expo, React|Expo / React")
    restos = []
    for p in PLANES_FRONT:
        for n, linea in enumerate(p.read_text(encoding="utf-8").splitlines(), 1):
            if patron.search(linea) and not permitido.search(linea):
                restos.append(f"{p.name}:{n}")
    check(not restos, f"ningún plan de frontend nombra el stack anterior fuera de la transición {restos[:6] or ''}")

    print()
    if fallas:
        print(f"{len(fallas)} FALLAS")
        return 1
    print("TODO OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
