# Carril PR5 — CI y operación (Pablo, turno noche 2026-09-21)

> **AVANCE: 8 / 54 — 14,8 %.** (+ 1 BLOQUEADO con causa explícitamente autorizada por el encargo)
> **Estado:** `IN_PROGRESS`.

Encargo: [repartos/2026-09-21/PromptNoche/Backend/Pablo/PR5-Ci.Operacion/CiRealSupplyChainBordeYCierre.md](../../../../../PasanakuPromptManager/repartos/2026-09-21/PromptNoche/Backend/Pablo/PR5-Ci.Operacion/CiRealSupplyChainBordeYCierre.md)
(en el repo `PasanakuPromptManager`, no en este). Daily en el repo del estándar:
`repartos/2026-09-21/PromptNoche/Backend/Pablo/Pablo-Daily-Noche-2026-09-21.md`.

## Instalación del estándar

- `.claude/hooks`, `.claude/rules`, `.claude/settings.json` copiados desde `PasanakuPromptManager`
  el 2026-09-21. `.claude/skills` fusionado (66 propias de AportaYa + 194 del estándar = 260),
  ver nota en [baseline.md](../baseline.md).
- JDK 21 (Temurin) instalado en la máquina (`winget install EclipseAdoptium.Temurin.21.JDK`);
  no estaba en el PATH.
- `python .claude/hooks/plan_gate.py --self-test` → 11 PASS, 0 FAIL.

## Avance por hito

| Hito | Microtareas | HECHO | Estado |
|---|---:|---:|---|
| H1 — Baseline global | 12 | 8 | EN CURSO |
| H2 — CI verde sin trampas | 20 | 0 | TODO |
| H3 — Borde | 7 | 0 | TODO |
| H4 — Carga medida | 3 | 0 | TODO |
| H5 — Runbooks, cierre | 12 | 0 | TODO |
| **TOTAL** | **54** | **4** | |

