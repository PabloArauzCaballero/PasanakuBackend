# Carril PR3 — Plataforma/Infra (Leo, turno noche 2026-09-21)

> **AVANCE: 1 / 49 — 2,0 %.**
> **Estado:** `IN_PROGRESS`. PR [#4](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/4)
> (plantilla de perfiles, H3.S3.M1) **mergeado** — sin bloqueo del clasificador de permisos esta
> vez. H1.S1.M2–M5 (helper de idempotencia) tienen implementacion escrita y pendiente de correr:
> el baseline (H1.S1.M1) todavia esta corriendo en background y ocupa el lock de Gradle del
> worktree, así que ningún `./gradlew` propio corrió todavía — no se reporta ningún PASS/FAIL de
> eso hasta tener la salida real.

Encargo: [repartos/2026-09-21/PromptNoche/Backend/Leo/PR3-Plataforma.Infra/OutboxQuePublicaYGuardasComunes.md](../../../../../PasanakuPromptManager/repartos/2026-09-21/PromptNoche/Backend/Leo/PR3-Plataforma.Infra/OutboxQuePublicaYGuardasComunes.md)
(repo `PasanakuPromptManager`, no este). Daily en el repo del estándar:
`repartos/2026-09-21/PromptNoche/Backend/Leo/Leo-Daily-Noche-2026-09-21.md`.

## Instalación del estándar

Ya hecha en el commit `2d2da96` (previo a esta sesión): `.claude/hooks`, `.claude/rules`,
`.claude/settings.json` copiados; `.claude/skills` fusionado (66 propias + 194 del estándar = 261).
`python .claude/hooks/plan_gate.py --self-test` → 11/11 PASS.

## Avance por hito

| Hito | Microtareas | HECHO | Estado |
|---|---:|---:|---|
| H1 — Idempotencia | 12 | 0 | EN CURSO — código de H1.S1.M2–M5 escrito, sin correr todavía |
| H2 — Outbox/Relevo | 20 | 0 | TODO |
| H3 — Guardas comunes | 9 | 1 | EN CURSO — H3.S3.M1 mergeado; resto TODO |
| H4 — Barridos/Dinero/logs/probes | 8 | 0 | TODO |
| **TOTAL** | **49** | **1** | |

## H1 — resumen

| ID | Qué se hizo | Resultado |
|---|---|---|
| H1.S1.M1 | Baseline de `plataforma/*` en corrida de background | **EN CURSO** — ver `baseline-PR3-plataforma.md`, se completa este doc en cuanto termine |
| H1.S1.M2 | `IdempotenciaRepositorioTest` con 10 casos (`plataforma/comun-web/src/test/java/.../idempotencia/IdempotenciaRepositorioTest.java`) | Escrito. Pendiente de correr (rojo esperado) — bloqueado por el lock de Gradle mientras el baseline corre |
| H1.S1.M3 | `exigirNueva` filtra por `(usuario_id, operacion, clave_idempotencia)` — las tres, no solo `(clave, operacion)` como antes | Escrito, pendiente de correr |
| H1.S1.M4 | `IdempotenciaConflicto` (subclase de `ErrorDeNegocio`, `AP-CU00-01`) → `409` cuando el hash difiere; handler dedicado en `ManejadorGlobalDeErrores`, mas especifico que el generico de `ErrorDeNegocio` (mismo patron que `OperacionRepetida` ya usaba) | Escrito, pendiente de correr `webTest` |
| H1.S1.M5 | `guardarRespuesta(dsl, ctx, operacion, clave, codigo, cuerpo)` — identidad completa, ya no solo `clave_idempotencia` | Escrito, pendiente de correr |

**Adelantado desde H1.S2** (necesario para que los 10 casos del test compilen/tengan sentido, documentado en ADR-046):
`Reloj` inyectado en `Idempotencia` (constructor `Idempotencia(esquema, reloj)`, bean actualizado en
`ConfiguracionComunWeb`), expiración de reservas, y "una respuesta `≥500` no se replaya" (para el caso
"reintento tras error transitorio"). La externalización de `aportaya.idempotencia.vigencia` a config
(sin literal, gate `SinUmbralLiteral`) queda pendiente, explícitamente, para H1.S2.M1.

`IdempotenciaEnProceso` (nueva clase, `409 AP-CU00-02`) cubre la reserva sin respuesta final y la
carrera del `INSERT ... ON CONFLICT DO NOTHING` — no existía antes.

`ADR-046 Alcance de la idempotencia.md` escrito con la decisión completa (identidad de tres
columnas, cuatro resultados de `exigirNueva`, qué CU adoptan el helper).

## H3 — resumen

| ID | Qué se hizo | Resultado |
|---|---|---|
| H3.S3.M1 | `scripts/nuevo_servicio.py`: `application.yml` (común) + `application-{local,test,staging,production}.yml` (lo que varía: `aportaya.entorno.productivo`, `aportaya.cors.origenes`) | **PASS — mergeado en PR #4**, primera hora del turno. Verificado corriendo `crear()` contra un `DEST` temporal (no se tocó `servicios/**`) |
| — (addendum) | `management.endpoints.web.exposure.include: health,prometheus,info` en la misma plantilla — hallazgo F-05 de Pablo (carril PR5): ningún servicio exponía `/actuator/prometheus` | Commit `b229c42`, listo para PR/merge |

## Hallazgos registrados (§6, no bloquean)

- **Windows: `python3` no resuelve** (alias de ejecución de apps de la Microsoft Store, exit 9009)
  aunque `python` sí (real, `C:\Python314\python.exe`). Encontrado porque bloqueaba **todo**
  `comun-web` en el baseline: la tarea `erroresCatalogo` (dependencia de `processResources`) usa
  `executable = "python3"`. Corregido en `plataforma/comun-web/build.gradle.kts` (mi archivo
  reservado) con `OperatingSystem.current().isWindows` para elegir `python` en Windows. **El mismo
  patrón existe en el `build.gradle.kts` raíz — fuera de mi alcance, hallazgo para quien lo tenga
  (Pablo/CI).**
- `comun-datos:integrationTest` tuvo un `TimeoutException: calentarElContenedor() timed out after
  600 seconds` en `AppendOnlyRepositorioTest` durante el baseline — la máquina tenía ~7 procesos
  Java y 5+ contenedores Postgres de Testcontainers corriendo en simultáneo (otras sesiones
  trabajando sus carriles a la vez). No es un defecto de código: es contención de recursos del
  entorno compartido. Se registra para que no se lea como una regresión real de `comun-datos`
  (fuera de mi alcance de todos modos).

## Ambigüedades (arrastradas del encargo, §5 — no se resuelven por conveniencia)

Q-01 a Q-05 ya vienen **DECIDIDAS** en el encargo; se usan tal cual (ver el archivo del encargo).
Ninguna ambigüedad nueva registrada todavía.

## Cómo retomar si esta sesión se corta acá

1. `git log --oneline -5` en este worktree debe mostrar `b229c42` como HEAD.
2. Confirmar que el baseline terminó: `grep -c '^DONE$' <ruta del log>`. Si sí, transcribir la
   clasificación real a `docs/auditoria-produccion/baseline-PR3-plataforma.md` (H1.S1.M1) — todavía
   no escrito a la espera de esa salida.
3. Correr, en este orden (el lock de Gradle está libre recién cuando el baseline dice `DONE`):
   `./gradlew :plataforma:comun-web:test :plataforma:comun-web:integrationTest --tests '*IdempotenciaRepositorioTest*'`
   y pegar la salida real (roja o verde) antes de afirmar nada de H1.S1.M2–M5.
4. Si sale roja por algo que no anticipé (compilación, jOOQ, lo que sea), arreglar y volver a
   correr — nunca marcar `HECHO` sin la salida.
5. Seguir el ritual: `spotlessApply`, `webTest`/`integrationTest` de `comun-web`, `ArranqueTest` x14,
   commit, PR, intentar merge, espejo a `test`.
