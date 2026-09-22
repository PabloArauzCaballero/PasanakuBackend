# Runbook — Timeout del proveedor de retiros

> Carril PR2 (`nucleo-financiero`), H4.S2. Ver [[../Arquitectura/ADR-049 Doble aprobación de retiros|ADR-049]] y `withdrawal-reconciliation` para lo que pasa DESPUÉS de un timeout, no durante.

## Síntomas

- `withdrawal_requested_total` y `withdrawal_approved_total` (Micrometer,
  `CU11RetirarSaldo`) crecen con normalidad, pero el conteo de órdenes
  `EN_PROCESO` no baja — se están instruyendo pagos y nadie confirma si
  salieron.
- Alertas de latencia o de tasa de error en las llamadas salientes de
  `nucleo-financiero` hacia el proveedor de retiros (dashboard de
  `ProveedorDeRetiro` / `resilience4j`, cuando el adaptador HTTP real de
  H4.S2 esté desplegado).
- Reclamos de usuarios: "pedí un retiro y no me llegó ni se me devolvió".

## Qué NO es una falla

`ProveedorDeRetiro.Estado.TIMEOUT` (el puerto, `dominio/puertos/ProveedorDeRetiro.java`)
es un resultado **legítimo y esperado**, no una excepción sin controlar. Un
timeout dice "no se sabe todavía", nunca "no salió" — confundirlos es la
diferencia entre reconciliar después y pagarle dos veces a alguien (o no
pagarle nunca) por asumir un rechazo que nunca pasó. Cuando `instruirPago`
recibe `TIMEOUT`, la orden pasa igual a `EN_PROCESO` (mismo camino que
`ACEPTADO`) — **nunca** se libera la retención ni se asume rechazo solo
porque la respuesta tardó.

## Dashboards y consultas

```bash
# Ordenes EN_PROCESO, la mas vieja primero — si la mas vieja tiene mas de
# unos minutos, algo se quedo sin resolver
docker exec aportaya-postgres psql -U pasanaku -d pasanaku -c \
  "SELECT id, cuenta_billetera_id, referencia_proveedor, solicitada_en
     FROM nucleo_financiero.orden_retiro
    WHERE estado = 'EN_PROCESO'
    ORDER BY solicitada_en ASC
    LIMIT 20;"

# Cuantas hay en total ahora mismo
docker exec aportaya-postgres psql -U pasanaku -d pasanaku -c \
  "SELECT count(*) FROM nucleo_financiero.orden_retiro WHERE estado = 'EN_PROCESO';"
```

Métricas de negocio (`withdrawal_requested_total`, `withdrawal_approved_total`,
`withdrawal_failed_total`, expuestas por `/actuator/prometheus` como
cualquier otro contador de Micrometer): un `approved` que crece mucho más
rápido que lo que después se confirma pagado es la señal temprana de que el
proveedor está tardando, antes de que alguien reclame.

## Diagnóstico

1. **¿Es un timeout aislado o el proveedor está caído?** Un puñado de
   órdenes `EN_PROCESO` recientes (minutos) es tráfico normal — el job de
   reconciliación (ver [[withdrawal-reconciliation]]) las resuelve solo en su
   próxima corrida. Un crecimiento sostenido, o una orden con horas de
   antigüedad, es un incidente.
2. **¿El proveedor real responde, aunque lento?** Revisar el dashboard del
   cliente resiliente (`resilience4j` — mismo patrón que `CotizadorPorHttp`,
   ver `application.yml: resilience4j.*`): si el *circuit breaker* está
   `OPEN`, el proveedor está fallando de forma sostenida, no solo lento.
3. **¿Es un problema del lado de `nucleo-financiero` (red, DNS, certificado)
   o del proveedor en sí?** Un timeout de *conexión* (no llega a establecer
   el socket) apunta a la red propia; un timeout de *lectura* tras conectar
   apunta al proveedor.
4. **¿Cuánto saldo hay retenido en órdenes `EN_PROCESO`?** Es plata que la
   persona no puede usar mientras dure la incertidumbre — cuantificarlo
   ayuda a decidir la urgencia:
   ```bash
   docker exec aportaya-postgres psql -U pasanaku -d pasanaku -c \
     "SELECT sum(monto_neto) FROM nucleo_financiero.orden_retiro WHERE estado = 'EN_PROCESO';"
   ```

## Acciones seguras

- **Nada manual sobre `orden_retiro` mientras el proveedor pueda seguir
  respondiendo tarde.** El job de reconciliación (`ReconciliacionDeRetiros`,
  `@Scheduled`, cron por defecto cada 5 minutos —
  `aportaya.retiro.reconciliacion.cron`) ya pregunta por cada orden
  `EN_PROCESO` en cada corrida; forzar un estado a mano se adelanta a una
  respuesta real que todavía puede llegar.
- Si el *circuit breaker* está `OPEN` y se confirma que el proveedor volvió
  a responder, no hace falta intervención: `resilience4j` reintenta solo en
  estado semi-abierto (`permitted-number-of-calls-in-half-open-state`).
- Si el proveedor está confirmadamente caído por un tiempo prolongado
  (anuncio del proveedor, incidente conocido): comunicar a soporte para que
  no prometa plazos a los usuarios con retiros `EN_PROCESO`, pero **no**
  tocar las órdenes — siguen retenidas correctamente hasta que se sepa.

## Recuperación

1. Cuando el proveedor vuelve a responder, la **siguiente corrida programada**
   de `ReconciliacionDeRetiros` resuelve automáticamente cada orden
   `EN_PROCESO`: `ACEPTADO` → `confirmarPago` (pasa a `PAGADA`), `RECHAZADO`
   → `rechazar` (libera la retención). Ver [[withdrawal-reconciliation]] para
   el detalle de ese job.
2. Si la urgencia lo justifica, se puede disparar la reconciliación fuera de
   su horario invocando el mismo *endpoint*/*bean* manualmente (según cómo
   quede expuesto en el hardening operativo del servicio) — nunca escribiendo
   `UPDATE` directo sobre `orden_retiro` o `retencion_saldo`.

## Validación

```bash
# El conteo de EN_PROCESO baja tras la siguiente corrida del job
docker exec aportaya-postgres psql -U pasanaku -d pasanaku -c \
  "SELECT count(*) FROM nucleo_financiero.orden_retiro WHERE estado = 'EN_PROCESO';"
```

Repetir cada pocos minutos — una baja sostenida, no un número único.

## Escalamiento

- Órdenes `EN_PROCESO` con más de unas horas de antigüedad, o un *circuit
  breaker* que no cierra tras varios ciclos: escalar al dueño de la
  integración con el proveedor — puede ser un problema del lado del
  proveedor que necesita contacto directo, no solo reintentos.
- Saldo retenido acumulado significativo (umbral de negocio, no técnico):
  escalar a quien gestione la relación comercial con el proveedor.

## Ver también

[[withdrawal-reconciliation]] · [[../Arquitectura/ADR-049 Doble aprobación de retiros]] · [[outbox-backlog]] · [[postgres-down]]
