#!/usr/bin/env python3
"""
La matriz de cobertura contra la criticidad que ADR-026 le asigna a cada ambito.

    ./gradlew test webTest integrationTest      (primero: sin datos no hay medida)
    ./gradlew jacocoTestReport
    python3 scripts/cobertura.py

No falla el build — eso lo hace `./gradlew cobertura`, con los pisos declarados por
cada modulo. Este informe existe para lo otro: **fijar esos pisos desde evidencia**,
en vez de copiar un numero de un documento.

Lee `build/reports/jacoco/test/jacocoTestReport.xml` de cada modulo. Ese informe se
arma con los `.exec` de TODOS los corredores, asi que un modulo medido sin correr
`integrationTest` aparece muy por debajo de lo real: los casos de uso viven ahi. El
informe avisa cuando detecta que falto un corredor, porque un numero bajo por no haber
corrido las pruebas es la forma mas facil de fijar un piso equivocado.

Los pisos de ADR-026:

    global            80 % lineas · 70 % ramas
    dominio/          95 % lineas y ramas, en los servicios de dinero y cumplimiento
    aplicacion/       90 % lineas, en esos mismos
"""
import pathlib
import sys
import xml.etree.ElementTree as ET

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

R = pathlib.Path(__file__).resolve().parent.parent

# ADR-026 §Cobertura como piso.
PISO_GLOBAL = (0.80, 0.70)
PISO_DOMINIO = (0.95, 0.95)
PISO_APLICACION = (0.90, 0.0)

# Donde vive el dinero y el cumplimiento: los que llevan el piso alto.
CRITICOS = {
    "aportes",
    "cumplimiento",
    "entregas",
    "erp",
    "garantia",
    "nucleo-financiero",
    "tarifas",
}

CORREDORES = ["test", "webTest", "integrationTest", "contractTest", "sagaTest"]


def porcentaje(nodo, tipo):
    for c in nodo.findall("counter"):
        if c.get("type") == tipo:
            perdidas, cubiertas = int(c.get("missed")), int(c.get("covered"))
            total = perdidas + cubiertas
            return cubiertas / total if total else None
    return None


def ambito(raiz, sufijo):
    """Agrega todos los paquetes cuyo nombre termina en `sufijo` (o lo contiene)."""
    perdidas = {"LINE": 0, "BRANCH": 0}
    cubiertas = {"LINE": 0, "BRANCH": 0}
    hubo = False
    for paquete in raiz.findall("package"):
        nombre = paquete.get("name", "")
        if not (nombre.endswith("/" + sufijo) or ("/" + sufijo + "/") in nombre):
            continue
        hubo = True
        for c in paquete.findall("counter"):
            if c.get("type") in perdidas:
                perdidas[c.get("type")] += int(c.get("missed"))
                cubiertas[c.get("type")] += int(c.get("covered"))
    if not hubo:
        return None, None
    def pct(t):
        total = perdidas[t] + cubiertas[t]
        return cubiertas[t] / total if total else None
    return pct("LINE"), pct("BRANCH")


def marca(valor, piso):
    if valor is None:
        return "     —"
    texto = f"{valor * 100:5.1f}%"
    if piso and valor < piso:
        return texto + " ✗"
    return texto + "  "


def main():
    informes = sorted(R.glob("*/*/build/reports/jacoco/test/jacocoTestReport.xml"))
    if not informes:
        print("Sin informes. Corré primero:")
        print("  ./gradlew test webTest integrationTest && ./gradlew jacocoTestReport")
        return 0

    print(f"{'MODULO':22s} {'LINEAS':>8s} {'RAMAS':>9s}   {'dominio L':>10s} {'dominio R':>10s}   {'aplicacion L':>12s}")
    print("-" * 82)

    sin_correr = []
    bajos = []
    for informe in informes:
        modulo = informe.parts[len(informe.parts) - 6]
        raiz = ET.parse(informe).getroot()

        faltantes = [
            c
            for c in CORREDORES
            if not (informe.parents[3] / "jacoco" / f"{c}.exec").exists()
        ]
        if "integrationTest" in faltantes:
            sin_correr.append(modulo)

        linea, rama = porcentaje(raiz, "LINE"), porcentaje(raiz, "BRANCH")
        piso_alto = modulo in CRITICOS
        dl, dr = ambito(raiz, "dominio")
        al, _ = ambito(raiz, "aplicacion")

        print(
            f"{modulo:22s} {marca(linea, PISO_GLOBAL[0])} {marca(rama, PISO_GLOBAL[1])}   "
            f"{marca(dl, PISO_DOMINIO[0] if piso_alto else None)} "
            f"{marca(dr, PISO_DOMINIO[1] if piso_alto else None)}   "
            f"{marca(al, PISO_APLICACION[0] if piso_alto else None)}"
        )
        if linea is not None and linea < PISO_GLOBAL[0]:
            bajos.append(modulo)

    print()
    if sin_correr:
        print("AVISO — estos modulos se midieron SIN integrationTest, asi que el numero")
        print("        esta muy por debajo del real (los casos de uso corren ahi):")
        print("        " + ", ".join(sin_correr))
        print("        No fijes un piso con estos numeros. Levantá Docker y repetí.")
        print()

    print(f"modulos medidos: {len(informes)} · bajo el piso global de lineas: {len(bajos)}")
    print("Para fijar el piso de un modulo, en su build.gradle.kts:")
    print('    extra["pisoDeCobertura"] = 0.80   // lineas')
    print('    extra["pisoDeRamas"]     = 0.70   // ramas')
    print('    extra["pisoDelDominio"]  = 0.95   // lineas y ramas de dominio/')
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
