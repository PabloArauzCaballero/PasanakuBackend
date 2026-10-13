---
name: facturacion-sin
description: "Cobrar comisiones y facturar en AportaYa: tarifario parametrizable, cotización congelada, devengo, cargo, impuestos, factura electrónica y nota de crédito. Úsala al tocar precios, al implementar cualquier cobro de comisión, al integrar el servicio de impuestos, o cuando haya que devolver un cargo ya facturado."
---

# Comisiones, impuestos y factura

## El principio del módulo

**La política de cobro es dato, no código.** Cambiar cuánto se cobra, a quién y
cuándo no puede requerir un despliegue. Toda la política vive en el tarifario, con
vigencia y con historial.

```
catalogo_hecho_generador   qué hechos pueden generar cobro
tarifario (versionado)     el conjunto de precios vigente
  └── concepto_tarifa      un cobro concreto: base, monto o porcentaje, mínimo, máximo
        └── regla_tarifa   cuándo aplica y a quién
politica_redondeo          cómo se redondea, una sola vez y documentado
segmento_comercial         a qué población aplica un tarifario
asignacion_tarifario       qué tarifario rige para quién
```

Un `if (esPremium) monto * 0.02` en el código es exactamente lo que este diseño
existe para evitar.

## Cotizar, congelar, devengar, cobrar

Cuatro momentos distintos, con cuatro tablas distintas. Confundirlos es el error
clásico:

| Momento | Tabla | Qué guarda |
| --- | --- | --- |
| **Cotizar** | `cotizacion_comision` | Qué se cobraría, con qué tarifario y qué reglas. Se le muestra al usuario **antes** de que acepte |
| **Congelar** | `tarifa_congelada_grupo` | El precio pactado al crear el grupo. Un cambio de tarifario **no** afecta grupos en curso |
| **Devengar** | `devengo_comision` | El ingreso ganado, cuando ocurre el hecho generador |
| **Cobrar** | `cargo_comision` | El movimiento efectivo contra el saldo |

Devengo y cobro pueden no coincidir en el tiempo: si se devengó y no se cobró,
queda `cuenta_por_cobrar_comision`. Esa distinción es la que hace que el estado de
resultados tenga sentido.

## Trazabilidad del precio

Todo cargo debe poder responder: **¿por qué me cobraron esto?** El cargo apunta a
su cotización, la cotización al tarifario y a las reglas que aplicaron, y el
tarifario a la decisión que lo aprobó (`cambio_tarifario`). Un cargo sin ese
rastro es un cargo indefendible ante un reclamo.

## Cambiar precios

1. Nueva versión de `tarifario` — **nunca** editar la vigente.
2. `cambio_tarifario` con quién lo aprobó, cuándo y desde cuándo rige.
3. Preaviso al usuario según lo que exija la norma y el contrato de adhesión.
4. Los grupos en curso conservan su tarifa congelada.
5. `simulacion_tarifa` antes de aplicar: se mide el impacto sobre datos reales, no
   se estima.

## Exenciones y promociones

`exencion_comision` (por regla, con vigencia y motivo) y `campana_promocional` +
`aplicacion_promocion` (por campaña, con tope). Ambas dejan rastro en el cargo: el
usuario ve que se le descontó y por qué. Un descuento sin registro es una
diferencia de caja el día del cierre.

## Impuestos

`impuesto` (alícuota con vigencia y base legal) → `calculo_impuesto` sobre cada
hecho gravado. La alícuota **no se escribe en el código**: cambia por norma.

El redondeo se aplica **una sola vez, al final**, según `politica_redondeo`, y el
resultado se guarda. Redondear en cada paso produce diferencias de centavos que se
acumulan y aparecen en el cierre; ver `dinero-decimal`.

## Factura electrónica

```
datos_facturacion → factura_electronica → lote_envio_sin → evento_significativo_sin
```

| Regla | Por qué |
| --- | --- |
| La factura se emite después del cobro efectivo | Facturar lo no cobrado descuadra ingresos |
| El envío al servicio de impuestos es **asíncrono**, por la cola | Su indisponibilidad no puede tumbar el cobro |
| Si el servicio no responde, se emite en contingencia y se registra el evento | `evento_significativo_sin` con su motivo y su regularización |
| El envío se reintenta con retroceso y termina en cola muerta visible | Un lote perdido en silencio es una contingencia no declarada |
| Los datos de facturación se validan antes de emitir | Corregir una factura emitida cuesta una nota de crédito |

## Devolver: nota de crédito, no borrado

Una comisión mal cobrada se devuelve con `devolucion_comision` **y**
`nota_credito_debito` que anula la factura. La factura original no se edita ni se
elimina: queda con su nota asociada. Es lo mismo que en el libro de dinero —se
corrige agregando, nunca editando.

Si la devolución nace de un reclamo favorable, es obligatoria antes de cerrarlo
(`R-CON-04`, skill `reclamos-consumidor`).

## Checklist

- [ ] Ningún precio, porcentaje, mínimo, máximo ni alícuota en el código.
- [ ] El usuario vio la cotización antes de aceptar.
- [ ] El grupo en curso conserva su tarifa congelada.
- [ ] Devengo y cobro están separados; lo devengado sin cobrar figura por cobrar.
- [ ] El cargo enlaza a su cotización, tarifario y reglas.
- [ ] El redondeo se aplica una vez y se persiste.
- [ ] La emisión al servicio fiscal es asíncrona y tiene camino de contingencia.
- [ ] La devolución genera nota de crédito; la factura original queda intacta.

## Ver también

`dinero-decimal` · `contabilidad-partida-doble` · `norma-nueva` ·
`semillas-catalogos` · `reclamos-consumidor` · `trabajos-outbox` ·
CU-30 a CU-35 · familia `R-TAR`
