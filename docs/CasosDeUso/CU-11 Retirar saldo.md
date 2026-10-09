---
tags:
  - caso-uso
  - modulo/10-billetera-custodia-y-dinero-electronico
codigo: CU-11
criticidad: alta
actores: [Usuario, Proveedor de pago, Aprobador]
normas: [BCB, UIF art. 52 inc. i, antifraude]
---

# CU-11 — Retirar saldo (cash-out)

> **Objetivo.** Que el titular pueda sacar su dinero cuando quiera, y que nadie más
> pueda. Es la operación de mayor riesgo del sistema y por eso es **pesimista**:
> primero se reserva, después se paga.

## Actores y disparador

- **Actor principal:** titular de la cuenta.
- **Actores secundarios:** proveedor de pago; aprobador interno si el monto lo exige.
- **Disparador:** solicitud de retiro desde la app.

## Precondiciones

1. Cuenta `ACTIVA` o `LIMITADA` (retirar el propio saldo siempre debe ser posible,
   salvo bloqueo de autoridad).
2. [[instrumento_fondeo]] destino verificado, `titular_coincide=true` y
   `bloqueado_hasta` vencido (`R-BIL-09`).
3. Sin [[bloqueo_saldo]] vigente que afecte el importe.
4. `saldo_disponible >= monto_solicitado + costo_retiro`.

## Flujo principal

1. Se cotiza el costo con [[CU-30 Cotizar la comisión antes de operar]] y se muestra
   el neto final.
2. Se evalúan límites ([[CU-40 Evaluar límites antes de una operación]]) y
   antifraude ([[evaluacion_antifraude]]).
3. Se exige MFA ([[CU-04 Autenticar con MFA y registrar dispositivo]]);
   `orden_retiro.mfa_verificado=true`.
4. **En la misma transacción**: se crea [[orden_retiro]] con `clave_idempotencia` y
   se crea la [[retencion_saldo]] por el importe total
   (`motivo='ENTREGA_EN_CURSO'`), moviendo el importe de disponible a retenido.
5. Si el monto supera el tope de política, se exige segundo aprobador
   (`aprobada_por` ≠ solicitante, `R-SEG-04`) y/o se respeta
   `ventana_enfriamiento_hasta`.
6. Se envía la instrucción al proveedor con la misma clave de idempotencia.
7. Al confirmarse el pago, **en una transacción**:
   - se ejecuta la retención (`estado='EJECUTADA'`);
   - se crea [[transaccion_billetera]] `tipo='RETIRO'` con débito al usuario y
     crédito a `PUENTE_CUSTODIA`;
   - se registra el [[cargo_comision]] del costo de retiro si el tarifario lo
     define;
   - se genera el asiento contable y el `evento_dominio` `RETIRO_PAGADO`.
8. Se evalúan umbrales UIF (retiro de billetera acumulado) →
   [[CU-41 Detectar umbral y registrar formulario PCC-01]].

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 2a | Antifraude decide `REVISAR` | La orden queda `EN_REVISION`; la retención se mantiene; se notifica plazo al usuario |
| 2b | Antifraude decide `RECHAZAR` | Se libera la retención y se registra el motivo; el usuario puede reclamar ([[CU-52 Atender un reclamo en plazo]]) |
| 6a | El proveedor falla o rechaza | Se libera la retención (`estado='LIBERADA'`) y el saldo vuelve a disponible. **Nunca queda dinero en el limbo** |
| 6b | Timeout sin respuesta | Se consulta estado por idempotencia; sin confirmación, la retención vence por `expira_en` y se libera |
| 3a | Instrumento agregado hoy | `bloqueado_hasta` impide el retiro: enfriamiento anti-toma de cuenta |
| — | Existe [[bloqueo_saldo]] parcial | Solo se puede retirar el excedente no bloqueado |

## Postcondiciones

- O el usuario recibió el dinero y su saldo bajó, o el saldo volvió íntegro a
  disponible. No hay tercer estado.

## Contrato · `openapi/nucleo-financiero.yaml`

