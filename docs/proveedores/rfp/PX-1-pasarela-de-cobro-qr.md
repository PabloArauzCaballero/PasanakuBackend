---
titulo: "RFP PX-1 — Pasarela de cobro por QR (principal y segundo proveedor)"
estado: lista para enviar una vez completados los campos [ … ] (sección 10)
fecha: 2026-11-21
origen: "PLAN-PROVEEDORES.md §PX-1 y §PX-0; plantilla docs/proveedores/00-plantilla-rfp.md"
tags: [proveedores, rfp, pagos, qr]
---

# Solicitud de propuestas PX-1 — Pasarela de cobro por QR

Esta RFP se envía junto con el **Anexo A — Requisitos comunes** (`docs/proveedores/00-plantilla-rfp.md`,
secciones 5, 7, 8 y 9). Acá se indica cómo aplica cada requisito común a este servicio y se agregan los
propios. Se responde por código (ver «Forma de respuesta»).

## 1. Quiénes somos y qué buscamos

[Razón social de la empresa] opera **Pasanaku** ([marca]), una aplicación de pasanaku digital en Bolivia:
grupos de personas que se conocen ahorran juntas; cada una paga una cuota por periodo y, por turno, una de
ellas recibe el pozo. En nuestro producto **un pago solo existe cuando la pasarela lo confirma por webhook
firmado**; ninguna persona aprueba pagos a mano. La empresa no custodia el dinero del grupo en su cuenta
operativa: su único ingreso es una comisión de porcentaje único, igual para todos, que se cobra en el mismo
QR como línea aparte. No operamos efectivo. Hoy no movemos dinero real: la habilitación depende de una
autorización regulatoria en trámite y la contratación tiene que estar lista antes.

**Buscamos dos proveedores de este servicio.** Tener un **segundo proveedor contratado es obligatorio** antes
de la puesta en producción. Esta RFP elige a ambos; el oferente indica si se postula como principal, como
segundo o como cualquiera de los dos. Operamos con un enrutador que reparte el tráfico según la salud medida
de cada proveedor.

Usos del servicio:
- Cobro de cuotas de los grupos (con la comisión como segunda línea del mismo QR).
- Recarga de la billetera del usuario por QR; ese saldo luego paga cuotas.
- Reembolso de un pago, cuando corresponde.

Volumen estimado: [operaciones por mes], [monto promedio por operación en BOB], [pico diario].

## 2. Calendario y forma de respuesta

| Hito | Fecha |
|---|---|
| Publicación | [fecha] |
| Preguntas por escrito hasta | [fecha] |
| Cierre de propuestas | [fecha] |
| Demostración en entorno de pruebas (preseleccionados) | [rango] |
| Resultado | [fecha] |

Se responde **cada código** (`RF-PX1-xx`, `RM-xx`, `SEG-xx`, `DAT-xx`, `CON-xx`, casos de la sección 6) con:
**Cumple** (cómo, con referencia a la documentación) · **Cumple con desarrollo** (qué y en qué plazo) ·
**No cumple**. Sin respuesta cuenta como «No cumple». Propuestas a [canal de recepción].

## 3. Contexto técnico

- Integración por API HTTP documentada detrás de un puerto propio; no dependemos de un SDK.
- Toda operación lleva **nuestra clave de idempotencia**; los importes van en decimal exacto con moneda
  ISO 4217 (`BOB`); fechas en ISO 8601 con zona horaria (`America/La_Paz`).
- Nuestro flujo: obligación de pago → orden de cobro → QR → intento de pago → webhook firmado → pago → asiento
  contable. Respondemos al webhook en menos de 2 segundos y procesamos de forma asíncrona.

## 4. Requisitos funcionales del servicio

