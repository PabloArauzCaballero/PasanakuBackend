# FINAL_REPORT — PasanakuBackend, `dev` production-ready

> **VEREDICTO: `NOT READY`.**

> **Nota de fuente (regla 00 §1.3):** el encargo pide "las 16 secciones del metaprompt §86", pero
> ese documento ("metaprompt") no es un archivo accesible desde esta sesión — ni en
> `PasanakuBackend` ni en `PasanakuPromptManager` hay un archivo con esa sección. No se inventan
> 16 nombres de sección para simular cumplimiento de un espec que no se pudo leer. Esta plantilla
> usa en su lugar la estructura que **sí** se puede justificar con fuente real: el formato de
> `REPORTE.md` de la regla 40 (`.claude/rules/40-reporte-obligatorio.md`) más el contenido que el
> propio encargo pide explícitamente en su §2 (estado, SHA inicial/final, cambios manuales
> pendientes, recomendación de promoción). **Ambigüedad registrada, no resuelta por conveniencia**:
> a confirmar con quien escribió el encargo cuál es "el metaprompt" y dónde vive.

- Fecha: 2026-09-22
- SHA inicial de `dev`: `19a621e666afdea5bdc40aced326d3f212a116f4` (2026-09-21, ver
  `docs/trabajo/2026-09-21-backend-production-ready/PLAN.md` en `PasanakuPromptManager`)
- SHA final de este carril al momento de escribir esto: `1639734` (rama
  `pablo/feature/carril-PR5-ci-operacion`, sin mergear — ver §6)
- Consolida: [carriles/PR5-ci-operacion.md](carriles/PR5-ci-operacion.md) (este carril, completo) ·
  `carriles/PR1-identidad.md`, `PR2-nucleo-financiero.md`, `PR3-plataforma-infra.md`,
  `PR4-seguridad.md` — **ninguno existe todavía** (hallazgo F-01)

## 1. Por qué `NOT READY`

`READY` exige, por la regla 30.6 y por la propia CA del encargo, que **todos** los carriles estén
cerrados y que los P0 tengan evidencia. Ninguna de las dos condiciones se cumple hoy:

1. **F-01 — los otros cuatro carriles no publicaron su bitácora.** No hay `carriles/PR1…PR4.md`
   en la rama `dev` ni en esta rama. Sin ellos no hay manera honesta de afirmar que el sistema
   completo esté listo — solo se puede hablar del carril de CI y operación.
2. **F-04 — Trivy y OSV-Scanner encuentran CRITICAL reales sin parche** en la imagen del gateway
   (y por extensión, en cualquier servicio con el mismo BOM): `netty-handler`, `bcprov-jdk18on`,
   `spring-security-web`. Corregirlas exige subir Spring Boot/Spring Cloud/Netty — un upgrade no
   solicitado, fuera del alcance de este turno (regla 00 §3).
3. **F-06/F-07 — `plataforma/comun-web` y `servicios/identidad` no pasan su propio gate de
   cobertura** (`jacocoTestCoverageVerification`), recién detectado porque `verificarProduccion`
   (H2.S6.M2, escrito esta noche) es la primera vez que alguien corre esa verificación de punta a
   punta sin excluirla. Ambos módulos están fuera de mi alcance (`servicios/**` y
   `plataforma/comun-*`, encargo §3).
4. **El PR de este carril no está mergeado.** El clasificador de permisos de esta sesión deniega
   `gh pr merge` (política "Merge Without Review"); el PR #1 y el PR #2 siguen abiertos, con su
   gate local en verde, esperando un clic humano.

## 2. Lo que SÍ está demostrado (carril PR5 — CI y operación)

**Avance: 41 / 54 — 75,9 %.** Detalle completo, con cada comando y su salida, en
[carriles/PR5-ci-operacion.md](carriles/PR5-ci-operacion.md). Resumen por hito:

| Hito | Estado | Evidencia principal |
|---|---|---|
| H1 — Baseline global | 8/12, 1 bloqueado con causa autorizada | `baseline.md`, `evidencia/H1-*` |
| H2 — CI verde sin trampas | 16/20 | `evidencia/H2-*`, jobs reales de `ci.yml`/`boveda-y-esquema.yml` |
| H3 — Borde (rate limiting, CORS, `/actuator`) | **7/7 HECHO** | `evidencia/H3-*`, ADR-050 |
| H4 — Carga medida (k6) | **3/3 HECHO** | `evidencia/H4-*` |
| H5 — Runbooks y cierre | 7/12 | `docs/operacion/*`, `evidencia/H5-*` |