```ts
export const EntradaCU11 = z.object({
  claveIdempotencia: z.string().uuid(),
  cuentaBilleteraId: z.string().uuid(),
  monto:             MontoSchema,
  instrumentoDestinoId: z.string().uuid(),
  factorMfa:         z.string().min(6).max(8),
}).strict()

export const SalidaCU11 = z.object({
  ordenRetiroId: z.string().uuid(),
  estado:        z.enum(['PENDIENTE','EN_REVISION','AUTORIZADA','PAGADA','RECHAZADA']),
  costoRetiro:   MontoSchema,
  montoNeto:     MontoSchema,
  retencionId:   z.string().uuid(),
}).strict()

export const ErroresCU11 = {
  SALDO_INSUFICIENTE: 'AP-CU11-01',
  MFA_REQUERIDO: 'AP-CU11-02',
  INSTRUMENTO_EN_ENFRIAMIENTO: 'AP-CU11-03',
  TITULAR_NO_COINCIDE: 'AP-CU11-04',
  BLOQUEO_DE_AUTORIDAD: 'AP-CU11-05',
  ENCAJE_INCUMPLIDO: 'AP-CU11-06',
  MFA_INVALIDO: 'AP-CU11-07',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `SALDO_INSUFICIENTE` | El disponible no cubre monto más costo (R-BIL-02) |
| `MFA_REQUERIDO` | No se envió evidencia de segundo factor (R-BIL-09) |
| `INSTRUMENTO_EN_ENFRIAMIENTO` | El destino se agregó dentro de la ventana de enfriamiento |
| `TITULAR_NO_COINCIDE` | El instrumento no es del titular |
| `BLOQUEO_DE_AUTORIDAD` | Hay saldo inmovilizado por oficio |
| `ENCAJE_INCUMPLIDO` | El sistema está en modo restringido (R-BIL-11) |
| `MFA_INVALIDO` | Se envió evidencia de segundo factor, pero la firma, el vencimiento, el propósito o el `jti` no son válidos (R-BIL-09, H2) |

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `calcularNetoDeRetiro` | Monto menos costo, con redondeo declarado; puro |
| Átomo | `puedeRetirar` | Reúne las condiciones duras y devuelve el motivo del rechazo |
| Molécula | `OrdenRetiroRepositorio` | Alta y transiciones |
| Molécula | `RetencionSaldoRepositorio` | Reserva y liberación del importe |
| Molécula | `DesembolsoAdaptador` | Instrucción al proveedor con la misma clave de idempotencia |
| Organismo | `CU11RetirarSaldo` | Transacción: retención primero, pago después; nunca al revés |
| Página | `POST /billetera/retiros` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `retiro.solicitado` | Retención del importe y evaluación antifraude | `BILLETERA_OPERAR` |
| `retiro.pagado` | Transacción de billetera, asiento y umbrales UIF | — |
| `retiro.rechazado` | Liberación de la retención y aviso | — |

## Interfaz

- **App:** *Retirar*: destino, monto, costo y neto a la vista antes de confirmar con biometría.
- **Backoffice:** Cola de retiros en revisión, con el puntaje antifraude y la decisión del motor.

## Restricciones aplicables

`R-BIL-01` · `R-BIL-02` · `R-BIL-06` · `R-BIL-07` · `R-BIL-08` · `R-BIL-09` ·
`R-BIL-11` · `R-BIL-19` · `R-BIL-20` · `R-SEG-04` · `R-LIM-01` · `R-AUD-01` ·
`R-AUD-03` · `R-UIF-02`

## Evidencia que deja

[[orden_retiro]] · [[retencion_saldo]] · [[evaluacion_antifraude]] ·
[[transaccion_billetera]] · [[movimiento_billetera]] · [[asiento_contable]] ·
[[respuesta_idempotente]] ·
[[registro_operacion_relevante]] (si aplica) ·
[[discrepancia_proveedor]]

## Criterios de aceptación

```gherkin
Dado un usuario con saldo suficiente y MFA verificado
Cuando solicita un retiro
Entonces se crea una retencion_saldo VIGENTE por el importe total
Y el saldo_disponible disminuye y el saldo_retenido aumenta en el mismo monto

Dado un retiro cuyo proveedor responde error definitivo
Cuando se procesa la respuesta
Entonces la retención queda LIBERADA
Y el saldo_disponible vuelve a su valor original

Dadas dos cuentas con la misma clave de idempotencia
Cuando cada una solicita un retiro
Entonces cada una recibe una orden distinta

Dada una orden de retiro con costo registrado
Cuando se reintenta con la misma clave y otro costo
Entonces se devuelve el costo registrado

