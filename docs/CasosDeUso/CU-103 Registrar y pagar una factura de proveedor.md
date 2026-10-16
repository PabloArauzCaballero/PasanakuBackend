---
tags:
  - caso-uso
  - modulo/13-contabilidad-erp
codigo: CU-103
criticidad: alta
actores: [Contabilidad, Tesorería]
normas: [Ley 393 (libros y conservación), Código de Comercio, SIN]
---

# CU-103 — Registrar y pagar una factura de proveedor

> **Objetivo.** Que la empresa le pague a un proveedor externo con la misma
> disciplina de partida doble y segregación de funciones que cualquier otro
> movimiento de dinero del sistema.

## Actores y disparador

- **Actor principal:** Contabilidad (registra y aprueba), Tesorería (paga).
- **Disparadores:** llega una factura física o electrónica de un
  [[tercero_comercial]], vinculada o no a una [[orden_compra]] previa.

## Precondiciones

1. Existe el [[tercero_comercial]] emisor.
2. Si hay [[orden_compra]] asociada, está en estado `APROBADA` o
   `RECIBIDA_PARCIAL`/`RECIBIDA_TOTAL`.
3. El [[periodo_contable]] correspondiente a la fecha de emisión está `ABIERTO`.

## Flujo principal

1. Se registra la [[factura_proveedor]] con `numero_factura`, `monto`, `moneda`,
   `fecha_vencimiento` y estado `REGISTRADA`.
2. Contabilidad aprueba: `estado = 'APROBADA'`, con `aprobada_por`. La tabla es
   append-only, así que la aprobación es un dato del alta (`aprobar: true`) y el
   aprobador es el sujeto autenticado (el token), nunca un dato del cuerpo: el
   contrato no declara `aprobadaPor` y, si llega, se rechaza con 400.
3. **En la misma transacción** que la aprobación se crea el [[asiento_contable]]
   (`DEBE` gasto/activo, `HABER` cuentas por pagar) enlazado por
   `factura_proveedor.asiento_contable_id` (ver
   [[CU-24 Registrar el asiento contable de una operación]]).
4. Tesorería registra el [[pago_a_proveedor]], con `autorizado_por` **distinto**
   de quien aprobó la factura (`R-SEG-04`). `autorizado_por` es el sujeto autenticado
   (el token), nunca un dato del cuerpo de la petición. La forma de pago es una de
   `TRANSFERENCIA`, `CHEQUE` o `EFECTIVO` (`ck_pago_a_proveedor_forma_pago`); `QR` no es
   una forma de pago a proveedor.
   El pago lleva su `Idempotency-Key` (obligatoria): `R-CTB-11`.
5. **En la misma transacción**: se actualiza `monto_pagado`/`saldo_pendiente` de
   la factura, se genera el asiento del pago (`DEBE` cuentas por pagar, `HABER`
   banco/caja), y si `saldo_pendiente = 0`, `estado = 'PAGADA'`.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 4a | `autorizado_por` del pago es la misma persona que `aprobada_por` de la factura | El sistema rechaza el pago (`R-SEG-04`) |
| 1a | El período contable de la fecha de emisión está `CERRADO` | Se rechaza el registro; la factura se asienta en el período abierto vigente con nota |
| — | Pago parcial de una factura | La factura queda `PAGADA_PARCIAL`; admite más de un `pago_a_proveedor` |
| 4b | El mismo pago se repite con la misma `Idempotency-Key` (red, cliente o cola reintentan) | Responde lo mismo que el original, sin registrar otro pago ni otro evento (`R-CTB-11`) |
| 4c | La misma `Idempotency-Key` llega con otro monto, otra forma de pago u otro autorizante | Se rechaza (`CLAVE_DE_IDEMPOTENCIA_REUTILIZADA`): responder con el pago viejo haría creer que el nuevo se hizo |
| — | Se necesita anular una factura ya aprobada sin pagos | `estado = 'ANULADA'` con motivo; **no se borra** (`R-AUD-01`) |

## Postcondiciones

- Toda factura de proveedor aprobada tiene su asiento contable, y todo pago tiene
  el suyo: el saldo de cuentas por pagar del mayor coincide con la suma de
  `saldo_pendiente` de las facturas no anuladas.

## Contrato · `openapi/erp.yaml`