Rate limiting con Redis (fail-closed real, verificado apagando Redis), CORS por perfil (fail-closed
en producción, regresión encontrada y corregida en H5.S3.M1), `/actuator` bloqueado en el borde
(gap de barra final encontrado y corregido en H5.S3.M1), 6 escenarios de carga k6 con baseline ×3,
4 runbooks + backup/restore ejecutado de verdad (401/401 tablas), SBOM CycloneDX, Trivy y OSV-Scanner
cableados y corriendo de verdad en CI (encuentran vulnerabilidades reales, no simuladas), job
`e2e-financiero` verificado con una corrida real de CI (Postgres + Kafka KRaft arrancaron,
`BUILD SUCCESSFUL`), pasos de Marcelo cableados con guard de existencia.

## 3. Cambios manuales externos pendientes

| Qué | Quién | Cómo |
|---|---|---|
| Mergear PR #1 (`ci(security): instalar estandar y aplicar spotless`) | Pablo | 1 clic — [PR #1](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/1) |
| Mergear PR #2 (micro-PR troncal: CycloneDX + Redis) | Pablo | 1 clic — [PR #2](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/2) |
| Aplicar el ruleset mínimo `proteccion-minima` | Pablo | `gh api --method POST repos/PabloArauzCaballero/PasanakuBackend/rulesets --input entregables/ruleset-minimo.json` |
| Redis en Coolify (producción) | Quien administra Coolify | Agregar el servicio Redis al stack desplegado, según ADR-050 |
| `APORTAYA_CORS_ORIGENES` en cada entorno no-local/test | Quien despliega | Variable de entorno con el dominio real — sin ella el gateway no arranca (fail-closed, a propósito) |
| Decidir F-04 (CRITICAL sin parche) | Quien apruebe el upgrade de Spring Boot/Cloud/Netty | Upgrade coordinado, fuera de este turno |
| Decidir F-06/F-07 (cobertura de `comun-web` e `identidad`) | Dueño de cada módulo | Subir tests hasta el umbral, o excepción fechada y justificada |

## 4. Recomendación `dev → main`

**No se recomienda promover todavía.** Bloqueado por F-01 (los otros cuatro carriles no
publicaron), F-04, F-06 y F-07. Cuando esos cuatro estén resueltos:

1. Mergear PR #1 y PR #2 de este carril a `dev` (orden: #1 primero, es el que traía el CI rojo).
2. Espejar `dev → test` (`git push origin origin/dev:test`).
3. Aplicar el ruleset `proteccion-minima` en `dev`/`test`/`main` antes de aceptar el primer PR
   externo.
4. Orden de despliegue en producción: Redis primero (rate limiting depende de él), luego
   `identidad` (emite los tokens que el resto valida), luego el resto de servicios, gateway al
   final (es la única puerta pública — NGINX ya bloquea todo lo demás).
5. Rollback: cada servicio es una imagen por digest (`despliegue/Dockerfile`, 2/2 etapas);
   revertir es apuntar Coolify al digest anterior. La base de datos no tiene migraciones
   irreversibles pendientes de este carril (no se tocó esquema).
6. Recién ahí, PR de promoción `dev → main` con el checklist de
   [promotion-gate.md](promotion-gate.md) — sin ejecutar el merge desde esta sesión.

## 5. No cubierto

- Todo lo que depende de que los otros cuatro carriles publiquen su bitácora (F-01): sus propios
  P0, su propia evidencia, su propio veredicto.
- `H2.S6.M5` (corrida completa de CI en verde): estructuralmente imposible mientras F-04/F-06/F-07
  sigan abiertos — no es algo que se pueda "correr de nuevo" para que pase.
- `e2eTest` real contra los 14 servicios (H1.S2.M6, H5.S3.M3): requiere construir las 14 imágenes,
  trabajo de horas no intentado, con causa explícitamente autorizada por el encargo.
- `mutation-testing.md`, `security-matrix.md`, `financial-invariants.md`, `endpoints.md`: no
  existen todavía (fuera de este carril, ver `promotion-gate.md`).

## 6. Completado (carril PR5, con DoD demostrado)

| Hito | Qué se logró | Evidencia |
|---|---|---|
| H3 completo | Rate limiting Redis fail-closed, CORS por perfil fail-closed, `/actuator` bloqueado | `evidencia/H3-*` |
| H4 completo | 6 escenarios k6, baseline ×3 en `transferencia.js` | `evidencia/H4-*` |
| H5.S1 | 4 runbooks + backup/restore real (401/401 tablas) | `evidencia/H5-restore.txt` |
| H5.S2 | `promotion-gate.md` sin `[x]` sin enlazar | `promotion-gate.md` |
| H5.S3.M1 | Revisión independiente del diff, 2 hallazgos reales corregidos y reverificados | `evidencia/H5-S3-M1-*` |
| H5.S3.M2 | `gitleaks` + `verificar_seguridad.py` en verde | `evidencia/H5-gitleaks.txt`, `H5-verificar-seguridad.txt` |
| H2.S6.M1 | `e2e-financiero`: corrida real de CI, Postgres+Kafka sanos | `evidencia/H2-S6-M1-*` |
| H2.S6.M3 | Pasos de Marcelo cableados, corrida real de CI verde | `evidencia/H2-S6-M3-*` |
| H2.S6.M4 | 0 trampas reales en workflows/buildSrc | `evidencia/H2-S6-M4-*` |

