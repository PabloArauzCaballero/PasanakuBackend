---
name: qr-pagos
description: "Cobrar con QR y conciliar en AportaYa: orden de cobro, QR con vencimiento, intentos, webhook de la pasarela, extracto bancario, conciliación y excepciones. Úsala al implementar cobro, recarga, retiro o devolución con proveedor externo, al integrar una pasarela nueva, o cuando un pago aparezca en el banco y no en el sistema."
---

# Cobrar con QR y conciliar

El dinero real se mueve **afuera**, en la pasarela y en el banco. Adentro tenemos
una promesa que hay que confirmar contra el extracto. Ese desfase es el origen de
casi todos los incidentes de una billetera, y por eso la conciliación no es un
extra: es parte del flujo.

## El circuito

```
orden_cobro       lo que se espera cobrar: monto, moneda, vencimiento, a quién
  └── qr_cobro    el QR emitido, con su vencimiento propio y su referencia
        └── intento_pago      cada intento del usuario, exitoso o no
              └── pago        el cobro confirmado
                    └── conciliacion  contra movimiento_bancario del extracto
```

Más `webhook_pasarela` (el crudo, siempre), `proveedor_pago` (quién) y
`excepcion_conciliacion` (lo que no cuadró).

## Reglas del QR

| Regla | Por qué |
| --- | --- |
| **El QR vence, y el vencimiento se guarda** | Un QR eterno es un cobro que aparece meses después sin contexto |
| **Monto y moneda van en el QR** | Un QR de monto libre convierte cada cobro en una conciliación manual |
| **Un QR, una obligación** | Reutilizarlo hace imposible saber qué pagó el usuario |
| **La referencia es única por proveedor** | `UNIQUE (proveedor_id, referencia)`: es la clave de conciliación |
| **El QR expirado no se reactiva** | Se emite uno nuevo enlazado al mismo `orden_cobro` |

## El webhook

1. **Verificar la firma antes de procesar.** Sin firma válida: se guarda en
   `webhook_pasarela` y se descarta. Nunca se ejecuta.
2. **Guardar el cuerpo crudo siempre**, incluso si no se reconoce la referencia.
   Es la evidencia de qué dijo el proveedor y cuándo.
3. **Responder rápido.** El procesamiento pesado va a la cola; el webhook solo
   registra y encola.
4. **Duplicado ⇒ `200` sin efecto nuevo.** Responder error hace que el proveedor
   reenvíe indefinidamente.
5. **Fuera de orden ⇒ se compara contra el estado actual.** Una confirmación
   tardía no revive un pago reversado.

Ver `idempotencia-reintentos` para el detalle de las cuatro fallas.

## Acreditar

El pago confirmado dispara el movimiento de dinero **en la misma transacción**:
`pago` + `transaccion_billetera` + `movimiento_billetera` + `asiento_contable`.
Nunca "primero acredito y después registro": si el proceso muere en el medio, el
saldo quedó inflado sin respaldo. Ver `contabilidad-partida-doble`.

## Conciliación

```
extracto_bancario → movimiento_bancario → conciliacion → excepcion_conciliacion
```

Se emparejan los movimientos del banco con los pagos del sistema por referencia,
monto y fecha valor. Los cuatro casos que hay que resolver:

| Caso | Qué significa | Qué se hace |
| --- | --- | --- |
| **En el banco y no en el sistema** | Cobramos y no acreditamos | Se acredita con la evidencia del extracto; es deuda con el cliente |
| **En el sistema y no en el banco** | Acreditamos algo que no entró | Excepción grave: se investiga antes de reversar |
| **Monto distinto** | Comisión del proveedor, tipo de cambio o error | Se registra la diferencia; si es costo del proveedor, va a `costo_proveedor_operacion` |
| **Duplicado en el banco** | El cliente pagó dos veces | Se acredita y se gestiona la devolución, no se descarta |

Ninguno se cierra sin explicación escrita. El cierre diario **no procede** con
excepciones abiertas (`R-BIL-12`).

## Devolver

`reembolso` para el dinero del pago; `devolucion_comision` + nota de crédito para
la comisión (skill `facturacion-sin`). Una devolución es un movimiento nuevo en
sentido inverso, con su motivo, nunca la edición del pago original.

## Disputas

`disputa_pago` tiene plazo, evidencia y resultado. Mientras está abierta, el saldo
involucrado puede retenerse (`retencion_saldo`), que **no es** debitarlo. Perder
una disputa genera el movimiento correspondiente y, si hubo pérdida operativa,
`evento_riesgo_operativo`.

## Integrar un proveedor nuevo

- [ ] Fila en `proveedor_pago` con su configuración, no constantes en el código.
- [ ] Adaptador detrás de la interfaz de dominio: el caso de uso no sabe qué
      proveedor es.
- [ ] Verificación de firma implementada y probada con una firma inválida.
- [ ] Mapeo de sus estados a los nuestros, incluidos los que no tenemos.
- [ ] Sus comisiones a `costo_proveedor_operacion`: el margen real se mide.
- [ ] Doble contra sus fallas reales: timeout, duplicado, fuera de orden, error
      permanente (skill `pruebas-cu`).

## Checklist de un flujo de cobro

- [ ] El QR tiene vencimiento guardado y monto fijo.
- [ ] La referencia es única por proveedor.
- [ ] El webhook verifica firma y guarda el crudo antes de procesar.
- [ ] Acreditación y registro contable en una sola transacción.
- [ ] Existe el camino de conciliación para los cuatro casos.
- [ ] Hay prueba de webhook duplicado y de webhook fuera de orden.
- [ ] El pago tardío tras vencimiento tiene destino definido, no silencio.

## Ver también

`contabilidad-partida-doble` · `idempotencia-reintentos` · `trabajos-outbox` ·
`facturacion-sin` · `errores-api` · CU-20 a CU-24 · familia `R-BIL`
