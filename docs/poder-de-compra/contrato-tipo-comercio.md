---
titulo: "Contrato tipo con comercios aliados — «Poder de compra» (borrador para el abogado)"
estado: borrador — lo revisa el abogado; las cláusulas marcadas [L-11] dependen de su opinión
fecha: 2026-11-21
origen: "PX10.M02 de PLAN-PROVEEDORES.md; DECISIONES.md tandas 5 y 6; ADR-057 (propuesta)"
tags: [poder-de-compra, comercios, contrato, legal]
---

# Contrato tipo con comercios aliados — borrador

> **Para quien revisa.** Esto es un **borrador de estructura y contenido** escrito desde el producto, no un
> texto jurídico. No es asesoramiento legal. Lo redacta y ajusta el abogado de Pablo.
>
> - Las cláusulas marcadas **`[L-11]`** dependen de la opinión legal sobre cómo clasifica ASFI esta modalidad
>   y si se puede dejar el bien como garantía hasta terminar de pagar (`PREGUNTAS-LEGALES.md` L-11). **No se
>   firma ningún contrato con esas cláusulas antes de tener la opinión.**
> - Las marcadas **`[DR]`** son decisiones de negocio abiertas (`DECISION_REQUIRED` de Pablo).
> - No se cita ninguna norma que no esté en el repo. Donde el contrato necesita una referencia normativa, dice
>   «[norma a identificar por el abogado]».
> - Entre corchetes `[…]`, los datos a completar.

## Partes

- **[Razón social de la empresa]**, operadora de la aplicación Pasanaku («la Plataforma»).
- **[Razón social del comercio]**, NIT [número], representada por [representante legal] («el Comercio»).

## Cláusula 1 — Objeto

1.1 El Comercio ofrece, a través de la Plataforma, productos de su catálogo con un descuento sobre su precio de
lista, a grupos de ahorro cuyos integrantes se conocen y que eligen ese producto como meta de su grupo.

1.2 **`[L-11]`** La naturaleza jurídica de la relación (venta con pago diferido por el comprador, venta al contado
en la fecha de canje, u otra figura) y el papel de la Plataforma (intermediaria tecnológica que no vende ni
compra el producto) se redactan según la opinión legal. Este borrador asume, para poder describir el flujo, que
**el comprador es el participante**, la venta se perfecciona **en el canje** y la Plataforma no es parte de la
compraventa.

1.3 La Plataforma **no financia** la compra, **no cobra interés** y **no pone dinero** por ningún participante.

## Cláusula 2 — Alta y verificación del Comercio

2.1 Antes de publicar ofertas, el Comercio pasa la verificación automática de la Plataforma: datos de la empresa
(razón social, NIT), identidad de su representante legal, titularidad de la cuenta bancaria donde cobrará y
control contra las listas que defina el área de Cumplimiento.

2.2 El Comercio informa por escrito, dentro de [plazo], cualquier cambio de representante, de cuenta bancaria o
de situación societaria. **Un cambio de cuenta bancaria inicia un período de enfriamiento** de [plazo, parámetro]
durante el cual no se le paga a la cuenta nueva.

2.3 La Plataforma puede suspender al Comercio si deja de cumplir 2.1; mientras esté suspendido no puede validar
canjes nuevos. Los canjes ya validados se pagan.

## Cláusula 3 — Catálogo, descuento y precio congelado

3.1 El Comercio publica productos con: descripción, precio de lista, descuento, precio con descuento, moneda
(`BOB`), plazo de entrega desde el canje, garantía del producto y política de devoluciones.

3.2 **Precio congelado.** El precio con descuento vigente el día en que se crea un grupo con ese producto como
meta queda **fijo para todo el ciclo de ese grupo**, aunque el Comercio cambie después su precio de lista o su
oferta. El riesgo de variación de precio, de costo o de tipo de cambio es del Comercio.

3.3 **`[DR]`** Si el día del canje el precio con descuento vigente es **menor** que el congelado, el participante
paga el menor y la diferencia queda para él. (Propuesta; decide Pablo.)

3.4 La Plataforma controla el precio de lista contra precios de mercado para que el descuento no esté inflado. Si
detecta un precio de lista inflado, puede retirar la oferta para grupos nuevos sin afectar a los ya creados.

3.5 El Comercio garantiza el stock del producto para los grupos creados con su oferta, o un producto equivalente
**igual o superior**, aceptado por el participante. Si no puede entregar, se aplica la cláusula 6.

## Cláusula 4 — Canje en la tienda

4.1 Cuando un participante recibe su turno y elige el producto, la Plataforma le muestra un **QR de canje** de un
solo uso, con vencimiento, atado a esa compra.

4.2 El Comercio escanea el QR **en el momento de entregar el producto**, con su aplicación o su panel. El escaneo
válido es la constancia de entrega. El Comercio no escanea antes de entregar.

4.3 **`[L-11]` `[DR]`** El plazo máximo entre el turno y el canje, y qué pasa si vence (este borrador asume: el
dinero pasa a la cuenta del participante y la orden de compra queda sin efecto). Depende de cuánto tiempo puede
quedar retenido el dinero del participante según la opinión legal.

4.4 **Opción de recibir el dinero.** Mientras el canje no se haya validado, el participante puede elegir en
cualquier momento recibir su pozo en su cuenta bancaria en lugar del producto. El Comercio acepta que esa
opción existe siempre y que no genera ninguna obligación del participante ni de la Plataforma hacia él.

