#!/usr/bin/env python3
"""
Genera el perfil `todo` del compose: los catorce servicios y sus dependencias.

    python3 scripts/generar_compose.py

Lee
    servicios/*/descriptor.yml               que servicios hay
    servicios/*/src/main/resources/*.yml     que variables exige cada uno
Escribe
    despliegue/compose/servicios.yml         un servicio por carpeta, perfil `todo`

La salida es DERIVADA y no se edita a mano. Catorce bloques escritos a mano
divergen, y la divergencia aparece cuando alguien levanta el stack completo y un
servicio no arranca porque le falta una variable que los otros trece si tienen.

Las variables que cada servicio exige NO se inventan: salen de leer su
`application.yml` y buscar los `${...}`. Si alguien agrega una clave nueva y no la
declara aca, este script la incluye sola en la proxima corrida.

Ningun servicio publica puerto: la unica entrada publica sigue siendo NGINX
(ADR-025). Y ninguno arranca antes de que la base este lista y migrada.
"""

import pathlib
import re
import sys

RAIZ = pathlib.Path(__file__).resolve().parent.parent
SERVICIOS = RAIZ / "servicios"
SALIDA = RAIZ / "despliegue/compose/servicios.yml"
# En la RAIZ y no junto a los otros: Coolify ejecuta el compose con
# `--project-directory` en la raiz del clon, y las rutas relativas se resuelven
# desde ahi — con el archivo en despliegue/compose/, `context: ../..` terminaba en
# `/` y la construccion moria con «lstat /despliegue: no such file or directory».
SALIDA_DESPLEGADO = RAIZ / "docker-compose.coolify.yml"

# Lo que vale igual para los catorce. Un valor por variable, y aca se ve entero.
COMUNES = {
    "BD_URL": "jdbc:postgresql://pgbouncer:6432/pasanaku",
    "BD_CLAVE": "pasanaku",
    "KAFKA_URL": "kafka:9092",
    "JWKS_URI": "http://identidad:8080/.well-known/jwks.json",
}

# Secretos y datos del entorno. En el compose local son valores de desarrollo y
# estan a la vista a proposito: lo que NO puede pasar es que en produccion salgan
# de aca. Ahi los pone el almacen de secretos, y el servicio no levanta sin ellos.
DE_DESARROLLO = {
    "SEGURIDAD_PIMIENTA": "pimienta-local-no-es-la-de-produccion",
    # El servidor de archivos (ADR-034). La cedula, la selfie y los comprobantes van
    # ahi, no al disco del contenedor: un contenedor se reemplaza y la evidencia
    # legal no. En la columna queda una clave de objeto, nunca una URL publica.
    "ARCHIVOS_URL": "http://minio:9000",
    "ARCHIVOS_USUARIO": "aportaya",
    "ARCHIVOS_CLAVE": "aportaya-local",
    "ARCHIVOS_BUCKET": "aportaya-archivos",
    "WEBHOOK_SECRETO": "secreto-local-no-es-el-de-produccion",
    "CERTIFICADOS_CLAVE_FIRMA": "clave-local-no-es-la-de-produccion",
    "CUENTA_PUENTE_CUSTODIA": "00000000-0000-0000-0000-0000000000c0",
    "BASE_URL_PUBLICA": "http://localhost",
    "SIN_NIT_EMISOR": "1234567890",
    # Vacia a proposito: el compose levanta UNA replica de identidad, y con una sola
    # la clave generada en memoria alcanza. El arranque avisa que lo hizo. En
    # cualquier entorno con dos replicas esto lo inyecta el almacen de secretos, y no
    # este archivo (ADR-037).
    "JWT_CLAVE_FIRMA": "",
}

# ── El mismo cuadro, para un entorno DESPLEGADO (Coolify) ───────────────────
#
# Misma fuente y mismo barrido: lo unico que cambia es de donde sale cada valor.
# Aca ninguno es un literal de desarrollo — los secretos llegan como variables del
# entorno, que en Coolify se editan en la interfaz y no viven en el repositorio.
# Un `${...}` que Coolify no conozca lo crea el solo al leer el compose.
COMUNES_DESPLEGADO = {
    "BD_URL": "jdbc:postgresql://pgbouncer:6432/pasanaku",
    "BD_CLAVE": "${BD_CLAVE}",
    "KAFKA_URL": "kafka:9092",
    "JWKS_URI": "http://identidad:8080/.well-known/jwks.json",
}

