# Carril PR5 — CI y operación (Pablo, turno noche 2026-09-21)

> **AVANCE: 29 / 54 — 53,7 %.** (+ 1 BLOQUEADO con causa autorizada por el encargo, + 1 BLOQUEADO
> por decisión de negocio pendiente — H2.S5.M3 —, + 1 A MEDIAS)
> **Estado:** `IN_PROGRESS`. PRs abiertos: [#1](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/1)
> (estándar + spotless), [#2](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/2)
> (micro-PR troncal: catálogo CycloneDX + Redis). Ninguno mergeado todavía — el clasificador de
> permisos deniega `gh pr merge`; sigo trabajando sin esperar (regla 65), cada PR queda listo para
> un clic.

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
| H1 — Baseline global | 12 | 8 | EN CURSO (1 BLOQUEADO con causa) |
| H2 — CI verde sin trampas | 20 | 11 | EN CURSO (1 A MEDIAS, 1 BLOQUEADO, 2 TODO en S2, S6 sin empezar) |
| H3 — Borde | 7 | 7 | **HECHO** — rate limiting, CORS y bloqueo de `/actuator` con evidencia real |
| H4 — Carga medida | 3 | 3 | **HECHO** — 6 escenarios k6 reales, `transferencia.js` con baseline ×3 completo |
| H5 — Runbooks, cierre | 12 | 0 | TODO |
| **TOTAL** | **54** | **29** | |

## H3.S1 — resumen (rate limiting real con Redis)

| ID | Qué se logró | Resultado |
|---|---|---|
| H3.S1.M1 | ADR-050 + Redis en `despliegue/compose/base.yml` | PASS — contenedor real, sano |
| H3.S1.M2 | `RUTAS_SENSIBLES` en `scripts/modelo.py`: 4 rutas reales (login, registro, retiro, transferencia); refresh/reset/MFA/OTP no tienen endpoint todavía (carril de identidad, esta noche) | PASS, con el hueco declarado, no oculto |
| H3.S1.M3 | `generar_gateway.py` emite `RequestRateLimiter` con `KeyResolver` propio, antes de la ruta general | PASS |
| H3.S1.M4 | `RateLimitGatewayTest` (nombrada `ArranqueRateLimitGatewayTest` por el corredor compartido) | **PASS real, 3/3, con Redis de Testcontainers** — [evidencia/H3-S1-RateLimitGatewayTest-PASS.txt](../evidencia/H3-S1-RateLimitGatewayTest-PASS.txt) |

**Dos bugs de seguridad reales encontrados y corregidos, verificados contra el gateway
corriendo de verdad (no contra la documentación):**

1. `RedisRateLimiter` de Spring Cloud Gateway no manda `Retry-After` en el 429 — el kill-test del
   encargo lo pide. Corregido con un `GlobalFilter` propio (`LimitadorDeTasa.retryAfterEnRateLimit`).
2. **`RedisRateLimiter` falla ABIERTO por defecto** con Redis caído (verificado contra su código
   fuente: comentario literal *"we don't want a hard dependency on Redis to allow traffic"*),
   exactamente lo contrario de lo que pide el encargo. Reproducido apagando Redis de verdad: la
   petición pasaba igual. Corregido con `LimitadorDeTasa.denegarSiRedisNoResponde`, que corta la
   cadena antes de que el backend vea la petición. Evidencia:
   [evidencia/H3-S1-M4-redis-caido-fail-closed.txt](../evidencia/H3-S1-M4-redis-caido-fail-closed.txt).

**Hallazgo de infraestructura compartida corregido en el camino:** `testImplementation(testFixtures(
project(":plataforma:comun-web")))` (de `aportaya.servicio`, aplicado a TODO servicio) traía Spring
MVC y Spring Security de verdad al classpath de prueba de gateway — un módulo reactivo sin ninguno
de los dos por diseño — y el contexto de Spring no levantaba. Excluido en
`plataforma/gateway/build.gradle.kts` (dentro de mi alcance), con el motivo documentado ahí.

## H3.S2/S3 — resumen (CORS por perfil, `/actuator` solo interno)

| ID | Qué se logró | Resultado |
|---|---|---|
| H3.S2.M1 | CORS por perfil (`aportaya.cors.origenes`), `ConfiguracionCors.java` con `CorsWebFilter` real + guarda fail-closed en producción | **PASS con evidencia real**: preflight permitido (200), preflight de otro origen (403), arranque cortado sin `APORTAYA_CORS_ORIGENES` en `production`, arranque normal con un origen real — [evidencia/H3-S2-cors.txt](../evidencia/H3-S2-cors.txt) |
| H3.S2.M2 | NGINX: `Cache-Control: no-store`; HSTS **no** agregado (TLS termina en el Traefik de Coolify, no en este NGINX — verificado, no adivinado) | PASS — [evidencia/H3-S2-nginx-headers.txt](../evidencia/H3-S2-nginx-headers.txt) |
| H3.S3.M1 | `/actuator` bloqueado (404) en NGINX; `prometheus.io/scrape` en `generar_k8s.py` (gateway + 14 servicios) | **PASS con hallazgo real**: antes del fix, `/actuator/prometheus` del gateway respondía 200 por la entrada pública a cualquiera |

**Hallazgo F-05 para Leo (plataforma-infra):** 0 de los 14 servicios exponen `prometheus` en
`management.endpoints.web.exposure.include` (verificado, `grep -rl exposure servicios/*/.../application.yml`
→ vacío). Es la plantilla compartida de cada servicio, fuera de mi alcance (`servicios/**` está OUT
en mi encargo) — las anotaciones `prometheus.io/scrape` que agregué en `generar_k8s.py` no sirven de
nada hasta que esto se corrija.

## H2 — resumen (detalle de evidencia en los commits de la rama)

| ID | Qué se logró | Resultado |
|---|---|---|
| H2.S1.M1 | `spotlessApply`, diff revisado a mano (7 archivos, solo formato) | PASS |
| H2.S1.M2 | Commit, PR #1 abierto | **A MEDIAS** — falta el merge (humano, bloqueado por el clasificador) |
| H2.S2.M1 | Dependency locking + 15 `gradle.lockfile` commiteados | PASS local |
| H2.S2.M2 | Job real `dependencias` (OSV-Scanner v2.6.0, reusable workflow) reemplaza el paso falso 19c | **Corrida real en CI**: escaneó, encontró vulnerabilidades reales, falló correctamente (`fail-on-vuln`) |
| H2.S2.M3 | Prueba negativa en rama temporal | **TODO** — no se hizo |
| H2.S2.M4 | `.github/dependabot.yml` | **TODO** — no se hizo |
| H2.S3.M1/M2 | SBOM CycloneDX por módulo (plugin con versión literal, ver commit `8c6bc4d`), job + `upload-artifact` | PASS local (200 componentes, CycloneDX 1.6 válido); pendiente de una corrida de CI completa desde el fix |
| H2.S4.M1 | Trivy real sobre `identidad:ci` (`aquasecurity/trivy-action@v0.36.0`) | Wireado; **encontró CRITICAL reales sin parche** (hallazgo F-04, bloqueante) |
| H2.S4.M2 | Dockerfile por digest (2/2 etapas) | PASS (`grep -c "@sha256"` → 2) |
| H2.S4.M3 | `read_only: true` + `tmpfs: [/tmp]` en los 15 servicios + gateway | **PASS con evidencia real**: gateway arrancó `Healthy` con filesystem de solo lectura |
| H2.S4.M4 | Decisión `wget` vs Java puro, en ADR-025 | PASS |
| H2.S5.M1 | `.github/CODEOWNERS` (provisional: solo 2 de 5 handles reales verificados) | PASS — `codeowners/errors` → `{"errors":[]}` real |
| H2.S5.M2 | `docs/operacion/branch-protection.md` + 3 rulesets JSON, checks con nombre exacto (verificado contra una corrida real) | PASS |
| H2.S5.M3 | Aplicar el ruleset mínimo | **BLOQUEADO — decisión/acción de Pablo**, un comando, ya documentado |
| H2.S6 | E2E financiero, `verificarProduccion`, cablear boveda, grep de trampas, corrida completa verde | **TODO** — no se empezó |

**Bug que introduje y corregí en el camino:** el primer intento de H2.S3 (commit `c679c8e`)
rompía la compilación de **todo** el monorepo (referenciaba una entrada del catálogo que solo
existe en el PR #2, sin mergear). Lo encontré porque corrí la corrida REAL de CI de esta rama
(no me quedé con la verificación local), lo diagnostiqué, y lo corregí con una versión literal
en `buildSrc` que no depende de ningún merge (commit `8c6bc4d`).

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
| F-05 | Ninguno de los 14 servicios expone `prometheus` en `management.endpoints.web.exposure.include` (verificado: 0/14). Las anotaciones `prometheus.io/scrape` que agregué en `generar_k8s.py` (H3.S3) no sirven hasta que esto se corrija en la plantilla | `plataforma/infra` (Leo — es la plantilla compartida, `servicios/**` está OUT en mi encargo) | ABIERTO |

## No cubierto

- Los baselines por módulo de los otros cuatro carriles (no existen todavía).

## Ambigüedades que arrastro

Ver §8 en el daily del equipo — mismas seis, todas `DECIDIDA` al repartir.
