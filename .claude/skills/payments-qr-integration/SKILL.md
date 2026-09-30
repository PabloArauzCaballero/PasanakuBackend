---
name: payments-qr-integration
description: Integración con pasarelas y cobro por QR en Pasanaku — orden de cobro como raíz del flujo, generación y vigencia del QR, enlaces de pago rápido con token de un solo uso, webhooks firmados e idempotentes como única fuente de acreditación, reconsulta de estado ante silencio del proveedor, estados del pago y del intento, comprobante manual como evidencia y no como acreditación, reembolsos, y cómo se prueba una pasarela sin depender de su sandbox. Usar al integrar o tocar un proveedor de pagos, al emitir un QR o un enlace, al escribir o revisar el webhook, o al diagnosticar un pago que el usuario dice haber hecho y el sistema no ve.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Cobro por QR e integración con pasarelas

El webhook de la pasarela es **el endpoint que acredita dinero**: el más atacado y el que más
duele si se equivoca. Todo lo de esta skill existe para que un reintento, un reenvío malicioso o
una caída no acrediten de más ni de menos.

Servicio dueño: `pagos` (módulo M3). Reglas duras: **91** (dinero) y **90.2** (datos).

## 1. La cadena de entidades

```
obligacion_aporte  →  orden_cobro  →  qr_cobro / enlace_pago_rapido
                            │                    │
                            └──▶ intento_pago ◀──┘
                                      │
                                      ▼ (webhook firmado, confirmado)
                                    pago  →  asiento contable  →  eventos
```

- La **obligación** es la deuda del cupo en ese periodo. Existe aunque nadie intente pagar.
- La **orden de cobro** es la intención de cobrar ese monto, con su `clave_idempotencia`.
- El **QR** o el **enlace** son instrumentos de una orden, con vigencia propia.
- El **intento** registra cada vez que alguien empieza a pagar. Muchos intentos, un pago.
- El **pago** solo nace con confirmación verificada del proveedor.

**Prohibido saltarse eslabones**: un pago sin orden, o una acreditación sin pago, rompe la
conciliación y el cuadre (`distributed-data-integrity`).

## 2. El QR

| Atributo | Regla |
|---|---|
| Monto | Fijo y **atado a la orden**. Un QR de monto abierto abre disputas: si se usa, el flujo de conciliación tiene que resolver el sobrante o faltante explícitamente |
| Vigencia | Corta y explícita; vencido no se acredita, se reemite |
| Unicidad | Un QR por orden e intento; no se reusa entre periodos |
| Identificación | Lleva la referencia que permite conciliar (no el nombre ni el documento) |
| Regeneración | Genera un `intento_pago` nuevo, no pisa el anterior |

**No se persiste ni se loguea el payload del QR** (regla 90.2.1). Se guarda su referencia y su
hash si hace falta verificar.

## 3. Enlace de pago rápido

`enlace_pago_rapido` es para quien recibe la notificación por WhatsApp y paga sin entrar a la
app (`notifications-delivery`).

- **Token de propósito único, un solo uso y con vencimiento** (`token_enlace_firmado`, M1).
- El enlace **no autentica al usuario**: da acceso a pagar una obligación concreta, nada más.
  Desde él no se ve el grupo, ni otros cupos, ni datos de terceros (regla 90.1.11).
- Se registra en `enlace_pago_notificado` a quién se le mandó y cuándo.
- Un enlace usado o vencido muestra un mensaje claro y ofrece pedir uno nuevo, **nunca** un error
  genérico que parezca que el pago falló.

## 4. El webhook: única fuente de acreditación

```ts
// El orden importa. Cada paso corta antes de tocar plata.
1. Verificar firma con comparación de tiempo constante  → si no, 401 y nada más
2. Verificar ventana temporal del timestamp firmado     → corta replays
3. Registrar el webhook crudo en `webhook_pasarela`     → append-only, sin loguear el cuerpo
4. Deduplicar por el id del evento del proveedor        → UNIQUE en base
5. Encolar / outbox y responder 2xx rápido              → el proveedor no espera tu contabilidad
6. Procesar asíncrono: validar monto y moneda contra la orden, crear pago, asentar
```

Reglas no negociables:

1. **Firma verificada siempre, también en desarrollo.** Un webhook sin firma es un endpoint que
   acredita a cualquiera que sepa la URL (regla 91.3.2).
2. **Idempotente por el id del proveedor.** El proveedor reintenta; dos entregas del mismo evento
   acreditan una vez (regla 91.1.4).
3. **Validar monto y moneda contra la orden.** Si no coinciden: no se acredita, se abre
   `excepcion_conciliacion`. Nunca se acredita "lo que vino".
