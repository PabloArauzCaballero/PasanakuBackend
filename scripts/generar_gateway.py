#!/usr/bin/env python3
"""Genera las rutas del gateway desde los prefijos reservados de cada servicio.

`plataforma/gateway/src/main/resources/application.yml` dice, desde el primer dia,
que «las rutas salen de los prefijos reservados de cada servicio (scripts/modelo.py
-> PREFIJOS)». Nunca las escribio nadie: el gateway levantaba sin una sola ruta y
todo lo que entraba por NGINX daba 404. Esto las escribe.

Se genera, no se escribe a mano, por la misma razon que el compose (ADR-025): un
prefijo nuevo en el modelo tiene que aparecer solo en la entrada publica, y catorce
bloques copiados a mano divergen en cuanto alguien toca uno.

    python3 scripts/generar_gateway.py

Salida: plataforma/gateway/src/main/resources/rutas.yml, que application.yml importa.
"""

from pathlib import Path

from modelo import PREFIJOS

RAIZ = Path(__file__).resolve().parent.parent
SALIDA = RAIZ / "plataforma/gateway/src/main/resources/rutas.yml"

# El contrato declara `/api/v1` y el servicio sirve en la raiz: el prefijo lo pone
# la entrada publica. `StripPrefix=2` saca `api` y `v1`, y a identidad le llega
# `POST /sesiones`, que es lo que su OpenAPI declara.
PREFIJO_PUBLICO = "/api/v1"
SEGMENTOS_A_SACAR = 2

# El nombre del servicio en el modelo no siempre es el del contenedor: el DNS de la
# red de Docker usa el nombre del directorio de `servicios/`.
ANFITRION = {"nucleo_financiero": "nucleo-financiero"}

CABECERA = """# GENERADO por scripts/generar_gateway.py — no editar a mano.
#
# Una ruta por servicio, con los prefijos que reserva scripts/modelo.py -> PREFIJOS.
# Una ruta que no cae en un prefijo real no existe: el gateway devuelve 404 y nadie
# tiene que discutir de quien era.
#
# `StripPrefix=2` saca `/api/v1`: el contrato lo declara y el servicio sirve en la
# raiz, asi que el prefijo lo pone y lo saca la entrada publica.
"""


def rutas() -> str:
    lineas = [CABECERA, "spring:", "  cloud:", "    gateway:", "      server:", "        webflux:", "          routes:"]
    for servicio in sorted(PREFIJOS):
        anfitrion = ANFITRION.get(servicio, servicio)
        caminos = ",".join(f"{PREFIJO_PUBLICO}{p}/**" for p in sorted(PREFIJOS[servicio]))
        lineas += [
            f"            - id: {servicio}",
            f"              uri: http://{anfitrion}:8080",
            "              predicates:",
            f"                - Path={caminos}",
            "              filters:",
            f"                - StripPrefix={SEGMENTOS_A_SACAR}",
        ]
    return "\n".join(lineas) + "\n"


def main() -> None:
    SALIDA.parent.mkdir(parents=True, exist_ok=True)
    SALIDA.write_text(rutas(), encoding="utf-8")
    print(f"{SALIDA.relative_to(RAIZ)} — {len(PREFIJOS)} servicios")


if __name__ == "__main__":
    main()