DE_ENTORNO = {
    # El nombre del host es el mismo que en desarrollo porque los contenedores
    # viven en la red `aportaya-interna`, igual que el compose local.
    "ARCHIVOS_URL": "http://minio:9000",
    "ARCHIVOS_BUCKET": "aportaya-archivos",
    "ARCHIVOS_USUARIO": "${ARCHIVOS_USUARIO}",
    "ARCHIVOS_CLAVE": "${ARCHIVOS_CLAVE}",
    "SEGURIDAD_PIMIENTA": "${SEGURIDAD_PIMIENTA}",
    "WEBHOOK_SECRETO": "${WEBHOOK_SECRETO}",
    "CERTIFICADOS_CLAVE_FIRMA": "${CERTIFICADOS_CLAVE_FIRMA}",
    "CUENTA_PUENTE_CUSTODIA": "${CUENTA_PUENTE_CUSTODIA}",
    "BASE_URL_PUBLICA": "${BASE_URL_PUBLICA}",
    "SIN_NIT_EMISOR": "${SIN_NIT_EMISOR}",
    # Con una sola replica la clave generada en memoria alcanza, pero aca se deja
    # inyectable: el dia que haya dos, cada una firmaria distinto y los tokens de
    # una los rechazaria la otra (ADR-037).
    "JWT_CLAVE_FIRMA": "${JWT_CLAVE_FIRMA}",
}


def url_de_servicio(variable: str, servicios: list[str]) -> str | None:
    """`URL_GRUPOS` -> `http://grupos:8080`, si `grupos` existe.

    No se escribe una tabla de catorce entradas: **se deriva del nombre del servicio**,
    que es la unica fuente. Una tabla a mano se olvida el dia que alguien agrega un
    adaptador nuevo, y el fallo aparece recien al levantar el stack entero.

    Se comprueba que el servicio exista de verdad: `URL_LOQUESEA` no se convierte en
    `http://loquesea:8080` en silencio, se declara como variable sin valor.
    """
    if not variable.startswith("URL_"):
        return None
    destino = variable.removeprefix("URL_").lower().replace("_", "-")
    return f"http://{destino}:8080" if destino in servicios else None


CABECERA = """# Perfil `todo`: los catorce servicios, GENERADO por scripts/generar_compose.py.
#
#   docker compose -f despliegue/compose/base.yml -f despliegue/compose/servicios.yml \\
#     --profile todo up -d --wait
#
# NO se edita a mano. Un bloque distinto de los otros trece es una divergencia que
# aparece recien cuando alguien levanta el stack entero (ADR-025).
#
# Ninguno publica puerto: la unica entrada publica es NGINX. Y ninguno arranca antes
# de que la base este lista — el orden del despliegue no es negociable.
name: aportaya

# La red la declara base.yml, que es el archivo que siempre se combina con este.
# Repetirla aca con `external: true` haria que un `up` de este solo no la creara.
networks:
  interna:
    name: aportaya-interna

services:
"""

BLOQUE = """  {nombre}:
    build:
      context: ../..
      dockerfile: despliegue/Dockerfile
      args:
        SERVICIO: {nombre}
    image: aportaya/{nombre}:local
    container_name: aportaya-{nombre}
    profiles: [todo]
    networks: [interna]
    environment:
{ambiente}
    healthcheck:
      test: ["CMD-SHELL", "wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1"]
      interval: 10s
      timeout: 3s
      retries: 18
      start_period: 60s
"""


CABECERA_DESPLEGADO = """# El stack desplegado — GENERADO por `python3 scripts/generar_compose.py --coolify`.
#
# Es el mismo barrido de servicios/ que el perfil `todo`, con estas diferencias, y
# cada una tiene un motivo medido en el primer despliegue de TEST:
#
#   1 · los valores no son literales de desarrollo, son variables del entorno;
#   2 · NO construye: arranca imagenes `aportaya/*:test` ya construidas en el host
#       por /opt/aportaya/bin/construir-todo.sh, UNA POR UNA. Construir desde Coolify
#       lanzo las quince a la vez —Coolify reescribe los Dockerfile para inyectar sus
#       ARG, asi que no reusa la cache—, y con `org.gradle.jvmargs=-Xmx3g` eso fueron
#       16 JVM, carga 49 y la maquina entera (Atlas incluido) camino al OOM.
#       `pull_policy: never` porque Coolify intenta bajar del registro hasta las
#       imagenes locales;
#   3 · postgres, pgbouncer, minio y kafka NO estan aca: viven fuera de Coolify, en
#       /opt/aportaya/, porque Coolify recrea la aplicacion entera en cada despliegue.
#
# La red `aportaya-interna` es externa y ya existe: ahi `postgres`, `pgbouncer`,
# `minio`, `kafka` y `gateway` resuelven igual que en la maquina de desarrollo.
name: aportaya

networks:
  interna:
    external: true
    name: aportaya-interna
  # La red por la que Traefik llega a lo que se publica: el gateway y los dos fronts.
  publica:
    external: true
    name: coolify

services:
  # El esquema viaja con el despliegue. Termina antes de que arranque un solo
  # servicio, y si falla no arranca ninguno.
  esquema:
    image: aportaya/esquema:test
    pull_policy: never
    restart: "no"
    environment:
      PGHOST: postgres
      PGPORT: "5432"
      PGDATABASE: pasanaku
      PGUSER: ${BD_USUARIO_ADMIN}
      PGPASSWORD: ${BD_CLAVE_ADMIN}
    networks: [interna]

  # El gateway: la unica entrada a la API (ADR-025).
  gateway:
    image: aportaya/gateway:test
    pull_policy: never
    restart: unless-stopped
    environment:
      SPRING_PROFILES_ACTIVE: ${PERFIL_SPRING}
    healthcheck:
      test: ["CMD-SHELL", "wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/liveness || exit 1"]
      interval: 10s
      timeout: 3s
      retries: 12
      start_period: 30s
    networks: [interna, publica]

  # El portal de operacion. Su nginx inyecta el meta del gateway y reenvia /api/ al
  # gateway por el MISMO origen: la CSP dice `connect-src 'self'`.
  backoffice:
    image: aportaya/backoffice:test
    pull_policy: never
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "wget -q -O /dev/null http://127.0.0.1:8080/ || exit 1"]
      interval: 15s
      timeout: 3s
      retries: 5
    networks: [interna, publica]

  # El sitio publico, con render en servidor.
  web:
    image: aportaya/web:test
    pull_policy: never
    restart: unless-stopped
    environment:
      APORTAYA_GATEWAY: http://gateway:8080/api/v1
    networks: [interna, publica]

"""