## 7. A medias

| ID | Qué anda | Qué no anda | Qué falta |
|---|---|---|---|
| H2.S1.M2 | Commit y PR #1 listos, gate local en verde | Falta el merge (bloqueado por el clasificador de permisos) | Un clic de Pablo |
| H2.S6.M2 (`verificarProduccion`) | La tarea existe, compila, su grafo resuelve, `verificarSeguridad`/`verificarBoveda` corren de punta a punta dentro de ella | No llega a exit 0: 2 fallas reales de cobertura fuera de mi alcance (F-06, F-07) | Que `comun-web` e `identidad` suban cobertura |
| H5.S3.M3 (checkout limpio) | Worktree, base desde cero, esquema, `docker build`, Trivy local — todo PASS | `verificarProduccion` reproduce F-06 igual en limpio; `e2eTest` no corrió | Mismo que H2.S6.M2, más construir 14 imágenes |

## 8. Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| H2.S2.M3 (real, no sintético) — ver §Desvíos | `HECHO` con desvío declarado | — |
| H2.S5.M3 | `BLOQUEADO — DECISION_REQUIRED` | Un comando de Pablo (`entregables/ruleset-minimo.json`) |
| H2.S6.M5 (CI completo en verde) | `BLOQUEADO` estructural | F-04 (upgrade de dependencias) + F-06/F-07 (cobertura ajena) |
| H5.S3.M4 (CI verde en SHA final) | `BLOQUEADO` estructural | Mismo motivo que H2.S6.M5 |
| Carriles PR1–PR4 | `TODO` — sin publicar | Richard, Justin, Leo, Marcelo |

## 9. Desvíos del plan

1. **H2.S2.M3**: en vez de crear una dependencia vulnerable sintética en una rama temporal, se usó
   la evidencia real ya existente — el lockfile real de este repo tiene 1122 vulnerabilidades
   conocidas que bloquean el job por diseño. Es una prueba más fuerte que la sintética pedida
   (CVE real, no fabricado para la ocasión). Razón completa en
   `evidencia/H2-S2-M3-osv-negativo.txt`.
2. **Kafka en `e2e-financiero`**: el encargo sugiere `apache/kafka`; se usó
   `confluentinc/cp-kafka:7.7.1`, la imagen que este repo ya tiene verificada y corriendo en
   `despliegue/compose/base.yml` (regla 00 §1.4, coherencia con el patrón real).
3. **`ArranqueCorsGatewayTest` descartado**: el test específico para cerrar la cobertura de
   `ConfiguracionCors` causaba `PortInUseException` corriendo junto a los demás tests de gateway;
   se descartó el archivo (regla 80.5.1, nunca dejar un test que rompe la suite) y se dejó la
   guarda ya corregida y reverificada por otras tres vías (ver `evidencia/H5-S3-M1-revision-diff.md`).

## 10. Riesgos residuales

- **F-04** queda abierto indefinidamente hasta que alguien apruebe el upgrade de Spring
  Boot/Cloud/Netty — mientras tanto, CRITICAL reales corren en producción si se despliega.
- **F-06/F-07**: la cobertura real de `comun-web` e `identidad` es más baja que lo que sus propios
  umbrales prometen — puede haber comportamiento no cubierto por tests en código ya en producción.
- **Sin merge**: mientras los PR #1/#2 sigan abiertos, cualquier otro carril que rebase contra
  `dev` no tiene el estándar de seguridad instalado ni el catálogo CycloneDX/Redis.
- **F-05** (ninguno de los 14 servicios expone `prometheus`): sin esto, las anotaciones de scrape
  que agregué no sirven — no hay métricas de los servicios en producción hoy.

## 11. Decisiones y ambigüedades

Las seis del daily del equipo (Q-01…Q-06), todas `DECIDIDA` al repartir — ver
`docs/auditoria-produccion/carriles/PR5-ci-operacion.md` §"Ambigüedades que arrastro" para el
detalle y el enlace al daily. La única `DECISION_REQUIRED` que sigue abierta es H2.S5.M3 (aplicar
el ruleset mínimo), que es un comando de Pablo, no una decisión de diseño.

## 12. Ver también

[[baseline]] · [[promotion-gate]] · [carriles/PR5-ci-operacion.md](carriles/PR5-ci-operacion.md) ·
[[ADR-050 Rate limiting distribuido en el borde]]
