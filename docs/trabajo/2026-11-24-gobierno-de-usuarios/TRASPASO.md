# Traspaso — gobierno de usuarios y traspaso general de Pasanaku

> **Autorización de Pablo (2026-11-24 23:25 UTC):** mergear a `dev` lo que sea necesario y promover a `test`. `main` sigue prohibido sin autorización explícita. Orden de integración: #516 (B) → dev; PRs de D, C y F → dev con CI verde; luego promoción `dev → test` por PR.

> **Para quien lo retome (otra cuenta u otra sesión):** leé este archivo entero, después `INVENTARIO.md` (misma carpeta) y `docs/trabajo/2026-11-23-continuar-traspaso/{PLAN,REPORTE}.md`. Lo que no está acá no se hizo. Actualizá la tabla «Estado» y el «Registro» al final cada vez que cambie algo y commiteá en `claude/modest-newton-4ckcid`.

- Repos: `PabloArauzCaballero/PasanakuBackend` y `PabloArauzCaballero/PasanakuFrontend` (monorepo; el frontend solo recibe dependabot).
- Ramas: trabajo en `dev`; promoción `dev → test` por PR; **NUNCA tocar `main` ni mergear/promover a `main` sin autorización explícita de Pablo.** Los PRs hacia `main` se cierran sin mergear.
- Sin secretos ni datos personales en logs, reportes ni capturas.
- Última actualización: 2026-11-24 ~23:30 UTC. Peldaño global: `TESTED` (CI), no `VERIFIED`.

## Estado

