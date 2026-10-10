# ALR-04 · Conciliación vencida

Regla: `despliegue/alertas/reglas.yml` (`ALR-04`, severidad CRITICA). Responsable: rol `TESORERIA`.
Escala a: `OFICIAL_CUMPLIMIENTO`. Evento de aviso: `OPERACION_CONCILIACION_VENCIDA`.

## Síntomas

- Una `cuenta_custodia` en estado `ACTIVA` no tiene ninguna `conciliacion_custodia` con `ejecutada_en` dentro
  de las últimas `conciliacion_horas` horas (valor sintético, ver `reglas.yml`).
- La evidencia muestra la cuenta (id acortado), su tipo, moneda y la fecha de su última conciliación.

## Diagnóstico

1. **¿Corrió el trabajo de conciliación?** Revisar el cierre diario (`aportes.cierre_diario`) y el último
   resultado del job. Si no corrió: causa de infraestructura o de calendario (día no hábil mal calculado).
2. **¿Falló por datos?** Falta de extracto bancario del día, o saldo según banco sin cargar
   (`cuenta_custodia.fecha_saldo` vieja).
3. **¿Hay una conciliación `EN_ANALISIS` colgada?** Cuenta como no cerrada si nadie la resolvió.

## Acciones seguras

- Cargar el extracto faltante por la vía existente y **ejecutar la conciliación** (es un registro nuevo).
- Mientras no concilie, tratar la cuenta como en posible brecha: revisar [[ALR-01-brecha-de-caja]] con el
  último dato disponible y limitar salidas si el hueco supera un día hábil.
- Nunca crear una conciliación "cuadrada" a mano sin extracto: sería fabricar evidencia.

## Recuperación

- Se resuelve con una conciliación nueva dentro de la ventana. La falta de conciliaciones del período
  queda como hallazgo (`hallazgo_auditoria`) con su causa.

## Validación

```bash
python despliegue/alertas/evaluar_alarmas.py --psql "<comando psql>" --registro alarmas.jsonl
# ALR-04 RESUELTA
```

## Escalamiento

- Más de un día hábil sin conciliar una cuenta con saldo: `OFICIAL_CUMPLIMIENTO` (es un control de
  continuidad y de encaje regulatorio).
- La ventana de horas es **decisión de tesorería y cumplimiento**: hoy es sintética.

## Ver también

[[ALR-01-brecha-de-caja]] · [[backup-recovery]]
