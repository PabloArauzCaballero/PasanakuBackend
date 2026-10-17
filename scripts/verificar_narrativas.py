#!/usr/bin/env python3
"""
Gate del catálogo de datos: ninguna tabla sin explicación de negocio, y la de columnas no retrocede.

    python3 -I scripts/verificar_narrativas.py                  (todo el modelo)
    python3 -I scripts/verificar_narrativas.py --modulo 08      (un módulo, mientras se escriben sus fichas)

Devuelve 1 si algo falla. Equivale al `check:entity-narratives` de Atlas, con la fuente de este repo:
la ficha de `docs/entidades/*.md` («Qué es» y «Para qué sirve (negocio)») de cada tabla del `.puml`.

Columnas: hoy se exige un piso, no el total (ambigüedad A8 del trabajo de tableros). El piso sube a
mano cuando se agregan notas; nunca baja. Una columna nueva sin nota no rompe el gate mientras el total
no caiga por debajo del piso, pero sí aparece listada.
"""

import argparse
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from catalogo_de_datos import construir  # noqa: E402

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

# Columnas con explicación de negocio (ficha o convención) al cerrar H7. Trinquete: solo sube.
PISO_COLUMNAS = 524


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--modulo", action="append", help="Limita a uno o más módulos (01..15)")
    a = p.parse_args()
    tablas = [t for t in construir()["tablas"] if not a.modulo or t["modulo"] in a.modulo]
    fallas = 0

    sin_ficha = [t for t in tablas if not (t["negocio"]["queEs"] and t["negocio"]["negocio"])]
    for t in sin_ficha:
        print(f"  FALLA · {t['modulo']} {t['esquema']}.{t['tabla']}: sin «Qué es» y «Para qué sirve (negocio)» "
              f"en docs/entidades/")
    fallas += len(sin_ficha)
    print(f"  {'OK   ' if not sin_ficha else 'FALLA'} · tablas con ficha: {len(tablas) - len(sin_ficha)}/{len(tablas)}")

    columnas = [c for t in tablas for c in t["columnas"]]
    con_nota = sum(1 for c in columnas if c["negocio"])
    if a.modulo:
        print(f"  INFO  · columnas con explicación de negocio en el filtro: {con_nota}/{len(columnas)}")
    elif con_nota < PISO_COLUMNAS:
        fallas += 1
        print(f"  FALLA · columnas con explicación de negocio: {con_nota}/{len(columnas)}, bajó del piso {PISO_COLUMNAS}")
    else:
        print(f"  OK    · columnas con explicación de negocio: {con_nota}/{len(columnas)} (piso {PISO_COLUMNAS})")

    print(f"\n{fallas} FALLAS" if fallas else "\nSin fallas.")
    return 1 if fallas else 0


if __name__ == "__main__":
    sys.exit(main())
