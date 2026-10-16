---
tags:
  - caso-uso
  - modulo/10-billetera-custodia-y-dinero-electronico
codigo: CU-13
criticidad: media
actores: [Sistema, Operador]
normas: [Integridad de saldo, ASFI Consumidor Financiero]
---

# CU-13 — Retener y liberar saldo

> **Objetivo.** Apartar dinero sin moverlo de dueño, de forma visible y explicable
> para el titular, y garantizar que **toda retención termina**: se ejecuta, se
> libera o vence.

## Actores y disparador

- **Actor principal:** el sistema.
- **Disparadores:** aporte programado; entrega en curso; retiro en trámite;
  disputa; alerta antifraude; comisión pendiente; orden de autoridad.

## Precondiciones

1. La cuenta tiene `saldo_disponible >= monto` a retener.

## Flujo principal

1. Se crea [[retencion_saldo]] con `motivo`, `monto`, `referencia_tipo`/`referencia_id`
   y **`expira_en` obligatorio** salvo motivo `ORDEN_AUTORIDAD` (`R-BIL-08`).
2. **En la misma transacción** se ajusta la cuenta: baja `saldo_disponible`, sube
   `saldo_retenido`. El `saldo_total` no cambia.
3. La app muestra la retención con texto en lenguaje llano: *"Bs 500 reservados
   para tu aporte del 10 de marzo"*.
4. Al concretarse el hecho, la retención se **ejecuta**: se crea la
   [[transaccion_billetera]] correspondiente y la retención pasa a `EJECUTADA`
   dentro de la misma transacción.
5. Si el hecho no ocurre, se **libera**: `estado='LIBERADA'`, el importe vuelve a
   disponible y se registra `liberada_por` y el motivo.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 1a | Saldo insuficiente | No se crea la retención; el hecho que la requería no puede avanzar |
| 5a | La retención vence sin resolverse | Un proceso diario la libera por `expira_en` y deja constancia. Ninguna retención queda viva indefinidamente |
| — | La retención se **ejecuta con destino** (inversión voluntaria → custodia de inversión; compra del mercado → vendedor y cargos) | Una operación **de servicio a servicio** (`SERVICIO_INVERSIONES` / `SERVICIO_ENTREGAS`; un usuario final recibe 403 aunque opere su billetera) cierra la retención `EJECUTADA` y escribe el movimiento en la **misma transacción**. El beneficiario y el reparto no los dicta quien llama: salen de la oferta que `entregas` confirma. Sin confirmación no se paga |
| — | Retención por orden de autoridad | No expira: solo se levanta con [[CU-17 Bloquear saldo por orden de autoridad]] |
| — | El titular reclama por saldo retenido | Se responde con la fila: motivo, monto, referencia y vencimiento ([[CU-52 Atender un reclamo en plazo]]) |

## Postcondiciones

- `saldo_retenido` es siempre igual a la suma de retenciones vigentes (`R-BIL-07`).
- Toda retención tiene un final registrado.

## Contrato · `openapi/nucleo-financiero.yaml`

