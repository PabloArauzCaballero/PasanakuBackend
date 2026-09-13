# syntax=docker/dockerfile:1.7
#
# Un Dockerfile y no catorce. Es la unica excepcion a "el servicio posee todos sus
# archivos", y esta justificada: no tiene contenido propio del servicio (ADR-025).
#
#   docker build -f despliegue/Dockerfile --build-arg SERVICIO=tarifas -t aportaya/tarifas .
#
# Sin root, sin `latest`, sin secretos en la imagen, y con las capas de Spring Boot
# separadas para que un cambio de codigo no empuje 200 MB de dependencias.

ARG SERVICIO
ARG MODULO=servicios

# ---------------------------------------------------------------- construccion --
FROM eclipse-temurin:21-jdk AS construccion
ARG SERVICIO
ARG MODULO
WORKDIR /construccion

# Python, y no por gusto: `comun-web:erroresCatalogo` genera el catalogo
# `constraint_name -> R-XXX-nn` con scripts/generar_errores.py, y sin el la imagen no
# compila. Vive solo en la etapa de construccion — la de runtime no lo lleva.
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 \
    && rm -rf /var/lib/apt/lists/*

# Primero lo que casi nunca cambia: asi el cache sobrevive a un cambio de codigo.
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
COPY buildSrc buildSrc
COPY bd bd

# El catalogo de errores se GENERA desde `sql/` en cada build: es lo que impide que
# el mensaje de una restriccion y su codigo AP-CU se separen. Por eso `sql/` y
# `scripts/` entran a la imagen de construccion, aunque no al runtime.
COPY scripts scripts
COPY sql/40_reglas/restricciones.sql sql/40_reglas/restricciones.sql
COPY sql/50_verificacion/prueba_humo.sql sql/50_verificacion/prueba_humo.sql

# Solo plataforma/ y EL servicio que se empaqueta: settings.gradle.kts descubre
# servicios/ por barrido, asi que la imagen de un servicio no arrastra a los otros
# trece.
COPY plataforma plataforma
COPY ${MODULO}/${SERVICIO} ${MODULO}/${SERVICIO}

# Las clases de jOOQ salen de introspeccionar la base VIVA, y `.dockerignore` deja
# `**/build` afuera: lo que se genere en la maquina no entra al contexto. Entonces se
# generan aca adentro, y para eso la construccion necesita llegar a la base — de ahi
# el `network: aportaya-interna` en el bloque `build` del compose.
#
# Sin esto, `compileJava` fallaba con «package bo.aportaya.<servicio>.generado does
# not exist» y ninguna imagen de servicio se podia construir.
ARG BD_URL_ADMIN=jdbc:postgresql://postgres:5432/pasanaku
ARG BD_USUARIO_ADMIN=pasanaku
ARG BD_CLAVE_ADMIN=pasanaku
RUN --mount=type=cache,target=/root/.gradle \
    BD_URL_ADMIN=${BD_URL_ADMIN} \
    BD_USUARIO_ADMIN=${BD_USUARIO_ADMIN} \
    BD_CLAVE_ADMIN=${BD_CLAVE_ADMIN} \
    ./gradlew --no-daemon :${MODULO}:${SERVICIO}:bootJar -x test

RUN mkdir -p /capas && cd /capas \
    && java -Djarmode=tools -jar /construccion/${MODULO}/${SERVICIO}/build/libs/*.jar extract --layers --launcher --destination /capas

# --------------------------------------------------------------------- runtime --
# glibc y no Alpine. Argon2 —lo unico admisible para una credencial— llega por
# `argon2-jvm`, que carga su biblioteca nativa con JNA, y esa biblioteca esta
# compilada contra glibc. Sobre musl fallaba con «Error loading shared library
# ld-linux-aarch64.so.1» en la primera peticion de ingreso: el servicio levantaba
# sano y despues devolvia 500 al intentar verificar la contrasena.
FROM eclipse-temurin:21-jre-noble AS runtime
ARG SERVICIO
ENV SERVICIO=${SERVICIO}

# Un proceso que corre como root dentro del contenedor convierte cualquier escape en
# un compromiso de la maquina.
RUN groupadd --system app && useradd --system --gid app app \
    && apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app

# El orden importa: de lo que menos cambia a lo que mas.
COPY --from=construccion --chown=app:app /capas/dependencies/ ./
COPY --from=construccion --chown=app:app /capas/spring-boot-loader/ ./
COPY --from=construccion --chown=app:app /capas/snapshot-dependencies/ ./
COPY --from=construccion --chown=app:app /capas/application/ ./

USER app
EXPOSE 8080

# Readiness mira base y Kafka; liveness solo el proceso.
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=6 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", \
            "-Djava.security.egd=file:/dev/urandom", \
            "org.springframework.boot.loader.launch.JarLauncher"]
