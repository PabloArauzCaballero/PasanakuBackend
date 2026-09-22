# Runbook — Reconciliación de retiros (`ReconciliacionDeRetiros`)

> Carril PR2 (`nucleo-financiero`), H4.S2.M3. El job que resuelve lo que
> [[provider-timeout|un timeout del proveedor]] deja a medias.

## Qué hace este job

`bo.aportaya.nucleofinanciero.trabajos.ReconciliacionDeRetiros`, disparado por
`@Scheduled(cron = "${aportaya.retiro.reconciliacion.cron:0 */5 * * * *}")`
(cada 5 minutos por defecto, zona `America/La_Paz`):

1. Lee todas las órdenes en estado `EN_PROCESO` (`CU11RetirarSaldo.ordenesEnProceso`,
   una transacción de solo lectura).
2. Para cada una, le pregunta al proveedor por su `referencia_proveedor`
   (`ProveedorDeRetiro.consultar`) — la misma llamada que un *webhook* del
   proveedor hubiera disparado si hubiera llegado a tiempo.
3. Según la respuesta, aplica **exactamente** el mismo camino que cualquier
   otra confirmación o rechazo:
   - `ACEPTADO` → `CU11RetirarSaldo.confirmarPago` (pasa a `PAGADA`, registra
     el movimiento contable).
   - `RECHAZADO` → `CU11RetirarSaldo.rechazar` (pasa a `RECHAZADA`, libera la
     retención, el saldo vuelve a disponible).
   - `TIMEOUT` de nuevo → no toca la orden. Sigue `EN_PROCESO`; la próxima
     corrida vuelve a preguntar.
4. Cada orden se resuelve en **su propia** transacción — una que falle (por
   ejemplo, porque un *webhook* real ya la resolvió entre el paso 1 y este
   punto) no tumba la reconciliación de las demás; queda registrada en el
   log y sigue con la siguiente.

El actor de estas transacciones es `ContextoSesion.deSistema(...)` (rol
`sistema`, sin usuario humano): el movimiento contable que genera
`confirmarPago` en este camino queda con `iniciada_por IS NULL` y
`canal = 'BATCH'` — no es un descuido, es la forma correcta de registrar que
lo hizo el sistema y no una persona (mismo patrón que
`CU24RegistrarAsiento.iniciadaPor`).

## Síntomas de que algo anda mal

- El conteo de órdenes `EN_PROCESO` no baja entre corridas, aunque el
  proveedor esté respondiendo con normalidad para pedidos nuevos.
- Logs con `CU-11 reconciliacion · <n> orden(es) resuelta(s), <n> siguen
  EN_PROCESO` mostrando el segundo número creciendo corrida tras corrida.
- Logs de `WARN` con `orden ... esta EN_PROCESO sin referencia_proveedor`
  (no debería pasar nunca — `pasarAEnProceso` siempre guarda la referencia;
  si aparece, es una orden corrupta que necesita revisión manual, no
  automática).

## Dashboards y consultas

```bash
# Las EN_PROCESO mas viejas: si el job corre cada 5 minutos y una tiene mas
# de un par de corridas de antiguedad, algo la esta trabando especificamente
# a ELLA (no es un problema general del proveedor)
docker exec aportaya-postgres psql -U pasanaku -d pasanaku -c \
  "SELECT id, referencia_proveedor, solicitada_en, now() - solicitada_en AS antiguedad
     FROM nucleo_financiero.orden_retiro
    WHERE estado = 'EN_PROCESO'
    ORDER BY solicitada_en ASC;"
```

## Diagnóstico

1. **¿El job está corriendo?** `@EnableScheduling` está en
   `Aplicacion.java`; si el proceso está caído o el *scheduler* interno de
   Spring está bloqueado (otro `@Scheduled` de larga duración monopolizando
   el único hilo del *pool* por defecto), esta corrida simplemente no pasa.
   Confirmar con los logs de arranque y la ausencia total de la línea
   `CU-11 reconciliacion · ...` en la ventana esperada.
2. **¿Es una orden puntual atascada, o todas?** Si es una sola orden con
   antigüedad fuera de lo normal mientras las demás se resuelven, sospechar
   de esa referencia específica del lado del proveedor (puede necesitar
   contacto directo) antes que del job.
3. **¿El proveedor sigue devolviendo `TIMEOUT` para TODO?** Eso es
   [[provider-timeout|un incidente del proveedor]], no del job — el job está
   haciendo su trabajo (preguntar), la respuesta simplemente sigue siendo
   "no sé".
4. **¿Hay ordenes con `referencia_proveedor IS NULL`?** Señal de un bug en
   `instruirPago`/`pasarAEnProceso`, no del job de reconciliación — escalar
   como defecto de código, no como incidente operativo.

## Acciones seguras

- **Nunca** actualizar `orden_retiro.estado` a mano para "destrabar" una
  reconciliación pendiente — el punto entero del job es que la fuente de la
  verdad es la respuesta del proveedor, no una suposición operativa.
- Si el proceso del servicio está caído, reiniciarlo: la próxima corrida
  programada retoma sin estado que reconstruir (el job es *idempotente* por
  diseño — corre sobre lo que encuentra `EN_PROCESO` en cada ejecución, no
  sobre una cola propia).
- Si se necesita reconciliar fuera de horario por urgencia operativa, es
  preferible **esperar a la próxima corrida** (cada 5 minutos) antes que
  intervenir manualmente — la ventana es corta.

## Recuperación

- El conteo de `EN_PROCESO` baja solo, corrida tras corrida, a medida que el
  proveedor contesta.
- Una orden que quedó atascada por una referencia mal formada u otro defecto
  de datos necesita revisión manual **puntual** (no del job en general): un
  operador con `RETIRO_APROBAR` o superior investiga esa orden específica
  contra el proveedor, y si corresponde, se resuelve con las herramientas
  normales del caso de uso (`confirmarPago`/`rechazar`), nunca con `UPDATE`
  directo.

## Validación

```bash
docker exec aportaya-postgres psql -U pasanaku -d pasanaku -c \
  "SELECT count(*) FROM nucleo_financiero.orden_retiro WHERE estado = 'EN_PROCESO';"
```

## Escalamiento

- Job que no corre (sin logs de ninguna corrida en más del intervalo
  esperado): escalar como incidente de plataforma — puede ser el *scheduler*
  de Spring bloqueado por otro trabajo.
- Órdenes puntuales atascadas por varias corridas mientras el resto del
  tráfico se resuelve con normalidad: escalar al dueño de la integración con
  el proveedor.

## Ver también

[[provider-timeout]] · [[ADR-049 Doble aprobación de retiros]] · [[outbox-backlog]]
