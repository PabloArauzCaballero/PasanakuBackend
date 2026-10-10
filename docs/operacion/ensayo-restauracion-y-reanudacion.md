# Runbook — Restauración de un respaldo y reanudación de eventos (ensayado)

Complementa [[backup-recovery]]: ese documento dice **cómo se respalda y restaura**; éste agrega lo que
faltaba para poder **reabrir**: la conciliación del mayor como compuerta, la reanudación de los eventos y el
ensayo con tiempos observados. H13.S1.M4 · E-H13-04.

El ensayo se corre con `despliegue/ensayos/restauracion/ensayo_restauracion.sh` (PostgreSQL desechable en Docker,
puerto 5439, bajo el guardián de RAM). Todo dato es sintético. **Nunca** apuntarlo a una base viva.

## Síntomas

- Pérdida o corrupción del volumen de Postgres confirmada (si el contenedor solo está caído, alcanza [[postgres-down]]).
- Hay que volver a un punto de recuperación y reabrir el sistema **sin** haber perdido ni duplicado dinero.

## Diagnóstico

1. **¿Cuál es el último punto de recuperación utilizable?** El más reciente cuyo manifiesto (`manifiesto.sql`,
   guardado FUERA de la base al respaldar) se conserve. Un respaldo sin manifiesto no se puede conciliar.
2. **¿Qué se perdió?** Todo lo posterior al respaldo (RPO). El ensayo lo lista: son exactamente los asientos con
   fecha posterior al punto de recuperación, y se explican contra la fuente externa (extracto bancario, eventos
   del proveedor).

## Acciones seguras (en este orden)

1. **Instancia nueva y vacía** con la misma configuración de base (`despliegue/compose/init/00-arranque.sql`:
   `ALTER DATABASE … SET search_path`, que **no viaja en `pg_dump`**).
2. **Roles primero**: `pg_dumpall --globals-only` restaurado con `ON_ERROR_STOP=0`. El dump intenta
   `CREATE ROLE pasanaku`, que ya existe (superusuario de arranque); con `ON_ERROR_STOP=1` ese primer error aborta
   el resto y **ningún rol de servicio llega a crearse**: pasó en la primera corrida del ensayo
   (`restauracion-corrida-1-FALLA-roles.log`: 715 errores en `pg_restore`, `role "rol_aplicacion" does not exist`).
3. `pg_restore --no-owner --role=pasanaku` sobre la base vacía.
4. **Conciliación del mayor — compuerta de reapertura** (`conciliacion_del_mayor.sql`, 17 comprobaciones). Si UNA
   es `f`, **no se reabre**. Cubre: cada asiento cuadra por moneda, debe = haber, sin asientos vacíos ni
   movimientos huérfanos, totales y huella del libro iguales al manifiesto, saldo almacenado = saldo recalculado,
   secuencia de asientos no retrocede, append-only y privilegios restaurados, cuadre diferido y moneda única,
   políticas de fila, `search_path` de la base, y que el outbox sea coherente con el mayor.
5. **Reponer lo perdido desde la fuente externa** con las mismas claves (determinísticas): el mayor repuesto
   debe quedar **idéntico** al del origen (misma huella). Nunca editar el mayor: se repone con asientos nuevos
   y, si algo estaba mal, con contra-asiento (91.1.3).
6. **Reanudar los eventos recién con la compuerta abierta** (`reanudacion_de_eventos.sql`): el relevo toma lo
   `PENDIENTE` y los `TOMADO` huérfanos, publica y marca; el consumidor aplica una sola vez por
   `(id_evento, consumidor)` y la **reentrega duplicada no aplica nada**. Publicar sobre un libro sin conciliar
   propaga el daño.

## Recuperación — qué se mide

Medido el 2026-10-09 en la máquina de desarrollo (Docker Desktop, `postgres:16`, 436 tablas, 400 asientos
sintéticos + 7 posteriores, dump de 3,0 MB). Salida literal en
`docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-S2/restauracion-corrida-2-PASS.log`.

| Medida | Valor observado | Lectura honesta |
|---|---|---|
| Respaldo (`pg_dump` + roles) | ~0,7 s | tamaño de juguete: **no** extrapola a producción |
| `pg_restore` | ~5,6 s | idem |
| Conciliación del mayor (17 comprobaciones) | ~0,2 s | crece con el libro; las consultas son agregadas completas |
| RTO medido (instancia vacía → compuerta abierta → eventos reanudados) | ~15 s | cota inferior: sin red, sin disco de producción, sin volumen real |
| RPO observado | 3 s | lo fija la frecuencia del respaldo; con `pg_dump` manual es lo que se haga a mano |
| Diferencia con el origen tras restaurar | 7 asientos | **explicada**: exactamente los posteriores al respaldo; repuestos desde la fuente, el mayor queda idéntico |

**Esto NO es el RPO/RTO de producción.** Producción necesita PITR con WAL continuo (decisión AMB-8, sin
implementar) y un volumen real; el número verdadero se mide con ese ensayo, no con éste.

## Validación — la compuerta tiene que poder CERRARSE

El ensayo inyecta tres defectos y exige que la compuerta se cierre en cada uno (no solo que pase en el caso feliz):

| Incidencia inyectada | Comprobación que cierra | Reparación |
|---|---|---|
| `ALTER DATABASE … RESET search_path` (se perdió con el dump) | C14 | reaplicar el arranque de la base |
| Un saldo almacenado deja de coincidir con el mayor | C08 | `fn_ctb_recalcular_saldo` **desde el mayor** (no se edita a mano) |
| La secuencia de asientos retrocede | C09 | `setval` al máximo existente |

Tras reparar, la compuerta vuelve a abrir. Si la compuerta no se cierra con un defecto, el ensayo falla.

## Escalamiento

- Cualquier comprobación `f` que no sea una de las tres reparables arriba: **no reabrir**, escalar a
  `OFICIAL_CUMPLIMIENTO` y `TESORERIA`; el mayor descuadrado es un incidente regulatorio.
- Restaurar producción: siempre dos personas (ver [[backup-recovery]]).
- Fuera de este ensayo y sin cerrar: cifrado y custodia del respaldo, retención, restauración de MinIO y
  restauración en staging **con anonimización** (90.2.6); `pg_dumpall --globals-only` incluye hashes de
  contraseña de los roles: el archivo de roles es un secreto, no se guarda junto al dump ni en el repo.

## Ver también

[[backup-recovery]] · [[outbox-backlog]] · [[ALR-03-cola-detenida]] · [[postgres-down]]
