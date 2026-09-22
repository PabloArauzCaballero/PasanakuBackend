# Carril PR3 — Plataforma/Infra (Leo, turno noche 2026-09-21)

> **AVANCE: 11 / 49 — 22,4 %.**
> **Estado:** `IN_PROGRESS`. PRs [#4](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/4),
> [#9](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/9),
> [#10](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/10),
> [#12](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/12) **mergeados** (ninguno
> bloqueado por el clasificador de permisos), todos espejados a `test`. **H1 cerrado salvo
> H1.S2.M3**, que queda `TODO` con un hallazgo real registrado (no es un simple "falta hacerlo":
> un gap en cómo `restricciones.sql` aplica sus `CHECK` cuando la misma tabla vive en más de un
> esquema — ver §Hallazgo H1.S2.M3). Pasando a H2 (outbox/Relevo).

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
| H1 — Idempotencia | 12 | 10 | EN CURSO — solo H1.S2.M3 TODO (hallazgo real, no bloqueante) |
| H2 — Outbox/Relevo | 20 | 0 | TODO |
| H3 — Guardas comunes | 9 | 1 | EN CURSO — H3.S3.M1 mergeado; resto TODO |
| H4 — Barridos/Dinero/logs/probes | 8 | 0 | TODO |
| **TOTAL** | **49** | **11** | |

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

## H1.S2 — resumen

| ID | Qué se hizo | Resultado |
|---|---|---|
| H1.S2.M1 | `aportaya.idempotencia.vigencia` (default `PT24H`) externalizada: `Idempotencia(esquema, reloj, vigencia)`, bean en `ConfiguracionComunWeb` con `@Value("${aportaya.idempotencia.vigencia:PT24H}")`. La constante `Duration.ofDays(1)` queda solo como default de los constructores de conveniencia (pruebas) | **PASS** — gate local completo (`spotlessApply`+`test`+`webTest`+`integrationTest`+`spotlessCheck` de `comun-web`), `BUILD SUCCESSFUL in 8m 44s` |
| H1.S2.M2 | "Reserva en proceso → 409" y "50 hilos → 1 efecto, 49×409" | **Ya cubierto** por `IdempotenciaRepositorioTest` (H1.S1.M2): `falloTrasReservarReintentoPosible` y `cincuentaHilosUnaReserva`, ambos PASS en la corrida 3 de H1.S1. No hizo falta código nuevo |
| H1.S2.M3 | `respuesta_idempotente` en `grupos` e `identidad` (los esquemas que adoptan el helper per ADR-046) vía `.puml` → `generar_ddl.py` | **TODO — hallazgo real, no un simple pendiente** (ver abajo) |

### Hallazgo H1.S2.M3 — `restricciones.sql` no escala a una tabla en más de un esquema

Agregué el entity `respuesta_idempotente` a `docs/entidades/01_identidad_usuarios.puml` y
`docs/entidades/02_grupos_turnos.puml` (mismo shape que `10_billetera_custodia.puml`) y corrí
`python scripts/generar_ddl.py` — generó limpio: `sql/ generado: 307 tablas · 632 claves foráneas
· 676 índices · 421 CHECK`, sin errores.

Pero **antes de commitear** revisé `sql/40_reglas/restricciones.sql` (generado por
`scripts/extraer_sql.py` desde `docs/Restricciones.md`, un pipeline **distinto** al de
`generar_ddl.py`) y encontré que R-BIL-19 aplica sus tres `CHECK` así:

```sql
ALTER TABLE respuesta_idempotente
  ADD CONSTRAINT ck_respuesta_idem_hash CHECK (length(hash_solicitud) = 64),
  ADD CONSTRAINT ck_respuesta_idem_expira CHECK (expira_en > registrada_en),
  ADD CONSTRAINT ck_respuesta_idem_http CHECK (codigo_http BETWEEN 100 AND 599);
```

**Sin calificar el esquema.** `sql/aplicar.sql` corre este archivo una sola vez con
`SET search_path TO aportes, auditoria, ..., grupos, identidad, ..., nucleo_financiero, ...`
— en Postgres, una referencia de tabla sin esquema resuelve contra el **primer** esquema del
`search_path` que la tenga. Con solo `nucleo_financiero.respuesta_idempotente` (como hoy) esto no
se nota. En cuanto agregué las de `grupos` e `identidad` (que preceden a `nucleo_financiero` en el
`search_path`), la sentencia de arriba pasaría a aplicar los tres `CHECK` **solo a
`grupos.respuesta_idempotente`**, dejando `identidad.respuesta_idempotente` y
`nucleo_financiero.respuesta_idempotente` sin la validación de hash/vencimiento/rango HTTP —
silenciosamente, sin ningún error en `generar_ddl.py` ni en `verificar_boveda.py` (ninguno de los
dos sabe de esta ambigüedad de `search_path`).

**Revertido `sql/` y `docs/entidades/*.puml`** (`git checkout --`) para no dejar un estado a medio
arreglar en el troncal: mergear esto tal cual habría sido peor que no tocarlo — un defecto real y
silencioso en datos de producción (una tabla de idempotencia sin sus checks de integridad).

**Lo que hace falta para cerrar H1.S2.M3 de verdad** (no lo hago yo: `docs/Restricciones.md` y
`scripts/extraer_sql.py` no están en mis reservas de este carril): calificar
`ALTER TABLE respuesta_idempotente` como `ALTER TABLE <esquema>.respuesta_idempotente` para cada
esquema que la tenga —o generar el bloque R-BIL-19 una vez por esquema en vez de una sola vez—.
Es un cambio al generador de restricciones, no a `generar_ddl.py`. Registrado acá y en mi daily
§6 para quien tenga ese archivo (posiblemente Pablo, dueño de `docs/Restricciones.md` en el reparto
original) — **no es un bloqueo del carril**: sigo con H1.S3 sin esperar.

## H1.S3 — resumen

| ID | Qué se hizo | Resultado |
|---|---|---|
| H1.S3.M1 | `ADR-046` enlazado en `docs/Arquitectura/_Arquitectura.md` (micro-PR troncal); `python3 scripts/verificar_boveda.py` encontró 3 FALLAs reales que mi propio ADR había introducido (secciones obligatorias faltantes, wikilink roto a un ADR-047 que todavía no existe, cifra de "46 ADR" desfasada en `planes/00 Plan maestro.md`) — las tres corregidas | **PASS** — `TODO OK` |
| H1.S3.M2 | `ClaveIdempotenciaSuelta` en `comun-pruebas/barrido`: detecta `.where(DSL.field("clave_idempotencia")` sin `.and(...)` en la misma sentencia. **Opt-in por módulo** (no en la plantilla de `nuevo_servicio.py`): aplicarla a ciegas en todo el monorepo gritaría sobre columnas `clave_idempotencia` de otros servicios que no usan el helper `Idempotencia` y tienen su propio diseño de unicidad — la maquiné para correr solo donde `Idempotencia.java` vive, vía un `BarridoTest` nuevo en `comun-web` (que no existía: ver hallazgo abajo) | **PASS** — ver evidencia |

### Evidencia H1.S3.M2 — negativo provocado y revertido

`ReglasPropiasTest` (dos pruebas, no un provocar-y-revertir manual sobre código real): `claveIdempotenciaSueltaSeDetecta` escribe un `.where(clave_idempotencia)` sin `.and()` en un archivo temporal y confirma que `ClaveIdempotenciaSuelta.revisar(...)` lo encuentra con archivo y línea exactos; `claveIdempotenciaSueltaNoGritaConAnd` confirma que la misma clave CON `.and(usuario_id...)` no dispara nada. Corrida real:

```
./gradlew :plataforma:comun-pruebas:test :plataforma:comun-web:testBarrido ...
BUILD SUCCESSFUL in 3m 25s
```

### Hallazgo — `comun-web` no tenía `BarridoTest` (encontrado al escribir H1.S3.M2)

Al conectar la regla nueva descubrí que **ningún módulo de `plataforma/` tiene su propio
`BarridoTest`** (la plantilla de `nuevo_servicio.py` genera uno por servicio, no por módulo de
plataforma). Al agregar `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/BarridoTest.java`
por primera vez, correr las reglas EXISTENTES contra `comun-web` (nunca antes ejercidas ahí) encontró
dos hallazgos reales de mi propio código de esta sesión, arreglados en el mismo commit:

- **Falso positivo de `sin-umbral-literal` sobre mi propia constante**: `UMBRAL_TRANSITORIO = 500`
  (un código HTTP, no dinero) matcheaba la regex de la regla por el prefijo `UMBRAL`. Renombrada a
  `CODIGO_HTTP_SERVIDOR` — ningún umbral real, ninguna cifra regulatoria, solo una coincidencia de
  nombre.
- **`tamano-archivo`**: `IdempotenciaRepositorioTest.java` había crecido a 338 líneas (límite 300).
  Partido en tres archivos, mismo patrón que `CU10Test`/`CU10ConcurrenciaTest` en
  `nucleo-financiero`: `BaseIdempotenciaRepositorioTest` (helpers compartidos, 109 líneas),
  `IdempotenciaRepositorioTest` (6 casos, 135 líneas) e `IdempotenciaConcurrenciaRepositorioTest`
  (50 hilos + los 3 casos de recuperación, 132 líneas).

**Hallazgo para otros carriles, no auto-detectado por esta regla** (porque `ClaveIdempotenciaSuelta`
solo corre donde alguien la conecta, y no toco `servicios/**`): `grep` encontró
`.where(DSL.field("clave_idempotencia")` **sin** `.and(...)` en la misma sentencia en
`servicios/aportes/.../PagoRepositorio.java:56`, `servicios/notificaciones/.../EnvioRepositorio.java:66`
y `servicios/organizador/.../AutomatizacionRepositorio.java:87`. Puede ser intencional (esas tablas
podrían tener su propio diseño de unicidad de `clave_idempotencia`, distinto del helper
`Idempotencia`/`respuesta_idempotente` de este carril) — no lo investigué a fondo porque
`servicios/**` está fuera de mi alcance. Registrado para sus dueños.

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
