# F1.L1.M08 parte 2 · Propuesta: sellar `aportes.webhook_pasarela` sin perder la retención ni la idempotencia

- Fecha: 2026-11-21 · Para decidir: **Pablo** · Estado: propuesta, sin implementar.

## El conflicto

- **Hoy.** `webhook_pasarela` guarda cada aviso de la pasarela en crudo. `WebhookRepositorio.marcarProcesado` lo
  actualiza una vez con `estado`, `procesado_en`, `pago_id` e `intentos_procesamiento`.
- **Lo que pide el anexo.** Que sea un hecho inmutable, como ya lo son `pago` y `reembolso` (R-BIL-30).
- **La retención.** La política de `60_semillas/19` dice 12 meses en caliente, 24 en total, acción `ELIMINAR`.
- **R-AUD-01.** Una tabla sellada no admite `DELETE`. El repo ya resuelve esto para `bitacora_evento` y
  `registro_acceso_datos`: tabla **particionada por mes**, y lo vencido se purga con `DETACH` + `DROP` de la
  partición (`PARTICIONADAS` en `scripts/modelo.py`).
- **El choque.** La idempotencia de los avisos es `uq_webhook_idem (proveedor_id, clave_idempotencia)`. En
  PostgreSQL, un `UNIQUE` de una tabla particionada **tiene que incluir la columna de partición**
  (`recibido_en`). Así, el mismo aviso reenviado en otro mes ya no choca, y la pasarela reenvía.

## Volumen esperado

No hay pasarela contratada (P-15), así que no hay dato medido. Se estima por fórmula, no por invento:
- avisos por mes ≈ pagos por mes × (1 + reenvíos);
- pagos por mes ≈ participantes activos × cuotas por mes.

Con 10.000 participantes, una cuota mensual y 2 reenvíos por aviso: unas 30.000 filas por mes, 720.000 en 24 meses.
Cada fila pesa pocos KB (el payload crudo). Esto **no exige** particionar por rendimiento: la partición se justifica
solo por la purga.

## Opción A · Partición mensual + tabla de deduplicación aparte

- `webhook_pasarela` particionada por `recibido_en` (mes) y sellada (append-only).
- **Deduplicación.** Una tabla chica `webhook_clave` con `(proveedor_id, clave_idempotencia)` como PK y
  `webhook_id` + `recibido_en`. No se particiona, así que el `UNIQUE` es global. El alta del aviso inserta las dos
  filas en la misma transacción; el conflicto en `webhook_clave` es el duplicado.
- **Estado.** Pasa a `historial_estado_webhook`, append-only, con una sola salida por estado, como `pago`.
- **Purga.**
  - Lo vencido se va con `DETACH`/`DROP` de la partición: sin `UPDATE` ni `DELETE` sobre datos de dinero.
  - `webhook_clave` sí necesita purga. No es un hecho de dinero, es un índice de deduplicación, así que no se sella
    y se borra con `DELETE … WHERE recibido_en < corte` dentro del mismo trabajo.
  - El historial se particiona igual, o se borra en cascada al soltar la partición.
- **Costo.** 2 tablas nuevas, cambios en `WebhookRepositorio` y `PlataformaDePagosRepositorio`, y un trabajo de
  purga con lock distribuido (ShedLock).

## Opción B · Sin partición: sellar la fila cruda y mover lo mutable

- `webhook_pasarela` deja de tener columnas mutables: el estado, `procesado_en`, `pago_id` y los intentos se van a
  `historial_estado_webhook`. Se sella (append-only).
- La idempotencia queda como está: `uq_webhook_idem` no cambia.
- **Purga.** Se cambia la acción de retención de `ELIMINAR` a **`ANONIMIZAR`**: a los 24 meses se borra
  `payload_crudo` y `firma` con un rol de mantenimiento autorizado, con la excepción declarada en R-AUD-01. También
  se puede dejar sin purga, porque el volumen lo permite.
  - Es un `UPDATE` sobre la fila cruda, así que necesita **una excepción explícita a R-AUD-01** y la decisión de
    retención de Pablo.
  - Alternativa: no purgar nunca y cambiar la política a `ARCHIVAR`, porque el aviso es respaldo de una conciliación.
- **Costo.** 1 tabla nueva y cambios en 2 repositorios. Sin trabajo de partición.

## Recomendación: **Opción A**

- Cumple las dos reglas sin excepciones: los datos de dinero no se tocan nunca (91.1.3 y R-AUD-01), y la retención
  se respeta como está.
- La deduplicación global queda en una tabla cuyo único propósito es deduplicar.
- Reusa el mecanismo de partición que el repo ya tiene probado (`bitacora_evento`).
- B es más barata, pero obliga a una excepción a R-AUD-01 o a cambiar una política de retención. Las dos cosas son
  de Pablo y de Cumplimiento, y abren un precedente.

**Para decidir:**
1. A o B.
2. Si B: ¿la política pasa a `ARCHIVAR` o se aprueba la excepción de anonimizar?
3. ¿Confirmamos los 24 meses para el respaldo de la conciliación?