## Cláusula 5 — Pago al Comercio

5.1 La Plataforma ordena el pago al Comercio **solo después de un escaneo válido** (4.2), a la cuenta bancaria
verificada del Comercio, por el precio aplicable (3.2 y 3.3). Si el Comercio no entrega, no cobra.

5.2 El dinero del pago sale de la cuenta donde está custodiado el pozo del participante; **nunca** de una cuenta
de la Plataforma ni pasa por ella.

5.3 Cada pago lleva un identificador único; un mismo canje nunca se paga dos veces. El Comercio recibe el
comprobante de la transferencia.

5.4 Plazo del pago tras el escaneo: [plazo]. Si la transferencia es rechazada por el banco del Comercio por datos
incorrectos, la Plataforma reintenta después de que el Comercio los corrija.

## Cláusula 6 — Incumplimiento de entrega, garantía y devoluciones

6.1 Si el Comercio no entrega en el plazo de 3.1 tras el canje, o no tiene el producto ni un equivalente aceptado,
el participante puede pedir la anulación. **`[L-11]`** Cómo se restituye el dinero si ya se pagó al Comercio
(devolución del Comercio a la cuenta del participante, en [plazo]) y si la Plataforma puede retener pagos
futuros al Comercio para compensarlo.

6.2 La garantía del producto y las devoluciones son **a cargo del Comercio**, según su política publicada y
[norma de protección al consumidor a identificar por el abogado].

6.3 Reclamos: el participante reclama al Comercio por la Plataforma, que media con un plazo de respuesta de
[plazo]. **`[L-11]`** La Plataforma no retiene fondos del participante ni del Comercio más allá de lo que permita
la opinión legal.

## Cláusula 7 — Garantía sobre el bien

7.1 **`[L-11]`** Si, para turnos tempranos, el producto puede quedar como garantía hasta que el participante termine
de pagar sus cuotas (por ejemplo, reserva de propiedad en vehículos), quién es el titular de esa garantía, cómo se
constituye, cómo se libera al terminar de pagar y cómo se ejecuta, **solo si** la opinión legal lo permite. Si no
lo permite, esta cláusula se elimina.

7.2 **`[L-11]` `[DR]`** Turno temprano para personas sin historial pagando cuotas por adelantado (F5.M4): si se
habilita, qué obligación tiene el Comercio (por ejemplo, entregar antes de que el grupo complete el pozo) y sin
que nadie cobre interés.

## Cláusula 8 — Comisión de la Plataforma

8.1 El Comercio paga a la Plataforma una comisión de [porcentaje, parámetro] sobre cada venta canjeada. **El
participante nunca paga esa comisión** ni se le traslada en el precio congelado.

8.2 **`[DR]`** Forma de cobro: descontada del pago al Comercio (neteo) o facturada aparte con plazo de pago.

8.3 La Plataforma factura la comisión por un proveedor de facturación electrónica autorizado.

## Cláusula 9 — Datos personales

9.1 El Comercio recibe del participante **solo** lo necesario para entregar el producto y emitir su comprobante
(nombre y lo que exija la factura del producto). No recibe montos de sus cuotas, su nivel de puntualidad ni datos
de los otros integrantes del grupo.

9.2 Los datos agregados de audiencia que vea el Comercio en su panel se muestran solo por encima de un mínimo de
personas por segmento, para que no permitan identificar a nadie.

9.3 El Comercio no usa los datos para fines distintos de la entrega y la garantía, no los cede y los borra al
vencer [plazo de retención]. Notifica cualquier brecha en [plazo].

## Cláusula 10 — Publicidad y marca

10.1 El Comercio autoriza el uso de su marca en la Plataforma para las ofertas vigentes. Ninguna parte presenta a
la otra como garante de sus obligaciones.

## Cláusula 11 — Vigencia, terminación y salida

11.1 Vigencia de [plazo], renovable.

11.2 La terminación **no afecta** a los grupos ya creados con una oferta del Comercio: el precio congelado y la
obligación de entregar se mantienen hasta el último canje de esos grupos (o la opción de recibir el dinero).

11.3 Al terminar, el Comercio entrega los comprobantes pendientes y la Plataforma le paga los canjes ya validados.

## Cláusula 12 — Auditoría, confidencialidad y continuidad

12.1 La Plataforma puede auditar los canjes, entregas y reclamos vinculados a la Plataforma.

12.2 Confidencialidad recíproca sobre la información comercial y técnica.

12.3 El Comercio informa interrupciones que le impidan entregar y su plazo estimado.

## Cláusula 13 — Ley aplicable y controversias

13.1 [Ley aplicable y jurisdicción a definir por el abogado.]

---

## Índice de cláusulas que dependen de L-11

| Cláusula | Qué depende de la opinión legal |
|---|---|
| 1.2 | Naturaleza jurídica de la relación y papel de la Plataforma |
| 4.3 | Plazo máximo de retención del dinero entre turno y canje |
| 6.1 | Restitución cuando el Comercio no entrega y compensación con pagos futuros |
| 6.3 | Retención de fondos durante un reclamo |
| 7.1 | Garantía sobre el bien (reserva de propiedad) |
| 7.2 | Turno temprano pagando cuotas por adelantado |

## Decisiones de negocio abiertas `[DR]`

3.3 diferencia de precio a favor · 4.3 plazo de canje y su vencimiento · 7.2 turno temprano · 8.2 neteo o
factura aparte · porcentaje de comisión (8.1, parámetro sin valor por defecto).