```ts
export const EntradaCU103 = z.object({
  terceroComercialId: z.string().uuid(),
  ordenCompraId: z.string().uuid().optional(),
  numeroFactura: z.string().max(30),
  fechaEmision: z.string().date(),
  fechaVencimiento: z.string().date(),
  monto: MontoSchema,
  aprobar: z.boolean().optional(), // el aprobador es el token; `aprobadaPor` no existe
}).strict()

export const EntradaPagoCU103 = z.object({
  monto: MontoSchema,
  moneda: z.enum(['BOB', 'USD']),
  formaPago: z.enum(['TRANSFERENCIA', 'CHEQUE', 'EFECTIVO']), // sin QR
}).strict() // + cabecera Idempotency-Key (UUID) obligatoria; `autorizadoPor` es el token

export const SalidaCU103 = z.object({
  facturaProveedorId: z.string().uuid(),
  estado: z.string(),
  asientoContableId: z.string().uuid(),
}).strict()

export const ErroresCU103 = {
  PERIODO_CERRADO: 'AP-CU103-01',
  MISMO_APROBADOR_Y_PAGADOR: 'AP-CU103-02',
  FACTURA_DUPLICADA: 'AP-CU103-03',
  MONTO_PAGO_MAYOR_AL_SALDO: 'AP-CU103-04',
  CLAVE_DE_IDEMPOTENCIA_REUTILIZADA: 'AP-CU103-05',
  FILTRO_DE_LISTADO_INVALIDO: 'AP-CU103-06',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `PERIODO_CERRADO` | La fecha de emisión cae en un período ya cerrado |
| `MISMO_APROBADOR_Y_PAGADOR` | `pago_a_proveedor.autorizado_por` = `factura_proveedor.aprobada_por` |
| `FACTURA_DUPLICADA` | Ya existe una factura con el mismo `numero_factura` para ese tercero (`UQ`) |
| `MONTO_PAGO_MAYOR_AL_SALDO` | El pago excede `saldo_pendiente` |
| `CLAVE_DE_IDEMPOTENCIA_REUTILIZADA` | La `Idempotency-Key` ya registró otro pago de esa factura (otro monto, forma o autorizante) |
| `FILTRO_DE_LISTADO_INVALIDO` | En el listado del backoffice: cursor a medias (`antesDe` sin `antesDeId` o al revés) o un estado fuera del catálogo |

**Listado del backoffice (carril A).** `GET /erp/facturas-de-proveedor` (`listarFacturasDeProveedor`) es la cola de cuentas por pagar, de la emisión más reciente a la más antigua (cursor `(fechaEmision, id)`). Lo pagado, el saldo y el estado se **derivan de `pago_a_proveedor`**, igual que al pagar: la factura es append-only y sus columnas `monto_pagado` y `estado` se quedan como nacieron. `aprobadaPorLaSesion` la resuelve el servidor contra el sujeto del token (R-CTB-05): el cliente no compara identidades. Tesorería la lee con `CONTABILIDAD_ERP_PAGAR` porque paga desde ella. Exige `CONTABILIDAD_ERP_CUENTAS_POR_PAGAR`, `CONTABILIDAD_ERP_PAGAR` o `CONTABILIDAD_ERP_REPORTES`. Pagina por cursor keyset con desempate por id (`limite` 1..100, 25 por omisión); un filtro inválido es `AP-CU103-06` y no una lista vacía.

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `validarSegregacionAprobacionPago` | Compara aprobador y pagador; puro |
| Molécula | `FacturaProveedorRepositorio` | Alta, aprobación y actualización de saldo |
| Molécula | `PagoAProveedorRepositorio` | Alta append-only de pagos |
| Organismo | `CU103RegistrarFacturaYPago` | Abre transacción, genera asientos vía `CU24RegistrarAsiento` |
| Página | `apps/backoffice` — cuentas por pagar | Listado con vencimiento visible, acción de pago con doble confirmación |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `factura_proveedor.aprobada` | Genera el asiento de la obligación | `CONTABILIDAD_ERP_CUENTAS_POR_PAGAR` |
| `pago_a_proveedor.registrado` | Genera el asiento del egreso y actualiza saldo | `CONTABILIDAD_ERP_PAGAR` |
| — (listado `listarFacturasDeProveedor`) | — | `CONTABILIDAD_ERP_CUENTAS_POR_PAGAR`, `CONTABILIDAD_ERP_PAGAR` o `CONTABILIDAD_ERP_REPORTES` |

## Interfaz

- **App:** No tiene pantalla: es gestión administrativa interna.
- **Backoffice:** Cuentas por pagar con vencimiento destacado, acción de aprobar
  y de pagar separadas por permiso.

## Restricciones aplicables

`R-AUD-01` · `R-AUD-05` · `R-AUD-06` · `R-CTB-01` · `R-CTB-04` · `R-CTB-05` · `R-CTB-11`

## Evidencia que deja

[[tercero_comercial]] · [[orden_compra]] · [[factura_proveedor]] ·
[[pago_a_proveedor]] · [[asiento_contable]]

## Criterios de aceptación

```gherkin
Dada una factura_proveedor aprobada con saldo_pendiente > 0
Cuando Tesorería registra un pago_a_proveedor por el saldo total
Entonces la factura pasa a estado PAGADA y queda su asiento contable enlazado

Dada una factura aprobada por la usuaria Ana
Cuando Ana intenta autorizar también el pago de esa misma factura
Entonces el sistema devuelve MISMO_APROBADOR_Y_PAGADOR

Dado un período contable cerrado
Cuando se intenta registrar una factura con fecha de emisión dentro de ese período
Entonces el sistema devuelve PERIODO_CERRADO

Dado un pago parcial ya registrado con una clave de idempotencia
Cuando se repite el mismo pago con la misma clave
Entonces el sistema responde lo mismo que la primera vez y no registra otro pago

# Listado del backoffice (backoffice carril A)
Dada una factura aprobada y pagada en parte
Cuando Tesoreria lista las cuentas por pagar
Entonces la ve PAGADA_PARCIAL con lo pagado y el saldo derivados de sus pagos, y no aprobada por ella

Dada una factura aprobada por quien consulta
Cuando lista las cuentas por pagar
Entonces la fila dice que la aprobo la sesion, para que no se le ofrezca pagarla

Dadas dos facturas del mismo dia y una de ellas pagada entera
Cuando Tesoreria las recorre de a una o filtra por PAGADA
Entonces no se repiten y la pagada figura PAGADA con saldo cero
```

## Ver también

[[CU-101 Presupuestar por centro de costo]] · [[CU-102 Dar de alta un tercero comercial y su orden de compra]] · [[CU-24 Registrar el asiento contable de una operación]]
