# Baseline — PasanakuBackend (AportaYa), turno noche 2026-09-21

> Estado vivo del turno: [carriles/PR5-ci-operacion.md](carriles/PR5-ci-operacion.md).
> Este documento es la foto de arranque; no se reescribe salvo para corregir un dato.

## §Estado

- **Rama:** `dev` · **SHA revalidado en este turno:** `5d7948eeddba3fa68ee7389984772689bfb977f1`
  (2026-09-21 18:30:42 -0400, "docs(auditoria): plan dev → production ready, contratos entre
  carriles y decisiones"). Distinto del SHA con el que se repartió el encargo
  (`19a621e666afdea5bdc40aced326d3f212a116f4`): entre el reparto y el arranque de este turno se
  sumó un commit de documentación (el plan madre de producción). Sin cambios de código.
- **Java:** OpenJDK Temurin 21.0.12.1 (instalado en este turno con `winget install
  EclipseAdoptium.Temurin.21.JDK`; la máquina no tenía JDK en el PATH).
- **Gradle:** 9.7.1 (wrapper), Launcher JVM 21.0.12.1, Daemon JVM compatible con Java 21
  (`gradle/gradle-daemon-jvm.properties`).
- **Docker:** 29.6.2, build dfc4efb.
- **PostgreSQL (contenedor `aportaya-postgres`):** 16.15 (Debian 16.15-1.pgdg13+2), imagen
  `postgres:16`.
- **Catálogo de versiones (`gradle/libs.versions.toml`):** `spring-boot = 3.5.6` ·
  `jooq = 3.20.5` · `shedlock = 6.9.0` · `resilience4j = 2.3.0`.

Salida literal de las cinco corridas de comando: [evidencia/H1-S1-M2-estado.txt](evidencia/H1-S1-M2-estado.txt).

## Nota de entorno — puerto de Postgres

Esta máquina de desarrollo ya tiene otro Postgres (`legion-core-postgres`, de un proyecto ajeno)
escuchando en `127.0.0.1:5433`, el puerto que `despliegue/compose/base.yml` reserva a propósito
para `aportaya-postgres`. Por la regla 70.1.6 ("un servidor de desarrollo por puerto"; nunca se
toca un contenedor de otro proyecto), el perfil `base` se levantó en este turno con un override
**local, no versionado y no commiteado**:
[`despliegue/compose/local-override.postgres.yml`](../../despliegue/compose/local-override.postgres.yml)
(remapea el puerto publicado del host a `5435`; el `base.yml` del repo no se tocó). Cualquier
comando de este turno que necesite hablarle a Postgres desde el host (`generateJooq`, `psql`) usa
`5435` en esta máquina — la variable de conexión sigue siendo la del compose/perfil, no un
hardcode nuevo.

## Nota de entorno — índice de skills

Al instalar el estándar de la casa (`.claude/hooks`, `.claude/rules`, `.claude/skills` del repo
`PasanakuPromptManager`) sobre el `.claude/skills` propio de AportaYa (66 skills), el gate
`scripts/verificar_boveda.py` § "índice de skills" pasó a `FALLA`: el índice
`.claude/skills/README.md` no listaba las 194 skills nuevas, y el chequeo exige que el índice
liste exactamente las carpetas que existen. Se corrigió agregando una sección nueva al README
("Estándar de la casa (heredadas)") con las 194 filas generadas desde el frontmatter
`description` de cada `SKILL.md`, sin tocar las 66 filas originales de AportaYa. Evidencia:
[evidencia/H1-verificar-boveda.txt](evidencia/H1-verificar-boveda.txt) (exit 0, `TODO OK`).

## Gates globales — H1.S2, en serie

| # | Comando | Veredicto | Evidencia |
|---|---|---|---|
| 1 | `spotlessCheck` | **FAIL** (rojo real, ver clasificación abajo) | [evidencia/H1-S2-M1-spotless.txt](evidencia/H1-S2-M1-spotless.txt) |
| 2 | `check -x test -x jacocoTestCoverageVerification -x jacocoTestReport` | PASS (tras H1.S3.M... regenerar jOOQ, ver clasificación) | [evidencia/H1-S2-M2-check.txt](evidencia/H1-S2-M2-check.txt) |
| 3 | `testBarrido` | PASS | [evidencia/H1-S2-M3-testBarrido.txt](evidencia/H1-S2-M3-testBarrido.txt) |
| 4 | `test` + `webTest` | PASS + PASS | [evidencia/H1-S2-M4-test-webTest.txt](evidencia/H1-S2-M4-test-webTest.txt) |
| 5 | `contractTest` + `sagaTest` | PASS + PASS | [evidencia/H1-S2-M5-contract-saga.txt](evidencia/H1-S2-M5-contract-saga.txt) |
| 6 | `e2eTest` sobre `compose --profile todo` | **BLOCKED** — solo existe la imagen `aportaya/gateway:local`; los otros 14 servicios no tienen imagen construida en esta máquina. El encargo autoriza este veredicto explícitamente ("o BLOCKED con causa si no hay imágenes construidas"). Camino: construir una por una siguiendo el README antes de H2.S6 | `docker images --filter "reference=aportaya/*"` → solo `gateway:local` |
| 7 | `generateJooq` + `generateOpenApiClients --no-parallel --no-build-cache` | PASS + PASS | [evidencia/H1-S2-M7-jooq-openapi.txt](evidencia/H1-S2-M7-jooq-openapi.txt) |

## Tabla de rojos clasificados (H1.S3.M1)

| Rojo | Clase (regla 80.4) | Causa demostrada | Corrige |
|---|---|---|---|
| `spotlessCheck` FAIL | `PRODUCT_BUG` (deuda de formato, no lógica) | 7 archivos con envoltorio de línea / orden de imports fuera del estilo de Spotless | H2.S1 — ya corregido en `pablo/feature/carril-PR5-ci-operacion` commit `ff85331`, PR [#1](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/1) esperando merge humano (el clasificador de permisos deniega `gh pr merge`, ver carril) |
| `check -x test` FAIL (primer intento) | `ENVIRONMENT` — no `PRODUCT_BUG` | La base recién levantada no tenía el esquema aplicado (`sql/aplicar.sql` nunca corrido en el volumen fresco de esta máquina); `generateJooq` de dos módulos fallaba por conexión rechazada. **No es una microtarea nueva**: es la instalación local que el propio H0.S1.M3/H0.S4.M1 del plan madre ya preveían | Resuelto en esta sesión: `./gradlew bd:aplicar -x levantar` |
| `check -x test` FAIL (segundo intento, tras aplicar esquema) | `ENVIRONMENT` — clases jOOQ generadas quedaron en caché de un intento anterior fallido (`DocumentoIdentidad.java` sin el campo `LUGAR_EXPEDICION`, aunque `sql/10_tablas/.../documento_identidad.sql:12` sí lo declara) | `grep -c LUGAR_EXPEDICION build/generated/.../DocumentoIdentidad.java` → `0` antes, `mtime` anterior al `bd:aplicar` limpio | Resuelto: `./gradlew generateJooq --rerun` |
| `docker compose --profile base up -d --wait` → exit 1 pese a que **todos** los servicios quedan `Healthy` | `PRODUCT_BUG` (del compose, no de este carril: `plataforma/infra`, dueño Leo) | Reproducido dos veces, con volumen limpio y con volumen viejo: el job de un solo uso `aportaya-minio-bucket` (`restart: "no"`, sale con exit 0) hace que `--wait` de Docker Compose v5.3.1 devuelva 1 aunque todo lo demás esté sano. `bd:levantar` (tarea Gradle) hereda el mismo exit y falla la build aunque la infra quede correcta | **Hallazgo F-02** (no corregido: fuera de mi alcance declarado, `bd/build.gradle.kts` no está en `despliegue/**`). Mitigación local usada este turno: `docker compose … up -d --wait; docker ps` para confirmar salud real, y `./gradlew bd:aplicar -x levantar` |
| `aportaya-postgres` puerto `5433` choca con `legion-core-postgres` (proyecto ajeno) | `ENVIRONMENT` (esta máquina, compartida entre proyectos) | `netstat -ano` → `5433` ya escuchado por otro proceso | Corregido **en el repo** (dentro de mi alcance, `despliegue/**`): puerto configurable `${APORTAYA_PG_PORT:-5433}` en `despliegue/compose/base.yml`, default sin cambios |

## Desconocidos §2.2 del plan madre — resueltos (H1.S3.M3, 2026-09-24)

Cada uno con comando y salida, o enlazado a la bitácora del carril que lo resolvió. Salida
literal de los que corrí yo: [evidencia/H1-S3-M3-desconocidos.txt](evidencia/H1-S3-M3-desconocidos.txt).

| # | Desconocido (§2.2) | Resolución | Evidencia |
|---|---|---|---|
| 1 | Resultado real de `verificar`, `integrationTest`, `contractTest`, `sagaTest`, `e2eTest` | Gates globales corridos uno por uno en H1.S2 (todos PASS salvo Spotless, ya corregido); `e2eTest`: el único `*E2ETest` del repo es `OutboxE2ETest` (comun-mensajeria) — ver H1.S2.M6 | [H1-S2-M4](evidencia/H1-S2-M4-test-webTest.txt), [H1-S2-M5](evidencia/H1-S2-M5-contract-saga.txt), [H1-S2-M6](evidencia/H1-S2-M6-e2eTest.txt) |
| 2 | Qué falla en Spotless | 7 archivos, solo formato; corregido en PR #1 (fusionado); paso `2 · formato` verde en `dev` | [H1-S2-M1](evidencia/H1-S2-M1-spotless.txt), [H2-S1-M2](evidencia/H2-S1-M2-merge-espejo.txt) |
| 3 | `aplicar.sql` + semillas ×2 + verificaciones | PASS en CI real sobre base vacía (pasos 7–11 del job `base`, run 35923629830) | [H1-S3-M3](evidencia/H1-S3-M3-desconocidos.txt) §3 |
| 4 | `AislamientoEsquemaTest` vs RLS/`FORCE ROW LEVEL SECURITY`/rol auditor | Resuelto por Marcelo (PR4): 15/15 PASS, `ENABLE`/`FORCE ROW LEVEL SECURITY` en `sql/40_reglas/restricciones.sql` | [carriles/PR4-seguridad.md](carriles/PR4-seguridad.md) |
| 5 | Adaptador del proveedor de retiros | Resuelto por Justin (PR2, H4.S2): puerto `ProveedorDeRetiro` + `ProveedorDeRetiroLocal` en tres niveles | [carriles/PR2-nucleo-financiero.md](carriles/PR2-nucleo-financiero.md) §H4 |
| 6 | Formato de `ManejadorGlobalDeErrores` y de los logs | Error: `ErrorApi(codigo, mensaje, detalle, trazaId)`. Logs: **texto plano** por omisión de Spring Boot — 0 configuraciones de log estructurado/JSON en el repo (hallazgo para observabilidad, no de este carril) | [H1-S3-M3](evidencia/H1-S3-M3-desconocidos.txt) §6 |
| 7 | Clientes HTTP (`*PorHttp`) y resilience4j | 5 clientes; solo `CotizadorPorHttp` usa `@Retry`/`@CircuitBreaker` (Justin, PR2). Los otros 4 no tienen resiliencia | [H1-S3-M3](evidencia/H1-S3-M3-desconocidos.txt) §7, [carriles/PR2-nucleo-financiero.md](carriles/PR2-nucleo-financiero.md) |
| 8 | ¿`identidad` valida/rota refresh tokens en Java además del trigger? | **No existen refresh tokens**: 0 coincidencias en `sql/` ni en `servicios/identidad/src/main`. Richard (PR1) no publicó bitácora en `carriles/` todavía | [H1-S3-M3](evidencia/H1-S3-M3-desconocidos.txt) §8 |

## Tabla A–J (`docs/trabajo/2026-09-21-backend-production-ready/PLAN.md` §2.4) — estado en este turno

Solo se revalidaron con evidencia fresca de esta sesión las filas H, I y J (las que tocan mi
alcance: CI y protección de ramas). A–G se heredan del SHA `19a621e6` **sin revalidar en esta
sesión** — corresponden a los carriles de Richard/Justin/Leo/Marcelo (H1–H5 del plan madre), no al
mío.

| # | Hallazgo previo | Estado revalidado acá | Evidencia de esta sesión |
|---|---|---|---|
| A–G | (ver plan madre §2.4) | **heredado, no revalidado esta sesión** | — |
| H | CI rojo en Spotless | **VIGENTE** en `origin/dev` (el fix vive en mi rama, sin mergear) | [evidencia/H1-S2-M1-spotless.txt](evidencia/H1-S2-M1-spotless.txt) |
| I | SCA falsa (solo `dependencies`, sin escaneo real) | **VIGENTE** — no tocado todavía, es H2.S2 | `.github/workflows/ci.yml:396-407` (sin verificar de nuevo esta sesión, cita heredada) |
| J | `dev` y `main` sin protección | **VIGENTE** | `gh api repos/PabloArauzCaballero/PasanakuBackend/rulesets` → `[]` — [evidencia/H1-rulesets-estado.txt](evidencia/H1-rulesets-estado.txt) |

## Enlaces a los baselines por módulo (Richard, Justin, Leo, Marcelo)

Publicados al 2026-09-24: [baseline-PR3-plataforma.md](baseline-PR3-plataforma.md),
[carriles/PR2-nucleo-financiero.md](carriles/PR2-nucleo-financiero.md),
[carriles/PR3-plataforma.md](carriles/PR3-plataforma.md),
[carriles/PR4-seguridad.md](carriles/PR4-seguridad.md). Richard (PR1, identidad) no publicó
bitácora en `carriles/` todavía (F-01 sigue abierto solo para PR1).