```ts
export const EntradaCU13 = z.object({
  cuentaBilleteraId: z.string().uuid(),
  monto:  MontoSchema,
  motivo: z.enum(['APORTE_PROGRAMADO','ENTREGA_EN_CURSO','DISPUTA','ORDEN_AUTORIDAD','ANTIFRAUDE','COMISION_PENDIENTE',
                  'INVERSION_VOLUNTARIA','COMPRA_EN_MERCADO']),
  referencia: z.object({ tipo: z.string(), id: z.string().uuid() }).optional(),
  expiraEn: z.string().datetime().optional(),
}).strict()

export const SalidaCU13 = z.object({
  retencionId: z.string().uuid(),
  saldoDisponible: MontoSchema,
  saldoRetenido:   MontoSchema,
  expiraEn:        z.string().datetime().nullable(),
}).strict()

export const ErroresCU13 = {
  SALDO_INSUFICIENTE: 'AP-CU13-01',
  VENCIMIENTO_REQUERIDO: 'AP-CU13-02',
  RETENCION_YA_EJECUTADA: 'AP-CU13-03',
  CLAVE_DE_OTRA_OPERACION: 'AP-CU13-04',
  MOTIVO_INCOMPATIBLE: 'AP-CU13-05',
  MONTO_DISTINTO_DE_LO_RETENIDO: 'AP-CU13-06',
  CUENTA_DE_SISTEMA_NO_CONFIGURADA: 'AP-CU13-07',
  DESTINATARIO_INVALIDO: 'AP-CU13-08',
  SOLO_SERVICIO: 'AP-CU13-09',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `SALDO_INSUFICIENTE` | No hay disponible para retener |
| `VENCIMIENTO_REQUERIDO` | Falta expiración y el motivo no es orden de autoridad (R-BIL-08) |
| `RETENCION_YA_EJECUTADA` | Se intentó operar sobre una retención cerrada |
| `CLAVE_DE_OTRA_OPERACION` | La misma clave ya identifica otra petición u otra operación |
| `MOTIVO_INCOMPATIBLE` | La retención no es del motivo que la operación ejecuta (inversión o compra del mercado) |
| `MONTO_DISTINTO_DE_LO_RETENIDO` | El importe no es exactamente lo retenido, o el precio de la oferta no cabe en ello |
| `CUENTA_DE_SISTEMA_NO_CONFIGURADA` | No hay custodia de inversión (o ingresos de plataforma) activa en esa moneda |
| `DESTINATARIO_INVALIDO` | La oferta no se pudo confirmar con `entregas`, no es la de la retención, o el vendedor es el comprador o no tiene billetera |
| `SOLO_SERVICIO` | Quien llama no es la identidad de servicio que esta operación exige |

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `vigenciaDeRetencion` | Calcula el vencimiento según motivo y política; puro |
| Molécula | `RetencionSaldoRepositorio` | Alta, ejecución y liberación |
| Organismo | `CU13RetenerSaldo` | Transacción: retención y recálculo de saldos por trigger |
| Página | `POST /billetera/retenciones` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `saldo.retenido` | Aviso al titular con el motivo en lenguaje llano | Interno o `BACKOFFICE` |
| `saldo.liberado` | Devolución del importe a disponible | — |

## Interfaz

- **App:** El saldo retenido se muestra separado, con el motivo y hasta cuándo.
- **Backoffice:** Listado de retenciones vigentes por cuenta, con su origen.

## Restricciones aplicables

`R-BIL-02` · `R-BIL-03` · `R-BIL-07` · `R-BIL-08` · `R-BIL-16` · `R-BIL-24` · `R-BIL-25` · `R-BIL-26` · `R-AUD-04`

## Evidencia que deja

[[retencion_saldo]] · [[cuenta_billetera]] · [[transaccion_billetera]] (si se ejecuta)

> Reparto de la venta de un turno, adoptado por delegación del dueño (2026-10-11) y alineado con lo que modela
> `entregas`: el comprador paga el **precio** (lo retenido), el vendedor cobra `precio − cargos` y la plataforma
> ingresa los `cargos`, cada uno con su línea en la transacción (regla 91.3.4). Los cargos los afirma `entregas` con
> la oferta; sin ellos no se paga. Errores: `AP-CU13-06` si el precio no es lo retenido o los cargos son negativos o superan el precio.

## Criterios de aceptación

```gherkin
Dada una cuenta con Bs 1.000 disponibles
Cuando se retienen Bs 400
Entonces saldo_disponible es 600 y saldo_retenido es 400
Y saldo_total sigue siendo 1.000

Dada una retención vencida y no ejecutada
Cuando corre el proceso diario
Entonces queda LIBERADA y el saldo vuelve a disponible

Dado un intento de crear una retención sin expira_en y motivo distinto de ORDEN_AUTORIDAD
Cuando se inserta
Entonces la base de datos la rechaza (R-BIL-08)

Dada una retención INVERSION_VOLUNTARIA de Bs 300 sobre una cuenta con Bs 1.000
Cuando el servicio de inversiones la debita
Entonces el titular queda con Bs 700 disponibles y Bs 0 retenidos
Y la custodia de inversión recibe Bs 300 en una transacción que cuadra

Dada una retención COMPRA_EN_MERCADO de Bs 500 para una oferta de precio Bs 500 con cargos de Bs 20
Cuando el servicio de entregas la paga
Entonces el vendedor recibe Bs 480 y la plataforma Bs 20
Y la retención queda EJECUTADA una sola vez

Dado una persona con una billetera BOB activa
Cuando pregunta por su billetera propia
Entonces recibe la suya y no la de otra persona
```

## Ver también

[[CU-12 Transferir saldo entre billeteras]] (rescate de inversión) · [[CU-14 Reversar una transacción]] (el débito y el pago se compensan con el espejo) · [[CU-11 Retirar saldo]] · [[CU-22 Liquidar y entregar el fondo]] · [[CU-17 Bloquear saldo por orden de autoridad]]