4. **Responder rápido** (< 2 s) y procesar después. Un webhook lento multiplica reintentos.
5. **Nunca confiar en el cuerpo para montos grandes**: reconsultar el estado en la API del
   proveedor antes de acreditar por encima del umbral definido.
6. El cuerpo **no se loguea entero** (lleva datos financieros).

## 5. Silencio del proveedor: reconsulta

El webhook puede no llegar nunca. Un sistema que solo reacciona a webhooks **pierde pagos**.

- Job de reconsulta que busca `intento_pago` en estado pendiente más viejos que N minutos y
  consulta el estado al proveedor (`background-jobs-scheduling`).
- Idempotente: si mientras tanto llegó el webhook, no duplica.
- Con backoff y tope; agotado, pasa a revisión manual con alerta.
- **Es el mecanismo que cierra la brecha entre "el usuario pagó" y "el sistema se enteró".** Sin
  él, el soporte vive resolviendo a mano.

## 6. Estados

```
intento_pago:  INICIADO → EN_PROCESO → { CONFIRMADO | RECHAZADO | VENCIDO | ABANDONADO }
pago:          ACREDITADO → { REVERSADO | EN_DISPUTA }
```

- Las transiciones se validan en el servidor (`state-machines-workflows`); no hay endpoint que
  setee el estado.
- `RECHAZADO` guarda el motivo del proveedor mapeado a **nuestro catálogo cerrado**
  (`terminology-value-sets`), no el texto libre del proveedor.
- Un pago `ACREDITADO` **no se borra jamás**: se reversa (`money-movement-safety` §4).

## 7. Comprobante manual: evidencia, no acreditación

`comprobante_manual` es una foto que sube el participante. Vale como **evidencia para un humano**
y para la conciliación; **no acredita por sí sola** (regla 91.3.1).

- Se acredita cuando el movimiento aparece en el extracto y se concilia
  (`payment-reconciliation`), o cuando un operador lo aprueba con doble control.
- La aprobación manual queda auditada con actor, motivo y evidencia.
- El archivo se guarda con las reglas de `file-uploads-media` y sus datos con las de
  `data-privacy-financial`.

## 8. Cómo se prueba sin depender del sandbox

El sandbox del proveedor es lento, inestable y no se puede romper a voluntad. Probá contra un
**stub que respeta el contrato** (`microservices-testing`):

| Caso obligatorio | Qué se verifica |
|---|---|
| Webhook con firma inválida | 401 y ningún efecto |
| Webhook duplicado | Un solo pago, un solo asiento |
| Webhook con monto distinto al de la orden | No acredita; abre excepción |
| Webhook viejo (replay) | Rechazado por ventana temporal |
| Webhook de una orden vencida | Rechazado o a excepción, según política declarada |
| Proveedor que no responde | Reconsulta lo resuelve, sin duplicar |
| Confirmación después de la reconsulta | Efecto único |

El sandbox se usa para una prueba de humo de conectividad, no como red de seguridad.

## Anti-patrones

- Acreditar con el `return_url` o con lo que dice el cliente.
- Webhook sin verificación de firma "en dev".
- Procesar la contabilidad dentro del handler del webhook.
- Guardar el payload completo del QR o del webhook en logs.
- Acreditar el monto que vino en vez de validarlo contra la orden.
- Reintentar un cobro sin clave de idempotencia.
- Confiar solo en webhooks, sin reconsulta.
- Texto libre del proveedor guardado como estado.
- Aprobar un comprobante manual sin doble control ni auditoría.
- Reemitir un QR pisando el intento anterior.

## Checklist

- [ ] Cadena completa: obligación → orden → instrumento → intento → pago → asiento.
- [ ] QR con monto atado a la orden y vigencia explícita.
- [ ] Enlaces con token de un solo uso, vencimiento y alcance mínimo.
- [ ] Webhook: firma, ventana temporal, registro crudo, deduplicación, 2xx rápido, async.
- [ ] Monto y moneda validados contra la orden; discrepancia ⇒ excepción, no acreditación.
- [ ] Reconsulta periódica de intentos pendientes, idempotente.
- [ ] Estados con transiciones validadas y motivos de rechazo en catálogo cerrado.
- [ ] Comprobante manual tratado como evidencia, con doble control para aprobar.
- [ ] Los siete casos de prueba de §8, ejecutados.
- [ ] Ningún dato financiero en logs.

## Evidencia / DoD

1. Salida del webhook con firma inválida: 401, sin efecto en base.
2. Salida del webhook duplicado: un pago y un asiento, con la consulta pegada.
3. Salida del webhook con monto distinto: excepción de conciliación abierta.
4. Salida de la reconsulta resolviendo un pago sin webhook, sin duplicar.
5. Asientos del pago acreditado, cuadrados.