| Pieza | Estado | Detalle |
|---|---|---|
| Traspaso general (H1–H7) | 34/40 | En `test` (#513 → `f957167b`). Falta `estado` de TEST (lo corre Pablo) y 5 bloqueados por Pablo. |
| Lote A — segregación | **HECHO** | #515 en dev `2c963e46`. |
| Lote E — revocación/inactividad/auditoría | **HECHO** | #517 en dev `5eb62e76`. Ambigüedades A1–A6 de su PLAN: confirmar con Pablo antes de promover a test; crear el tema Kafka `aportaya.identidad.revocacion` con `despliegue/host/crear-topicos.sh`. |
| Lote B — CU-08 por asignación | **#516 abierto**, fix de test en `2c29b16b`, esperando CI verde | Sesión `session_01ATKb2QKXrRcFwGMW5Biabx`. Cuando la CI esté verde: mergear a `dev`. |
| Lote G — CU-134 participantes | **HECHO el documento: #514 en dev `9cd4d2c4`** (Pablo autorizó mergear a dev lo necesario, 2026-11-24 23:25) | El CU sigue en estado BORRADOR dentro del documento: Pablo aún debe aprobar D-1…D-15 (`docs/trabajo/2026-11-24-gu-g/REPORTE.md`) ANTES de implementarlo. |
| Lote D — backoffice «Personal y accesos» | **LANZADO 2026-11-24 23:24 UTC** | Sesión `session_01ATKb2NFv1aTGuXmnRXK9eP`, rama `claude/gu-d-backoffice`, contra el contrato de B (rama `claude/gu-b-cu08`) con doble declarado; rebasea cuando #516 entre a dev. |
| Lote C — credencial/invitación/recuperación | **LANZADO 23:26 UTC** | `session_01ATKb2NNuSvsGjpcaPm4hVu`, rama `claude/gu-c-credencial`. |
| Lote F — roles propios + cuentas de servicio | **LANZADO 23:26 UTC** | `session_01ATKb2KrP6yE7LdUMcadjtq`, rama `claude/gu-f-roles-servicio`. |
| Participantes (CU-134 implementación) | Bloqueado | Después de que Pablo apruebe el CU. |

## Decisiones de Pablo vigentes (no volver a preguntar)

Ver `INVENTARIO.md` («Decisiones de Pablo»). Sobre B (2026-11-24): suspender revoca todos los roles y reactivar vuelve vacío; baja laboral con estado nuevo `BAJA_LABORAL`; el `jti` del step-up consumido va a tabla con UNIQUE (no memoria); #516 entra a dev con CI verde y **no se promueve a test hasta que D esté en dev** (con B solo, alta/suspender/reactivar/desbloquear del backoffice responden 422 `AP-CU08-09`). Códigos CU-08: A usa 08 y 12; B usa 09–11.

## Qué hacer ahora, en orden

1. **#516**: ver CI (`gh api repos/PabloArauzCaballero/PasanakuBackend/commits/<sha>/check-runs?per_page=100`). Si hay 429 de Maven Central (`org.jacoco.ant`, `Could not resolve`), es ENVIRONMENT: esperar a que la corrida termine y `gh api -X POST .../actions/runs/<run>/rerun-failed-jobs`. **No relanzar sobre una rama con corrida en curso**: `ci.yml` cancela la anterior (concurrency). Si hay rojo real, clasificar (PRODUCT_BUG/TEST_BUG), corregir sin debilitar aserciones. Verde → `gh api -X PUT repos/PabloArauzCaballero/PasanakuBackend/pulls/516/merge -f merge_method=merge -f sha=<head>`.
2. **Lanzar la ola 2** (`mcp__claude-code-remote__create_session`, `source_url` del repo, `source_revision: dev`, `outcome_branch` propia por lote: `claude/gu-c-…`, `claude/gu-d-…`, `claude/gu-f-…`). Máximo 4 sesiones simultáneas (CLAUDE.md regla 70), lotes con archivos disjuntos, cada una con PLAN.md, REPORTE.md (avance en la primera línea), PR a `dev`, capturas en 3 anchos × 2 temas cuando toque UI. D usa el contrato de B (ya en dev) y debe exigir step-up en alta/suspender/reactivar/desbloquear y usar `GET /roles?asignablesAPersonalInterno=true`.
3. Revisar cada PR hija (verificadores, CI verde, capturas mirando de verdad), mergear a `dev`. Promover `dev → test` solo con D en dev, por PR, con CI verde.
4. Si una sesión hija se corta por límite de uso (`usage_limit_reached` en `list_events kinds=["result"]`), reanudarla con `send_message` diciéndole que retome desde su PLAN/REPORTE.
5. Mantener este archivo, `INVENTARIO.md` y los PLAN/REPORTE; mandar capturas y el HTML de avance (artefacto «Avance Pasanaku», https://claude.ai/artifact/SKh1SnUvcNRzCcacHcmVFE; la fuente es la tabla de arriba: porcentaje = microtareas HECHO / total de cada PLAN, con `python .claude/hooks/plan_status.py --path <PLAN.md>`).

## Aprobaciones de Pablo (2026-11-24 ~23:30 UTC, «aprobado todo»)

Aprobado todo lo que estaba bloqueado por él: **CU-134 con la recomendación de cada D-1…D-15 y el número CU-134** (lote G implementa, ver su sesión); las suposiciones A1–A6 del lote E tal como están escritas en su PLAN; RESPALDO-003, APROB-001 y grupos/detalle: opción recomendada en los documentos o, si no hay propuesta concreta, la conservadora, registrada en el REPORTE del lote H (`session_01ATKb2NR2voBqug9DCL1pZ2`, rama `claude/gu-h-pendientes-app`).

## Pendiente que solo Pablo puede resolver (lo demás ya está aprobado)

Correr `estado` en TEST (SSH bloqueado desde la nube); **crear la keystore de release de Android y sus secretos de Actions** (acción sobre algo compartido; el lote H deja los pasos exactos); crear el tema Kafka `aportaya.identidad.revocacion` con `despliegue/host/crear-topicos.sh` al desplegar; prueba en dispositivo real (push FCM, rostros reales, goldens macOS).

## Cómo verificar sin la máquina de la sesión anterior

- Java: Gradle local suele recibir 429 de Maven Central → se verifica en CI. Python verificadores: `python3 scripts/verificar_*.py`, `scripts/inventario_endpoints.py --check`, `generar_postman`/`generar_ddl` sin diff.
- Backoffice: `yarn` (corepack: `COREPACK_ENABLE_STRICT=0`), E2E con `PLAYWRIGHT_BROWSERS_PATH=/opt/pw-browsers`, un solo worker.
- Aplica `.claude/rules/*` (plan antes de código, escalera de evidencia, reporte, regla 65: simular el contrato en tres niveles en vez de quedar bloqueado).

## Registro

| Fecha UTC | Qué pasó |
|---|---|
| 2026-11-24 19:30 | #515 (A) y #517 (E) en dev. |
| 2026-11-24 23:22 | B corrigió el TEST_BUG de `FixturaDeIdentidad.otorgante()` (`2c29b16b`); espera CI de #516. |
| 2026-11-24 23:30 | Se creó este traspaso y una tarea programada horaria que arranca una sesión nueva leyéndolo. |
| 2026-11-24 23:24 | Lanzado el lote D (`session_01ATKb2NFv1aTGuXmnRXK9eP`). |
| 2026-11-24 23:26 | #514 mergeado a dev; lanzados C y F en paralelo con D. Autorización de Pablo para mergear y promover a test. |
| 2026-11-24 23:32 | Pablo aprobó todo lo bloqueado: G implementa CU-134; lanzado lote H (`session_01ATKb2NR2voBqug9DCL1pZ2`) para CONFIG-001/RESPALDO-003/APROB-001/grupos-detalle. |