| Código | Requisito | Qué tiene que demostrar el proveedor |
|---|---|---|
| RF-PX1-01 | Generar un QR **de monto fijo atado a una orden**, con vigencia explícita y configurable por nosotros, uno por orden e intento. Regenerar crea un intento nuevo y no pisa el anterior | API de creación con monto, moneda, vigencia y nuestra referencia; cómo se distinguen dos intentos de la misma orden; qué pasa si se paga un QR ya reemplazado |
| RF-PX1-02 | El QR cobra **la cuota y la comisión como dos líneas** del mismo pago; necesitamos poder asentarlas por separado (pozo y comisión) | Cómo representa las líneas o el desglose en la creación, en el webhook y en el reporte de liquidación |
| RF-PX1-03 | Confirmar el pago por **webhook firmado** que informa **monto y moneda efectivamente pagados** y nuestra referencia. Si no coinciden con la orden, no acreditamos y abrimos una excepción de conciliación | Esquema del webhook, firma, campos de monto y moneda; reintentos hasta 2xx |
| RF-PX1-04 | Por encima de un umbral que fijamos nosotros, **reconsultamos el estado por API** antes de acreditar, aunque el webhook sea válido | API de consulta de estado por nuestra clave, con su latencia comprometida |
| RF-PX1-05 | **Recarga de billetera** por QR con el mismo flujo que la cuota | Que el mismo producto sirve para ambos usos con referencias distintas |
| RF-PX1-06 | **Reembolso** de un pago (en nuestro lado lo autorizan dos personas distintas). Si la pasarela no lo soporta, lo hacemos por transferencia saliente | API de reembolso total o parcial, idempotente por nuestra clave, con webhook de resultado; o declaración de que no lo soporta |
| RF-PX1-07 | **Pago tardío**: un pago confirmado después del vencimiento de la cuota se acepta y nosotros lo aplicamos a la deuda correspondiente | Que el pago de un QR vigente se informa igual aunque nuestra cuota haya vencido; que nunca se rechaza en silencio |
| RF-PX1-08 | **Enlace de pago rápido** enviado por push, SMS o correo con un token nuestro de un solo uso y vencimiento, que solo permite pagar esa obligación | Generación del QR bajo demanda por API (o enlace de pago del proveedor) asociado a nuestra orden; sin datos personales en la URL |
| RF-PX1-09 | Los **motivos de rechazo** se documentan en un catálogo cerrado y estable, que mapeamos al nuestro | Catálogo de códigos de rechazo y su política de cambios |
| RF-PX1-10 | **QR interoperable**: se tiene que poder pagar desde la app de cualquier banco del país, según el estándar de QR interoperable que rija en Bolivia (lo confirma nuestro Cumplimiento) | Estándar que implementa, bancos y billeteras desde los que se puede pagar, y autorización con la que opera |

Requisitos no funcionales propios:
- El proveedor reintenta el webhook hasta recibir 2xx; nosotros respondemos en menos de 2 s.
- El contenido (payload) del QR no viaja en URLs ni en logs del proveedor más allá de lo necesario; nosotros
  guardamos solo su referencia y su hash.
- Firma HMAC o asimétrica, verificable en tiempo constante, con marca de tiempo para rechazar repeticiones.

## 5. Requisitos comunes (Anexo A §5) aplicados a este servicio

