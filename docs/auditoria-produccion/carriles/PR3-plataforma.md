# Carril PR3 — Plataforma/Infra (Leo, turno noche 2026-09-21)

> **AVANCE: 6 / 49 — 12,2 %.**
> **Estado:** `IN_PROGRESS`. PR [#4](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/4)
> (plantilla de perfiles, H3.S3.M1) **mergeado**. H1.S1 completo (M1–M5) con ciclo rojo→verde real
> y `ArranqueTest` × 14 en verde — ver evidencia abajo. Commit `ee60ed7` listo para PR.

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
| H1 — Idempotencia | 12 | 5 | EN CURSO — H1.S1 (M1–M5) cerrado con evidencia; H1.S2/S3 TODO |
| H2 — Outbox/Relevo | 20 | 0 | TODO |
| H3 — Guardas comunes | 9 | 1 | EN CURSO — H3.S3.M1 mergeado; resto TODO |
| H4 — Barridos/Dinero/logs/probes | 8 | 0 | TODO |
| **TOTAL** | **49** | **6** | |

## H1 — resumen

| ID | Qué se hizo | Resultado |
|---|---|---|
| H1.S1.M1 | Baseline de `plataforma/*` (background) | **PASS** — 4/6 módulos verdes, 2 rojos de entorno (no de código); ver `baseline-PR3-plataforma.md` |
| H1.S1.M2 | `IdempotenciaRepositorioTest` con 10 casos | **Ciclo rojo→verde real**, ver evidencia abajo |
| H1.S1.M3 | `exigirNueva` filtra por `(usuario_id, operacion, clave_idempotencia)` — las tres, no solo `(clave, operacion)` como antes | **PASS** |
| H1.S1.M4 | `IdempotenciaConflicto` (subclase de `ErrorDeNegocio`, `AP-CU00-01`) → `409` cuando el hash difiere; handler dedicado en `ManejadorGlobalDeErrores`, mas especifico que el generico de `ErrorDeNegocio` (mismo patron que `OperacionRepetida` ya usaba) | **PASS** — `webTest` con los dos casos nuevos (`idempotenciaConflicto`, `idempotenciaEnProceso`) |
| H1.S1.M5 | `guardarRespuesta(dsl, ctx, operacion, clave, codigo, cuerpo)` — identidad completa, ya no solo `clave_idempotencia` | **PASS** |

**Adelantado desde H1.S2** (necesario para que los 10 casos del test compilen/tengan sentido, documentado en ADR-046):
`Reloj` inyectado en `Idempotencia` (constructor `Idempotencia(esquema, reloj)`, bean actualizado en
`ConfiguracionComunWeb`), expiración de reservas, y "una respuesta `≥500` no se replaya" (para el caso
"reintento tras error transitorio"). La externalización de `aportaya.idempotencia.vigencia` a config
(sin literal, gate `SinUmbralLiteral`) queda pendiente, explícitamente, para H1.S2.M1.

`IdempotenciaEnProceso` (nueva clase, `409 AP-CU00-02`) cubre la reserva sin respuesta final y la
carrera del `INSERT ... ON CONFLICT DO NOTHING` — no existía antes.

`ADR-046 Alcance de la idempotencia.md` escrito con la decisión completa (identidad de tres
columnas, cuatro resultados de `exigirNueva`, qué CU adoptan el helper).

### Evidencia real — ciclo rojo→verde de `IdempotenciaRepositorioTest`

**Corrida 1 (rojo genuino, 10/10 FAIL)** —
`./gradlew :plataforma:comun-web:integrationTest --tests '*IdempotenciaRepositorioTest*'`:
```
org.jooq.exception.IntegrityConstraintViolationException: ... violates foreign key
constraint "fk_respuesta_idempotente_usuario_id"
  Detail: Key (usuario_id)=(daa5d9c9-...) is not present in table "usuario".
10 tests completed, 10 failed
```
Causa: el test usaba `UUID.randomUUID()` como `usuario_id` sin crear el usuario real en
`identidad.usuario` — un bug del test (fixture faltante), no del código bajo prueba.
Corregido agregando `usuarioReal()` (INSERT mínimo en `identidad.usuario`, mismo patrón que
`FixturaDeBilletera.usuario()` en `nucleo-financiero`, copiado y no importado por estar en
`servicios/`, fuera de mi módulo).

**Corrida 2 (9/10 PASS, 1 FAIL — bug real de código):**
```
ERROR: new row for relation "respuesta_idempotente" violates check constraint
"ck_respuesta_idem_expira" (expira_en > registrada_en)
```
Causa real: `exigirNueva` insertaba `registrada_en = now()` (reloj de la base) pero calculaba
`expira_en` desde el `Reloj` inyectado — al inyectar `Reloj.fijo(hace 2 días)` en el caso
"expirada", `expira_en` quedaba ANTES de `registrada_en`. Corregido: las dos columnas ahora
salen del mismo `Reloj`.

**Corrida 3 (verde real):**
```
BUILD SUCCESSFUL in 7m 42s
27 actionable tasks: 3 executed, 24 up-to-date
```
Sin líneas `FAILED`; los 10 casos (`nueva`, `replay`, `cuerpo distinto → 409`, `otro usuario`,
`otra operación`, `50 hilos`, `expirada`, `rollback`, `fallo tras reservar`, `reintento tras
error transitorio`) en verde.

### Gate local completo

```
./gradlew spotlessApply :plataforma:comun-web:test :plataforma:comun-web:webTest \
  :plataforma:comun-web:integrationTest spotlessCheck
```
`BUILD SUCCESSFUL in 8m 50s`. `spotlessApply` corrido a nivel raíz reformateó de más
(`servicios/identidad/**`, fuera de mi alcance) — revertido con `git checkout -- servicios/identidad/`
antes de commitear; solo quedó el reformateo de `plataforma/comun-archivos/**` (mío) más los cambios
de `comun-web`.

### `ArranqueTest` × 14 (mi gate, aunque los servicios sean ajenos)

El comando literal del encargo (`./gradlew integrationTest --tests '*ArranqueTest*'`) **no
funciona tal cual**: la raíz del repo declara su propio `integrationTest` como tarea agregadora
(`tasks.register`, sin tipo `Test`), y Gradle rechaza `--tests` contra ella
(`Unknown command-line option '--tests'`). Se invocó explícitamente contra los 14
`:servicios:<nombre>:integrationTest`.

**Hallazgo de entorno, no de código:** `generateJooq` (dependencia de compilar cada servicio)
usa por defecto `jdbc:postgresql://127.0.0.1:5433/pasanaku`, y en esta máquina el puerto 5433
lo tiene un proyecto ajeno — el contenedor real `aportaya-postgres` de este repo está en
**`127.0.0.1:5543`** (confirmado con `docker port aportaya-postgres`), no en 5435 como decía el
`local-override.postgres.yml` (ese archivo no es el que levantó el contenedor que está corriendo
ahora). Se pasó `BD_URL_ADMIN=jdbc:postgresql://127.0.0.1:5543/pasanaku` (+ usuario/clave
`pasanaku`) como variables de entorno al invocar Gradle. Con eso:

```
BUILD SUCCESSFUL in 41m 10s
```

Los 14 servicios arrancan con el bean `idempotencia()` exigiendo ahora un `Reloj` — ninguno se
rompió.

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

1. `git log --oneline -5` debe mostrar `ee60ed7` como HEAD (o más nuevo, si ya se hizo el PR/merge
   de H1.S1).
2. Si `ee60ed7` no tiene PR abierto todavía: `git push -u origin HEAD`,
   `gh pr create --base dev --fill --title "fix(idempotencia): identidad completa H1.S1"`,
   `gh pr merge --rebase` (si el clasificador de permisos lo deniega, anotarlo acá y seguir — no
   es bloqueante).
3. Después del merge: `git fetch origin && git push origin origin/dev:test`.
4. Siguiente: H1.S2 (expiración a config `aportaya.idempotencia.vigencia`, sin literal; 50 hilos
   y "en proceso" con más detalle; `respuesta_idempotente` en otros esquemas vía
   `scripts/generar_ddl.py` — micro-PR al troncal) y H1.S3 (regla de barrido
   `SinClaveIdempotenciaSuelta` + ADR-046 ya enlazado + merge de H1 completo).
5. Recordar para cualquier `generateJooq`/`ArranqueTest`: `BD_URL_ADMIN=jdbc:postgresql://127.0.0.1:5543/pasanaku`
   (verificar con `docker port aportaya-postgres` por si el contenedor se reinició en otro puerto).
6. Recordar que `spotlessApply` sin acotar a `:plataforma:comun-web:...` reformatea TODO el repo
   incluyendo `servicios/**` — correrlo acotado o revisar `git status` y `git checkout --
   servicios/` antes de commitear.