BLOQUE_DESPLEGADO = """  {nombre}:
    image: aportaya/{nombre}:test
    pull_policy: never
    restart: unless-stopped
    depends_on:
      esquema:
        condition: service_completed_successfully
    environment:
{ambiente}
    healthcheck:
      test: ["CMD-SHELL", "wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1"]
      interval: 10s
      timeout: 3s
      retries: 18
      start_period: 60s
    networks: [interna]
"""


def variables_de(servicio):
    """Las variables que este servicio exige, leidas de su propia configuracion."""
    config = SERVICIOS / servicio / "src/main/resources/application.yml"
    if not config.is_file():
        return []
    texto = config.read_text(encoding="utf-8")
    return sorted(set(re.findall(r"\$\{([A-Z_]+)[:}]", texto)))


def main():
    # Un solo barrido y un solo cuadro de variables para los dos destinos: si el
    # desplegado tuviera su propio generador, divergirian, y la divergencia
    # aparece recien cuando el entorno de pruebas no arranca.
    desplegado = "--coolify" in sys.argv[1:]

    servicios = sorted(
        d.name for d in SERVICIOS.iterdir() if (d / "descriptor.yml").is_file()
    )
    if not servicios:
        print("no hay servicios con descriptor: nada que generar")
        return 1

    sin_valor = []
    bloques = []
    for servicio in servicios:
        lineas = []
        for variable in variables_de(servicio):
            # Con `is not None` y no con `or`: una cadena vacia es un valor legitimo
            # —la clave de firma que se genera sola— y `or` la trataria como ausente.
            if desplegado:
                valor = COMUNES_DESPLEGADO.get(variable)
                if valor is None:
                    valor = DE_ENTORNO.get(variable)
            else:
                valor = COMUNES.get(variable)
                if valor is None:
                    valor = DE_DESARROLLO.get(variable)
            if valor is None:
                valor = url_de_servicio(variable, servicios)
            if valor is None:
                sin_valor.append(f"{servicio}: {variable}")
                continue
            lineas.append(f"      {variable}: {valor}")
        if desplegado:
            # Cual perfil corre lo decide el entorno y no este archivo: `local`
            # trae el segundo factor de desarrollo —codigo fijo— y esa es
            # exactamente la clase de decision que no se hornea en el repositorio.
            lineas.append("      SPRING_PROFILES_ACTIVE: ${PERFIL_SPRING}")
            bloques.append(BLOQUE_DESPLEGADO.format(nombre=servicio, ambiente="\n".join(lineas)))
        else:
            # El perfil `local` enciende el simulador de pagos y la mensajeria
            # simulada, que son los defaults del contrato de implementacion.
            lineas.append("      SPRING_PROFILES_ACTIVE: local")
            bloques.append(BLOQUE.format(nombre=servicio, ambiente="\n".join(lineas)))

    if sin_valor:
        print("Variables que ningun valor cubre; agregalas al script antes de generar:")
        for falta in sin_valor:
            print(f"  {falta}")
        return 1

    salida = SALIDA_DESPLEGADO if desplegado else SALIDA
    cabecera = CABECERA_DESPLEGADO if desplegado else CABECERA
    etiqueta = "desplegado (Coolify)" if desplegado else "perfil `todo`"
    salida.parent.mkdir(parents=True, exist_ok=True)
    salida.write_text(cabecera + "\n".join(bloques), encoding="utf-8")
    print(f"compose {etiqueta}: {len(servicios)} servicios -> {salida.relative_to(RAIZ)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
