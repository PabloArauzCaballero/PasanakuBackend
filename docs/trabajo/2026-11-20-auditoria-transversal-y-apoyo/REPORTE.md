# Reporte — Auditoría L5 `transversal-y-apoyo` (solo lectura)

> **AVANCE: 9 / 9 — 100 %.** (de la auditoría; los 88 hallazgos quedan abiertos para sus dueños y las pruebas de integración siguen BLOQUEADAS por entorno)

- Fecha: 2026-11-20 · Plan: [PLAN.md](./PLAN.md) · Entregable: [AUDITORIA-transversal-y-apoyo.md](./AUDITORIA-transversal-y-apoyo.md) · Rama: `auditoria/transversal-y-apoyo-2026-11-20` (desde `origin/test` 07fc6971)
- Peldaño de evidencia alcanzado: el del área más baja es `DISCOVERED` (hallazgos por lectura). `VERIFIED` para los hallazgos ejecutados en base local. `TESTED` para las suites unitarias, web, de contrato y de barrido. Integración `BLOCKED`. Pantallas `UNKNOWN`.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Rama desde `origin/test` 07fc6971; `dev` 850e0f51 sin diferencias en el alcance; historial completo | `git rev-parse --is-shallow-repository` · `git diff --shortstat origin/test origin/dev -- <alcance>` | PASS: `false`; diff vacío |
| H1.S1.M2 | Huella (1 282 archivos, 279 181 líneas) y mapa de conexiones | `git ls-files <alcance>` + `grep` de `@KafkaListener`, clientes HTTP y `rutas.yml` | PASS: §3 del informe |
| H1.S1.M3 | Anexo de cobertura por archivo | `python3 anexo.py` (en el scratchpad de la sesión) | PASS: §12. 395 archivos leídos completos; el resto declarado parcial, por script o inventario |
| H1.S1.M4 | Inventarios de aprobaciones (12) y de respaldo (11); A-1 a A-4 sin resolver | grep de §6 del metaprompt + esquema | PASS: §4 |
| H1.S1.M5 | 88 hallazgos con ficha completa (5 S1, 34 S2, 41 S3, 8 S4) | conteo por `grep "Clase / Severidad"` | PASS: coincide con la tabla del resumen |
| H1.S1.M6 | Matriz de contratos y verificadores del repo ejecutados | `scripts/*.py` (ver Evidencia) · `inventario_endpoints.py --check` | PASS: §6. Salidas en `evidencia/v-*.txt` |
| H1.S1.M7 | Estándar y regla en cada ficha | `awk` sobre las fichas | PASS: 88/88 |
| H1.S1.M9 | Commit y push solo de esta carpeta | `git push -u origin auditoria/transversal-y-apoyo-2026-11-20` | PASS: `* [new branch] auditoria/transversal-y-apoyo-2026-11-20` (commit `da759555`) |
| H1.S1.M8 | Refutación de los S1 y S2 | reapertura y ejecución en base local | PASS: §11 «Refutación»; ninguno retirado. TYA-DATOS-001 se mantiene en S2 |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Pruebas de integración de los 7 servicios (256 casos: 242 en rojo, 14 en verde en inversiones) | BLOQUEADO (`ENVIRONMENT`) | Docker para Testcontainers, o CI. Fallo literal: `Could not find a valid Docker environment` |
| Auditoría de pantallas de `apps/*` que consumen estos servicios | fuera de esta sesión | lote L6 o el lote de cada módulo |
| Decisiones A-1 a A-4 | `DECISION_REQUIRED` | Pablo; Cumplimiento para A-1 |

## Evidencia
Índice de `evidencia/`:
- `v-*.txt`: salidas de los verificadores de `scripts/`.
- `g-*.txt`: salidas de Gradle.
- `db-*.txt`: base local efímera.