| Código | Aplica | Precisión para PX-1 |
|---|---|---|
| RM-01 | Sí | API HTTP documentada; SDK opcional |
| RM-02 | Sí | Declarar consulta de estado, idempotencia, reembolso, montos mínimo y máximo, moneda `BOB` y horarios |
| RM-03 | Sí — **eliminatorio** | Nuestra clave de idempotencia en la creación del QR y en el reembolso; viaja en el webhook |
| RM-04 | Sí — **eliminatorio** | Webhook firmado con marca de tiempo e id único de evento |
| RM-05 | Sí | Si llegan «pagado» y «fallido» de la misma operación, manda el último acuse; documentar la secuencia |
| RM-06 | Sí — **eliminatorio** | Consulta de estado por nuestra clave para resolver operaciones sin webhook |
| RM-07 | Sí | Un timeout nuestro al crear el QR no puede generar dos QR cobrables por la misma orden |
| RM-08 | Sí | Latencias de creación del QR y de consulta; límites de tasa |
| RM-09 | Sí | Página de estado y aviso proactivo de incidentes (con ellos suspendemos la mora mientras dura el incidente) |
| RM-10 | Sí | Arranque con porción baja de tráfico |
| RM-11 | Sí | Cláusulas de confidencialidad, auditoría y continuidad; casos de la sección 6 aprobados |
| RM-12 | Sí | Rotación de la llave de firma de webhooks con dos llaves válidas |
| RM-13 | Sí | No pedimos datos del pagador para cobrar; si el proveedor los recibe del banco, los declara |
| RM-14 | Sí | Costo por transacción, por reembolso y por QR no pagado |
| RM-15 | Sí | Consulta de operaciones históricas después de la baja |
| RM-16 | Sí | Id de correlación en respuestas y soporte |
| RM-17 | Sí | Sandbox que permita provocar todos los casos de la sección 6 |
| RM-18 | Sí | Versionado y política de deprecación |
| RM-19 | Sí — **segundo proveedor obligatorio** | RTO/RPO y última prueba; aceptar convivir con otro proveedor |
| RM-20 | Sí | Vigencia del QR y umbrales configurables por nosotros |

## 6. Casos obligatorios de prueba (en el entorno de pruebas del proveedor)

| Nivel | Caso | Resultado esperado |
|---|---|---|
| Correcto | Webhook firmado con monto igual al de la orden | Un pago y dos asientos (pozo y comisión) |
| Límite | El mismo webhook entregado dos veces | Un solo efecto |
| Límite | Evento en el borde de la ventana temporal | Aceptado |
| Límite | Pago justo al vencer el QR | Según la política de vigencia declarada por el proveedor, sin ambigüedad |
| Límite | Pago después del vencimiento de la cuota | Informado igual; nosotros lo aplicamos a la deuda |
| Inválido | Firma inválida | Rechazado (401) sin efecto |
| Inválido | Monto distinto al de la orden | No se acredita; excepción de conciliación |
| Inválido | Orden inexistente o vencida | Rechazado |
| Inválido | Evento viejo repetido (replay) | Rechazado |
| Inválido | La pasarela no responde | La operación se resuelve por consulta de estado sin duplicar |

## 7. Seguridad

Aplican SEG-01 a SEG-07 del Anexo A. Para este servicio además:
- PCI DSS solo si el oferente ofrece cobro con tarjeta; **no** se pide cobro con tarjeta en esta RFP.
- La llave de firma de webhooks es por comercio (por nosotros), no compartida entre clientes del proveedor.

## 8. Datos personales y financieros

Aplican DAT-01 a DAT-06 del Anexo A. Para este servicio además: declarar qué datos del pagador recibe del banco
emisor (nombre, cuenta) y si nos los transmite; preferimos **no** recibirlos.

## 9. Continuidad, disponibilidad, costo y salida

Aplican CON-01 a CON-07 del Anexo A. Para este servicio además:
- **Degradación declarada de nuestro lado:** si la pasarela cae, la app avisa que el cobro por QR no está
  disponible, la mora no corre mientras dure el incidente declarado y el tráfico conmuta al segundo proveedor.
  Necesitamos del proveedor el aviso del incidente (inicio y fin) para registrarlo.
- Liquidación: plazo y forma en que el dinero cobrado llega a nuestras cuentas de custodia (nunca a la cuenta
  operativa de la empresa, salvo la línea de comisión).

## 10. Campos a completar antes de enviar (`DECISION_REQUIRED` de Pablo)

Razón social, marca y canal de recepción · fechas del calendario · volumen estimado · los plazos abiertos del
Anexo A (RM-15, RM-18, SEG-05, DAT-06).