## Microtareas HECHO (con evidencia)

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `docs/auditoria-produccion/{PLAN.md,carriles,contratos,evidencia}` ya existían (copiados en el reparto) | `test -f docs/auditoria-produccion/PLAN.md && ls docs/auditoria-produccion` | PASS |
| H1.S1.M2 | `baseline.md` §Estado con las 8 líneas; `auditar_backend.py --json` guardado | ver [baseline.md](../baseline.md), [evidencia/H1-S1-M2-estado.txt](../evidencia/H1-S1-M2-estado.txt), [evidencia/H1-auditar-backend-inicial.json](../evidencia/H1-auditar-backend-inicial.json) | PASS, exit=0 |
| — | Base compose (`postgres`, `pgbouncer`, `minio`, `kafka`, `nginx`, `gateway`) arriba con override local de puerto | `docker compose -f despliegue/compose/base.yml -f despliegue/compose/local-override.postgres.yml --profile base up -d --wait` | Todos `Healthy` salvo `minio-bucket` (job, exit 0) |
| — | `verificar_boveda.py` corregido tras instalar el estándar (índice de skills) | `python3 scripts/verificar_boveda.py` | exit=0, `TODO OK` — [evidencia/H1-verificar-boveda.txt](../evidencia/H1-verificar-boveda.txt) |
| H1.S2.M1 | `spotlessCheck` con la lista de archivos que fallan (rojo real, hoy en `dev`) | `./gradlew spotlessCheck` | FAIL documentado — [evidencia/H1-S2-M1-spotless.txt](../evidencia/H1-S2-M1-spotless.txt); corregido aparte en H2.S1 (commit `ff85331`, PR #1, sin mergear) |
| H1.S2.M2 | `check -x test` (estático, ArchUnit, `sinJpa`) | `./gradlew check -x test -x jacocoTestCoverageVerification -x jacocoTestReport` | PASS tras aplicar esquema y regenerar jOOQ — [evidencia/H1-S2-M2-check.txt](../evidencia/H1-S2-M2-check.txt) |
| H1.S2.M3 | `testBarrido` | `./gradlew testBarrido` | PASS — [evidencia/H1-S2-M3-testBarrido.txt](../evidencia/H1-S2-M3-testBarrido.txt) |
| H1.S2.M4 | `test` y `webTest` (raíz) | `./gradlew test; ./gradlew webTest` | PASS + PASS — [evidencia/H1-S2-M4-test-webTest.txt](../evidencia/H1-S2-M4-test-webTest.txt) |
| H1.S2.M5 | `contractTest` y `sagaTest` | `./gradlew contractTest; ./gradlew sagaTest` | PASS + PASS — [evidencia/H1-S2-M5-contract-saga.txt](../evidencia/H1-S2-M5-contract-saga.txt) |
| H1.S2.M7 | `generateJooq` y `generateOpenApiClients` | `./gradlew generateJooq; ./gradlew generateOpenApiClients --no-parallel --no-build-cache` | PASS + PASS — [evidencia/H1-S2-M7-jooq-openapi.txt](../evidencia/H1-S2-M7-jooq-openapi.txt) |
| H1.S3.M1 | Tabla de rojos clasificados con dueño | revisión | Escrita en [baseline.md](../baseline.md) §"Tabla de rojos clasificados" |
| H1.S3.M2 | Tabla A–J en `baseline.md` | revisión | Escrita — solo H/I/J revalidadas con evidencia fresca; A–G heredadas sin revalidar (fuera de mi alcance) |

*(La numeración de microtareas de la tabla de avance corresponde al encargo; las filas sin ID
son trabajo de entorno necesario para poder ejecutar H1, no microtareas propias del encargo.)*

## A medias

Ninguna todavía.

### H1.S2.M6 — `e2eTest` (BLOQUEADO, con causa explícitamente autorizada por el encargo)

- **Qué anda:** el CA del encargo autoriza literalmente este resultado: "o `BLOCKED` con causa si
  no hay imágenes construidas: seguí el README para construirlas de a una".
- **Qué no anda:** solo existe la imagen `aportaya/gateway:local`; los otros 14 servicios no
  tienen imagen construida en esta máquina, y construirlas una por una (14 builds de Gradle +
  Docker, en serie por la regla 70.1.4) es varias horas de trabajo que no se intentaron todavía.
- **Qué falta exactamente:** correr `docker build` por servicio siguiendo el README, luego
  `./gradlew e2eTest` contra `compose --profile todo`.
- **Dónde quedó:** nada escrito en disco para esto todavía; es la microtarea H1.S2.M6 sin tocar.

## Bloqueado

| ID | Qué bloquea | Qué intenté | Qué lo destraba | De quién depende |
|---|---|---|---|---|
| H2.S5.M3 | Crear el ruleset mínimo `proteccion-minima` (el agente no tiene permiso para modificar recursos compartidos del repo) | JSON y comando quedarán listos en `entregables/ruleset-minimo.json` | `gh api --method POST repos/PabloArauzCaballero/PasanakuBackend/rulesets --input entregables/ruleset-minimo.json` | Pablo — un comando |
| H2.S1.M2 (parcial) | `gh pr merge` lo deniega el clasificador de permisos del agente ("Merge Without Review"), a diferencia de `gh pr create` que sí funciona | `gh pr merge 1 --rebase --delete-branch=false` | Pablo mergea el PR a mano: [PR #1](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/1) (`ci(security): instalar estandar y aplicar spotless`) | Pablo — un clic por PR |

**Esto cambia el ritual del encargo para el resto del turno:** el encargo asume que el agente
mergea solo (`gh pr merge --rebase`) para no frenar a los otros cuatro carriles. En esta máquina
el clasificador de permisos deniega el merge (no es un error transitorio: es una política
explícita, "Merge Without Review"). A partir de acá cada subtarea cerrada queda como **PR abierto,
local gate en verde, esperando un merge humano** — no se fuerza con push directo a `dev` ni con
ninguna otra vía que rodee la revisión (seria ir en contra de la intención del bloqueo).

## Hallazgos para el equipo

| ID | Qué | A quién le pega | Estado |
|---|---|---|---|
| **F-04** | **Trivy real (H2.S4.M1) encontró CRITICAL sin parche disponible** en la imagen (`aportaya/gateway:local`, mismo BOM que `identidad:ci`): `netty-handler` `CVE-2026-75595` (SNI routing bypass), `bcprov-jdk18on` `CVE-2025-14813`, `spring-security-web` `CVE-2026-22732` (bypass de política de seguridad); más HIGH en `spring-expression` (`CVE-2026-41850`) y `netty-handler` (`CVE-2026-44249`). Arreglarlas exige subir Spring Boot/Spring Cloud/Netty — fuera de alcance de este turno (regla 00 §3, ningún upgrade no solicitado). Evidencia: [evidencia/H2-S4-M1-trivy-gateway-table.txt](../evidencia/H2-S4-M1-trivy-gateway-table.txt) | **`FINAL_REPORT.md` — bloquea `READY`** hasta que alguien decida parchear o justifique una excepción fechada en `.github/.trivyignore` | **ABIERTO — bloqueante** |
| F-01 | `docs/auditoria-produccion/carriles/` y `evidencia/` estaban vacíos al arrancar: Richard, Justin, Leo y Marcelo todavía no publicaron su bitácora ni su baseline por módulo | H1.S3 (enlaces), H5.S4 (`FINAL_REPORT.md`) | ABIERTO |
| F-02 | `docker compose -f despliegue/compose/base.yml --profile base up -d --wait` devuelve exit 1 aunque **todos** los servicios queden `Healthy`, por el job de un solo uso `minio_bucket` (`restart: "no"`, exit 0); `bd:levantar` (tarea Gradle, `bd/build.gradle.kts`) hereda el fallo y rompe `bd:aplicar`/`bd:reset` en cualquier máquina con Docker Compose ≥ v5.3.1. Reproducido dos veces (volumen limpio y volumen con datos) | `plataforma/infra` (Leo, `bd/build.gradle.kts` no está en mi alcance) | ABIERTO |
| F-03 | El puerto host de `aportaya-postgres` estaba fijo en `5433` (`despliegue/compose/base.yml`); en una máquina compartida con otro Postgres ajeno en ese puerto, el perfil `base` nunca levanta. Corregido en este turno (dentro de mi alcance): puerto configurable vía `APORTAYA_PG_PORT`, default sin cambios | — (ya corregido, informativo para los otros 4 carriles si les pasa lo mismo en sus máquinas) | CORREGIDO |

## No cubierto

- Los baselines por módulo de los otros cuatro carriles (no existen todavía).

## Ambigüedades que arrastro

Ver §8 en el daily del equipo — mismas seis, todas `DECIDIDA` al repartir.
