---
name: reembolsos-disputas
description: "Devolver un cobro y responder un contracargo en AportaYa: motivos de reembolso, doble firma, reapertura de la obligación, nota de crédito, disputa de la pasarela con plazo guardado y armado del descargo con la evidencia que ya existe. Úsala cuando haya que devolver plata cobrada, cuando llegue un aviso de contracargo, o cuando un reclamo termine en 'hay que devolverle'."
---

# Reembolsos y disputas

Devolver es mover dinero al revés, y por eso tiene las mismas reglas que cobrarlo:
asiento, idempotencia, doble firma y evidencia. **No es "deshacer".**

## Reembolso ≠ reversa ≠ devolución de comisión

Los tres existen y se confunden todo el tiempo:

| Qué | Cuándo | Caso de uso |
| --- | --- | --- |
| **Reversa** | error **nuestro** en un movimiento interno de billetera | [[CU-14 Reversar una transacción]] |
| **Reembolso** | el dinero vuelve **al medio de pago externo** por el que entró | [[CU-19 Reembolsar un pago y atender una disputa]] |
| **Devolución de comisión** | se devuelve el cargo, con nota de crédito fiscal | [[CU-33 Devolver comisión y emitir nota de crédito]] |

Un reembolso de un aporte que ya tenía comisión facturada exige **los tres**: el
reembolso, la nota de crédito y la reapertura de la obligación. Hacer uno solo deja
el libro descuadrado o al cliente cobrado de más.

## Reglas duras

1. **No se reembolsa lo no conciliado.** Si el pago no cruzó con el extracto, no se
   sabe si entró. `PAGO_NO_CONCILIADO`.
2. **Nunca más de lo pagado**, contando reembolsos previos.
3. **Doble firma**: quien aprueba no es quien solicitó (`R-SEG-04`).
4. **Reembolsar no es condonar.** Si el pago saldaba una [[obligacion_aporte]], la
   obligación **vuelve a quedar pendiente** por ese importe, en la misma transacción.
5. **El saldo no queda negativo** (`R-BIL-02`). Si el beneficiario ya gastó el
   dinero, el reembolso crea [[deuda_participante]] y entra a cobranza.
6. **El dinero no vuelve hasta el acuse.** El ciclo es
   `SOLICITADO → APROBADO → ENVIADO → ACREDITADO`.

## Motivos, y por qué importan

| `motivo` | Origen típico | Consecuencia adicional |
| --- | --- | --- |
| `DUPLICADO` | el usuario pagó dos veces | ninguna: es nuestro error de UX o del proveedor |
| `MONTO_ERRONEO` | cobro por importe distinto | revisar la orden que lo generó |
| `NO_RECONOCIDO` | el titular desconoce la operación | evaluar fraude; puede abrir [[incidente_seguridad]] |
| `SERVICIO_NO_PRESTADO` | reclamo procedente | encadena [[CU-52 Atender un reclamo en plazo]] |

El motivo no es una etiqueta decorativa: alimenta el análisis de causa y la
recalibración antifraude.

## Disputa entrante (contracargo)

Cuando el emisor o la pasarela avisa un contracargo, el reloj empieza a correr y
suele ser corto.

1. **`fecha_limite_respuesta` se calcula y se guarda al recibirla** (`R-CON-01` por
   analogía). Si el proveedor avisó tarde, se deja constancia de la fecha de
   recepción real: el plazo se cuenta desde que lo supimos.
2. El descargo se arma con lo que **ya está guardado**: [[orden_cobro]],
   [[qr_cobro]], [[intento_pago]], acuses de [[evento_entrega_mensaje]], sesión y
   aparato desde el que se operó.
3. **Si no hay evidencia suficiente, eso es un hallazgo**: significa que el flujo de
   cobro no dejó rastro. Se registra como [[hallazgo_auditoria]], no como mala suerte.
4. Perdida la disputa: se debita, se registra el asiento de pérdida y se abre
   [[evento_riesgo_operativo]] con la pérdida cuantificada.

## Lo que nunca se hace

**No se le quita el dinero a quien ya cobró su turno** por una disputa perdida sobre
un aporte. La pérdida la absorbe la plataforma o el [[fondo_garantia]] según el
reglamento. Trasladarla al ganador del turno rompería la promesa central del
pasanaku, que es que cuando te toca, cobrás.

## Idempotencia

La clave del reembolso deriva de `reembolso.id`; la del webhook de disputa, del
identificador del proveedor. Reintento del proveedor = una sola disputa, un solo
débito (`R-BIL-06`).

## Qué no hacer

- No editar el pago original: el reembolso es un movimiento nuevo.
- No cerrar una disputa sin veredicto registrado.
- No responder un contracargo con evidencia armada a mano fuera del sistema.
- No aprobar el propio reembolso solicitado.
- No devolver antes de conciliar "para que el cliente no se enoje": si el pago no
  entró, devolverlo es regalar plata.

## Ver también

- [[CU-19 Reembolsar un pago y atender una disputa]] · [[CU-14 Reversar una transacción]] ·
  [[CU-33 Devolver comisión y emitir nota de crédito]] · [[CU-52 Atender un reclamo en plazo]]
- `R-BIL-02` · `R-BIL-06` · `R-BIL-12` · `R-SEG-04` · `R-TAR-11` en [[Restricciones]]
- Skills: `qr-pagos`, `desembolsos-payouts`, `contabilidad-partida-doble`,
  `facturacion-sin`, `reclamos-consumidor`
