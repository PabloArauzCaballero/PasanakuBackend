# ALR-01 · Brecha de caja

Regla: `despliegue/alertas/reglas.yml` (`ALR-01`, severidad CRITICA). Responsable: rol
`TESORERIA`. Escala a: `OFICIAL_CUMPLIMIENTO`. Evento de aviso: `OPERACION_BRECHA_DE_CAJA`.

## Síntomas

- La última conciliación de una cuenta de custodia (`nucleo_financiero.conciliacion_custodia`) tiene
  `saldo_custodia < saldo_dinero_electronico`: la plataforma debe a los participantes más de lo que
  tiene en custodia. `diferencia` es negativa y `ratio_cobertura` menor a 1.
- Suele venir con `cumple_encaje = false` y un `descuadre_custodia` abierto.

## Diagnóstico

1. **¿Es real o es tránsito?** Mirar `saldo_en_transito` de esa conciliación: una recarga acreditada en el
   libro y todavía no abonada por el banco es un desfase temporal (`DESFASE_TEMPORAL`), no un faltante.
   Un desfase temporal se espera que cierre solo en la próxima conciliación; un faltante no.
2. **¿El libro cuadra?** Correr la conciliación del mayor
   (`despliegue/ensayos/restauracion/conciliacion_del_mayor.sql`, comprobaciones C01-C04 y C08): si el mayor
   está descuadrado el problema no es de caja sino de registro.
3. **¿Qué se movió desde la última conciliación cuadrada?** Retiros y desembolsos aprobados contra el
   extracto del banco (`aportes.extracto_bancario` / `movimiento_bancario`).

## Acciones seguras

- **Detener salidas de dinero** del producto afectado hasta cerrar la brecha (bloqueo de retiros y
  desembolsos por la vía administrativa existente). Nunca "tapar" con un asiento manual.
- Registrar el `descuadre_custodia` con tipo, severidad y plan de acción (campos obligatorios del
  modelo), y abrir incidente operativo.
- Si es faltante: reponer fondos desde la entidad y **re-conciliar**; la reposición es un movimiento
  nuevo de custodia, no un `UPDATE`.

## Recuperación

- La brecha se cierra cuando una conciliación **nueva** (no editada) muestra `saldo_custodia >=
  saldo_dinero_electronico`. El historial de la anterior queda: el mayor y las conciliaciones son
  append-only.
- Si el motivo fue un error de registro, corregir con contra-asiento (regla 91.1.3) y documentar.

## Validación

```bash
python despliegue/alertas/evaluar_alarmas.py --psql "<comando psql>" --registro alarmas.jsonl
# ALR-01 debe pasar a RESUELTA y quedar su duración en el registro
```

## Escalamiento

- Cualquier brecha CRITICA que no se cierre en el mismo día: `OFICIAL_CUMPLIMIENTO`, y es candidata a
  reporte a la autoridad (CU-44/CU-45) si hay indicio de pérdida de fondos de terceros.
- Si **nadie** tiene el rol `TESORERIA`, la alarma escala sola a `OFICIAL_CUMPLIMIENTO`; si tampoco hay
  quien lo tenga, el evaluador sale con código 4 (sin destinatario): eso es un incidente por sí mismo.

## Ver también

[[ALR-04-conciliacion-vencida]] · [[backup-recovery]] · [[withdrawal-reconciliation]]