Dado un instrumento de fondeo agregado hace una hora
Cuando el usuario intenta retirar hacia él
Entonces la operación se rechaza por período de enfriamiento

Dado un retiro por encima del umbral de doble aprobacion
Cuando se solicita
Entonces la orden nace EN_REVISION, no AUTORIZADA

Dado un retiro EN_REVISION
Cuando un aprobador DISTINTO del solicitante lo aprueba
Entonces la orden pasa a AUTORIZADA con aprobada_por igual al aprobador

Dado un retiro bajo el umbral
Cuando se solicita y queda AUTORIZADA
Entonces las metricas withdrawal_requested_total y withdrawal_approved_total suman, y withdrawal_failed_total no

Dado un retiro que no pasa una condicion dura
Cuando se rechaza antes de crear la orden
Entonces solo suma la metrica withdrawal_failed_total, nunca requested ni approved

# Idempotencia: la clave es de cada billetera
Dados dos titulares que usan la misma clave de idempotencia
Cuando cada uno solicita su retiro
Entonces cada billetera tiene su espacio de claves y cada uno tiene su propia orden
Y la clave de uno no devuelve la orden del otro ni la rompe

# Despacho al proveedor
Dado un retiro cuyo proveedor liquida pero pierde la respuesta
Cuando se despacha la orden
Entonces se consulta al proveedor en vez de reenviar y el libro paga una sola vez
Y repetir el despacho devuelve lo mismo sin otro envío

Dado un envío al proveedor que se pierde antes de llegar
Cuando se despacha la orden
Entonces queda en proceso con el dinero retenido y sin débito
Y el siguiente intento lo envía y, al confirmarse, el libro paga una vez

Dado un proveedor caído
Cuando se despacha la orden
Entonces no se envía nada: sin poder preguntar no hay un segundo envío a ciegas
Y la orden queda en proceso con el dinero retenido

Dada una orden despachada que el proveedor rechaza en firme
Cuando se resuelve la orden
Entonces la orden queda RECHAZADA, la retención se libera y el saldo disponible vuelve
Y el libro no se toca, tampoco al repetir la resolución

Dada una orden despachada cuya respuesta del proveedor trae otro importe o referencia
Cuando se resuelve la orden
Entonces se registra una discrepancia MONTO_DISTINTO con el importe esperado y el informado
Y la orden sigue en proceso, el dinero retenido y no se debita nada

Dada una orden despachada cuya consulta al proveedor llega con firma inválida
Cuando se resuelve la orden
Entonces se registra una discrepancia FIRMA_INVALIDA
Y nada se paga y el dinero sigue retenido

Dada una orden ya pagada en el libro
Cuando el proveedor dice después que la rechazó
Entonces se registra un estado contradictorio
Y el pago no se deshace: sigue PAGADA con su único débito

# Contra el proveedor real
Dado el proveedor real que liquida y pierde la respuesta
Cuando se despacha la orden
Entonces la consulta por referencia encuentra la operación y el libro paga una sola vez
Y repetir el despacho devuelve lo mismo sin otra operación en el proveedor

Dado el proveedor real que muere con la orden pendiente
Cuando se resuelve la orden con el proveedor caído y luego vuelve con su archivo
Entonces caído el resultado es desconocido y el dinero sigue retenido
Y al volver se resuelve sin segundo envío: una operación y un débito

Dado un envío que se corta antes de llegar al proveedor real
Cuando se despacha la orden y se vuelve a intentar
Entonces la primera vez no hay operación en el proveedor y el dinero sigue retenido
Y el siguiente intento la crea una sola vez y el libro paga una vez

Dada una orden despachada que el proveedor real rechaza
Cuando se resuelve la orden
Entonces el dinero vuelve a estar disponible y no queda nada retenido
Y el libro no registra ningún débito
```

## Ver también

[[CU-04 Autenticar con MFA y registrar dispositivo]] · [[CU-10 Recargar saldo]] · [[CU-13 Retener y liberar saldo]] · [[CU-16 Cerrar billetera y devolver saldo]] · [[CU-17 Bloquear saldo por orden de autoridad]] · [[CU-18 Registrar y verificar una cuenta bancaria de destino]] · [[CU-28 Emitir la orden de desembolso y ejecutar el intento]] · [[CU-40 Evaluar límites antes de una operación]] · [[CU-57 Operar un punto de atención y arquear el efectivo]]
