# ALR-02 · Atraso de aportes

Regla: `despliegue/alertas/reglas.yml` (`ALR-02`, severidad ALTA). Responsable: rol `SOPORTE`.
Escala a: `ADMIN_PLATAFORMA`. Evento de aviso: `OPERACION_ATRASO_DE_APORTES`.

## Síntomas

- Hay filas de `aportes.obligacion_aporte` con `saldo_pendiente > 0`, fuera de estados finales
  (`PAGADO`, `ANULADO`, `CONDONADO`, `EXONERADO`, `CUBIERTO_POR_GARANTIA`) y con `fecha_fin_gracia` vencida hace
  más de `atraso_dias` días (valor sintético, ver `reglas.yml`).
- La evidencia de la alarma agrupa por grupo (id acortado) y moneda: cuántas obligaciones y cuánto
  pendiente. No lleva nombres ni documentos.

## Diagnóstico

1. **¿El pago existe y no se aplicó?** Revisar webhooks y conciliación de pagos (`aportes.pago`,
   `webhook_pasarela`, `conciliacion`): un pago confirmado por la pasarela y sin aplicar es un problema de
   conciliación, no un atraso del participante.
2. **¿Está en debido proceso?** Mirar si ya existe expediente de incumplimiento
   (`garantia`, CU-25): un caso en revisión **no** es sanción firme ni deuda ejecutable
   (H13.S1.M6, regla 91.4).
3. **¿Cubre la garantía?** Estado `CUBIERTO_POR_GARANTIA` o cobertura en curso.

## Acciones seguras

- Contactar al participante por el canal verificado y registrar el aviso (el plazo del descargo corre
  recién con la notificación probada: skill `debido-proceso`).
- Si no hay expediente, **abrirlo**; no marcar mora a mano ni ejecutar garantía sin plazo de descargo
  cumplido (regla 91.4.4).
- Si el pago ya llegó: re-aplicar el webhook por la vía idempotente existente (nunca insertar un abono a mano).

## Recuperación

- La alarma se resuelve cuando la obligación pasa a un estado final o sale de la ventana. La mora y sus
  recargos los calcula la política de mora (`aportes.politica_mora`); no se editan.

## Validación

```bash
python despliegue/alertas/evaluar_alarmas.py --psql "<comando psql>" --registro alarmas.jsonl
# ALR-02 RESUELTA; sin obligaciones fuera de gracia sin proceso
```

## Escalamiento

- Atrasos que crecen o que afectan a un grupo entero: `ADMIN_PLATAFORMA`; si compromete el pozo de una
  entrega ya programada, tratarlo como riesgo financiero (no solo operativo).
- El umbral de días **es una decisión de negocio** (Dirección de Operaciones): hoy es sintético.

## Ver también

[[ALR-03-cola-detenida]] · [[outbox-backlog]]
