---
tags:
  - caso-uso
  - modulo/03-aportes-pagos-qr-y-conciliacion
codigo: CU-51
criticidad: alta
actores: [Contabilidad, Sistema]
normas: [Contabilidad, ASFI conciliación]
---

# CU-51 — Ejecutar el cierre diario

> **Objetivo.** Cerrar el día solo cuando **todo cuadra**: pagos conciliados,
> asientos balanceados, custodia verificada y sin excepciones abiertas.

## Actores y disparador

- **Actor principal:** proceso programado + contabilidad.
- **Disparador:** fin del día operativo.

## Precondiciones

1. Se ingirieron los [[extracto_bancario]] del día y se cruzaron los
   [[movimiento_bancario]].
2. Corrió [[CU-50 Conciliar la custodia y verificar el encaje]].

## Flujo principal

1. Se concilian los [[pago]] del día contra los movimientos bancarios
   ([[conciliacion]]).
2. Se listan las [[excepcion_conciliacion]] abiertas de la fecha.
3. Se totalizan recaudado, conciliado y excepciones; se cuenta la cantidad de pagos.
4. Se verifica que todos los [[asiento_contable]] del día estén confirmados y
   balanceados.
5. Se crea [[cierre_diario]] con `cuadrado = (no hay excepciones abiertas) AND
   (conciliación de custodia CUADRADA)` (`R-BIL-12`).
6. Se cierra el día: se generan los [[saldo_diario_billetera]] y se emite
   `evento_dominio` `DIA_CERRADO`.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 2a | Hay excepciones abiertas | `cuadrado=false`; el día queda abierto y visible en tablero hasta resolverlas |
| 5a | Descuadre de custodia | `cuadrado=false` aunque la conciliación de pagos esté perfecta |
| — | Reapertura de un día cerrado | Requiere autorización; se registra `reabierto_en` y el motivo |
| 4a | Asiento sin confirmar | El cierre no procede: primero se resuelve el asiento |

## Postcondiciones

- Un día cerrado y cuadrado es un punto de control confiable para auditoría.
- Los saldos diarios quedan sellados y encadenados.

## Contrato · `openapi/aportes.yaml`

```ts
export const EntradaCU51 = z.object({
  fecha: z.string().date(),
  cerradoPor: z.string().uuid(),
}).strict()

export const SalidaCU51 = z.object({
  cierreId: z.string().uuid(),
  cuadrado: z.boolean(),
  totalRecaudado: MontoSchema,
  totalConciliado: MontoSchema,
  excepcionesAbiertas: z.number().int(),
  saldosDiariosGenerados: z.number().int(),
}).strict()

export const ErroresCU51 = {
  EXCEPCIONES_ABIERTAS: 'AP-CU51-01',
  CUSTODIA_DESCUADRADA: 'AP-CU51-02',
  ASIENTOS_SIN_CONFIRMAR: 'AP-CU51-03',
  DIA_YA_CERRADO: 'AP-CU51-04',
  FILTRO_DE_LISTADO_INVALIDO: 'AP-CU51-05',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `EXCEPCIONES_ABIERTAS` | Hay conciliaciones sin resolver (R-BIL-12) |
| `CUSTODIA_DESCUADRADA` | El encaje del día no cumple |
| `ASIENTOS_SIN_CONFIRMAR` | Quedan asientos en borrador |
| `DIA_YA_CERRADO` | Requiere reapertura autorizada |
| `FILTRO_DE_LISTADO_INVALIDO` | El listado de cierres recibió `antesDe` sin `antesDeId`, o al revés |

**Listado de cierres (backoffice carril A · H8).** `GET /contabilidad/cierres-diarios`
(`listarCierresDiarios`, `AUDITORIA_LEER`) vive en `openapi/nucleo-financiero.yaml`, porque
`cierre_diario` es de ese esquema; pagina por `(fecha, id)` del más reciente al más antiguo. Los
totales viajan como cadena **sin moneda**: la tabla no la modela y no se supone BOB (ambigüedad A-7 del
carril). La acción de cerrar desde el backoffice sigue pendiente: los totales de recaudo vienen de
aportes y exigen un puerto con su contrato.

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `evaluarCuadre` | Reúne las condiciones y devuelve el motivo del no cuadre; puro |
| Molécula | `CierreDiarioRepositorio` | Cierre y su estado |
| Molécula | `SaldoDiarioRepositorio` | Sella los saldos encadenados por hash |
| Organismo | `CU51CerrarDia` | Trabajo diario: sella saldos y cierra la fecha |
| Página | `POST /contabilidad/cierres/:fecha` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `dia.cerrado` | Sellado de saldos y habilitación de reportes | `CONTABILIDAD` |
| `dia.no_cuadra` | Alerta y bloqueo del cierre | — |

## Interfaz

- **App:** Sin pantalla en la app.
- **Backoffice:** Cierre diario con el detalle de lo que impide cuadrar.

## Restricciones aplicables

`R-BIL-12` · `R-AUD-05` · `R-AUD-07`

## Evidencia que deja

[[cierre_diario]] · [[conciliacion]] · [[excepcion_conciliacion]] ·
[[saldo_diario_billetera]]

## Criterios de aceptación

```gherkin
Dado un día sin excepciones y con custodia cuadrada
Cuando se ejecuta el cierre
Entonces cierre_diario.cuadrado es true
Y existen saldo_diario_billetera para todas las cuentas activas

Dado un día con una excepción de conciliación abierta
Cuando se ejecuta el cierre
Entonces cuadrado es false

Dado un día ya cerrado
Cuando el trabajo programado se ejecuta de nuevo
Entonces devuelve el cierre existente y no reescribe los saldos diarios

Dado un asiento_contable sin confirmar en la fecha
Cuando se ejecuta el cierre
Entonces el cierre no procede y el asiento queda señalado como bloqueante

# Listado de cierres (backoffice carril A)
Dados dos días cerrados
Cuando Contabilidad lista los cierres diarios
Entonces los recibe del más reciente al más antiguo con sus totales
```

## Ver también

[[CU-21 Cobrar el aporte del período]] · [[CU-24 Registrar el asiento contable de una operación]] · [[CU-35 Cerrar la liquidación mensual de ingresos]] · [[CU-50 Conciliar la custodia y verificar el encaje]] · [[CU-57 Operar un punto de atención y arquear el efectivo]] · [[CU-72 Sellar el bloque de transparencia]] · [[CU-98 Publicar el tablero de indicadores]]