```text
$ psql -v ON_ERROR_STOP=1 -f sql/aplicar.sql            → exit=0
$ psql -f sql/60_semillas/sembrar.sql  (×3)               → exit=0 ×3; corrida 2 vs 3: diff de conteos = 0 líneas (1 082 filas)
$ psql -f sql/50_verificacion/prueba_humo.sql             → 165 OK · 0 FALLA
$ psql -f sql/50_verificacion/verificaciones.sql          → 16 consultas, (0 rows) en todas
$ experimento de mayor (db-experimento-mayor.txt)         → E2 «debe=150.00 haber=100.00» aceptado; E3 negativos aceptados; E4 control rechazado R-AUD-05
$ SET ROLE svc_erp; SELECT … FROM nucleo_financiero.asiento_contable → ERROR: permission denied for schema nucleo_financiero
$ SET ROLE svc_publicidad; SELECT aportes.fn_uif_titular(gen_random_uuid()) → ejecuta sin error
$ ./gradlew :servicios:<7>:{test,webTest,contractTest,testBarrido} → 503 pruebas, 0 fallos (g-resumen-servicios.txt)
$ ./gradlew :servicios:<7>:integrationTest               → 256 casos: 242 en rojo, todos por falta de Docker (o contexto que no levanta por esa causa); 14 en verde
$ ./gradlew :plataforma:<6>:test                          → 186 pruebas, 0 fallos (2.º intento; el 1.º falló con HTTP 429 de Maven Central)
$ python3 scripts/verificar_seguridad.py                  → TODO OK · 7 aviso(s)
$ python3 scripts/inventario_endpoints.py --check         → endpoints relevados: 350 · hallazgos: 0
$ python3 scripts/verificar_rutas_frontend.py             → exit 1 (lista de conocidas desactualizada; fuera de alcance)
$ python3 scripts/auditar_backend.py                      → nota 9.64/10 · «0/393 pruebas verificadas»
```

## No cubierto
- **Pruebas de servicio:** las 250 clases de prueba se ejecutaron pero no se leyeron. Integración bloqueada.
- **Lectura parcial:** ~60 % del código main de `inversiones`; partes de `erp`, `publicidad` y `tarifas` (§11 del informe); ~2 800 líneas de `restricciones.sql`; la mayoría de los DDL de tabla, revisados solo por script.
- **Prueba de idempotencia de endpoints de dinero:** no ejecutada. Tampoco la prueba de duplicado de mensaje, la del dependido apagado ni la traza con `correlationId` (necesitan Kafka y servicios levantados).
- **Pantallas y UX-A11Y:** no revisadas.
- **`yarn lint` y `yarn typecheck`:** no corridos (el frontend no está en este lote).
- **Hallazgos *probable*:** vienen de lectura delegada y no se reabrieron línea por línea.

## Desvíos del plan
- **Base local efímera:** se levantó un PostgreSQL 16 en `/tmp/pg-l5`, puerto 5433, con binarios ya instalados. Sirvió para aplicar el esquema y para destrabar `generateJooq`. Se apagó y se borró. No se tocó TEST.
- **Reintento de Gradle:** la corrida de `plataforma` se reintentó una vez por un HTTP 429 de Maven Central.
- **Verificadores en modo sin escritura:** `matriz_de_pruebas.py` y `inventario_endpoints.py` se corrieron con `--servicio` y `--check` para no escribir en `docs/`.

## Riesgos residuales
- Cinco S1 abiertos: el mayor acepta descuadres tardíos, el esquema admite pagos sin QR, y hay tres agujeros en `tarifas`.
- Mientras siga el `DefaultErrorHandler` actual, cualquier caída de más de ~10 s puede perder eventos de dinero.

## Decisiones y ambigüedades
- **A-1 a A-4:** registradas en el §4c del informe. No se resolvió ninguna.
- **Supuestos** (a confirmar con Pablo):
  - Las pantallas no son de L5.
  - El fondo de garantía mutual no entra en R-NUEVA-2 salvo decisión.
  - Prefijo de hallazgos `TYA`.
- **Procesos en background:** no queda ninguno. PostgreSQL detenido; daemon de Gradle detenido (`./gradlew --stop`); los 4 subagentes terminaron.

## Entrega
```text
$ git push -u origin auditoria/transversal-y-apoyo-2026-11-20
 * [new branch]        auditoria/transversal-y-apoyo-2026-11-20 -> auditoria/transversal-y-apoyo-2026-11-20
```
Sin PR ni merge, como pide el metaprompt.

## Constancia del candado `blocker_gate.py` (regla 65)
Al cerrar el turno, el candado listó 9 microtareas en `BLOQUEADO`/`EN CURSO`. Todas pertenecen a planes **ajenos a este trabajo**:
- `2026-10-03-alta-fecha-actividad-autocaptura` (H4.S1.M2, H5.S2.M6, H7.S1.M2, H7.S2.M1)
- `2026-10-14-resolver-conflictos-prs-60-63` (H3.S1.M2)
- `2026-10-16-ci-formato-test` (H1.S2.M3)
- `2026-10-27-rediseno-backoffice-al-repo-desplegado` (H1.S2.M1)
- `2026-10-28-admision-directa-backoffice` (H3.S1.M3)
- `2026-10-30-arreglos-test-y-espejo` (H1.S2.M1)

No se modificaron. Esta auditoría es de solo lectura y solo escribe en su carpeta (metaprompt §0 y §3; regla 00 §3). El plan de esta auditoría no tiene microtareas en `BLOQUEADO` ni `EN CURSO`. Lo pendiente de esos planes queda para sus dueños.
