# Inventario y línea base

Fecha de captura: 2026-10-07. Se actualizaron las referencias locales con `git fetch`; Backend avanzó por fast-forward porque estaba limpio y dos commits detrás de `origin/main`.

| Repo | Rama | SHA auditado | Archivos versionados | Tamaño de archivos versionados | Workflows | Proyectos Gradle | Servicios | `package.json` | Dockerfiles |
|---|---|---|---:|---:|---:|---:|---:|---:|---:|
| Backend | `main` | `4e7bb51f47a948f38adc2598452f24492f4c1b07` | 4.705 | 37.432.057 bytes (35,70 MiB) | 3 | 26 | 14 | 9 | 5 |
| Frontend | `dev` | `1f4c01d9fc19e781bd17d97eb9912f0598c2d8ff` | 4.561 | 36.076.601 bytes (34,41 MiB) | 3 | 26 | 14 | 9 | 5 |

El tamaño suma el contenido de archivos versionados en el checkout; no incluye `.git`, cachés ni archivos ignorados. Los repositorios locales estaban limpios antes de generar esta auditoría.

## Componentes detectados

- Backend: 14 directorios de servicio con `build.gradle.kts`, 7 proyectos de plataforma y `bd`, 26 proyectos Gradle en total; SQL, descriptores, generación de despliegues, Docker Compose, OpenAPI y generación de clientes.
- Frontend: apps `web`, `backoffice` y `movil`; 6 paquetes Yarn en `packages/`, Yarn 4.18.0 y Turborepo 2.11.4; también contiene los 14 servicios, plataforma, SQL, `buildSrc`, wrapper y configuración Gradle del backend.
- Backend también contiene `apps/`, `packages/`, `package.json`, `yarn.lock` y `turbo.json`.
- Ambos tienen los mismos tres workflows por nombre: `boveda-y-esquema.yml`, `ci.yml` y `pages-landing.yml`. El workflow `ci.yml` difiere entre ramas.
- Ambos tienen cinco Dockerfiles rastreados y `docker-compose.coolify.yml` con el mismo SHA-256.

## Línea base de construcción y runtime

- `gradle.properties` en ambos repositorios declara `org.gradle.parallel=true`, `org.gradle.caching=true` y `-Xmx3g -XX:MaxMetaspaceSize=768m`. No fija `org.gradle.workers.max`.
- `docker-compose.coolify.yml` no contiene `deploy.resources`, `mem_limit` ni `cpus`; sí usa imágenes locales ya construidas con `pull_policy: never`. El comentario de generación documenta un incidente de 15 builds de servicios en Coolify, unas 16 JVM, load average cercano a 49 y riesgo de OOM. No hay métrica nueva del VPS disponible en este checkout.
- El stack Kubernetes generado ya aplica `requests` y `limits` a servicios desde `descriptor.yml`. Esto no demuestra límites efectivos en el modo Docker Compose/Coolify.
- Los 14 `application.yml` configuran Hikari: tres servicios fijan `maximum-pool-size: 20` y once fijan 5; suma configurada por proceso: 115 conexiones, antes de multiplicar por réplicas. Los descriptores fijan el pool de cálculo por réplica por separado y el generador valida su suma contra PgBouncer.
- `despliegue/infra.yml` configura factores de réplicas por entorno; el generador Kubernetes mantiene un mínimo de dos réplicas. La instancia TEST de Coolify no declara `deploy.replicas` en este compose.
- La JVM de la app no declara `JAVA_TOOL_OPTIONS` común con porcentajes de RAM en Compose.
- Los compose de Coolify omiten Postgres, PgBouncer, MinIO y Kafka porque el comentario indica que viven fuera del stack; no se inspeccionó el VPS ni sus métricas.

## Alcance y límites de esta captura

La medición cubre los árboles Git locales en los SHAs indicados. No mide CPU/RAM pico, tamaño de imágenes locales, latencias, pool activo, Kafka lag, top queries de PostgreSQL ni consumo del host: se necesitan Docker/Compose, servicios de infraestructura y métricas del VPS. Las fases de runtime deben completar esa evidencia en el entorno de despliegue antes de fijar presupuestos definitivos.
