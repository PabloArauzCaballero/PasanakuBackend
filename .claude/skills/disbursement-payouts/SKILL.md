---
name: disbursement-payouts
description: Gate de diseño de la entrega del fondo y el desembolso — la entrega como liquidación con bolsa bruta y deducciones línea a línea, validaciones previas obligatorias, cuenta bancaria verificada con periodo de enfriamiento, orden de desembolso idempotente contra el proveedor, reintentos sin doble pago, confirmación de recepción, incidencias y devoluciones bancarias, y los controles anti-fraude del punto de no retorno. Usar al modelar o tocar entrega de fondo, deducciones, cuenta bancaria del beneficiario, orden o intento de desembolso, confirmación o incidencia, y antes de cerrar cualquier cambio por el que salga plata del sistema.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Entrega del fondo y desembolso

Es el único momento en que la plata **sale**. Todo lo demás se puede corregir con un
contra-asiento; esto, no. Una vez ordenado el desembolso, el error se resuelve pidiéndole a una
persona que devuelva dinero, que es un proceso lento, caro y a veces imposible.

Servicio dueño: `entregas` (M4). Reglas duras: **91.3** y **91.6**.

## 1. La entrega es una liquidación, no una transferencia

```
bolsa bruta (aportes acreditados del periodo)
  − deducción: deuda propia del beneficiario
  − deducción: reposición de cobertura usada
  − deducción: otras, según reglamento
  ─────────────────────────────────────────
  = neto a desembolsar
```

- **Cada deducción es una línea** (`deduccion_entrega`) con concepto, monto, origen y el
  registro que la respalda. Un neto que no se explica línea por línea se va a disputar, y vas a
  tener que reconstruirlo a mano (regla 91.3.4).
- La bolsa bruta **se calcula desde el mayor** (regla 91.1.5), no sumando la tabla de pagos ni
  leyendo una proyección (`eventual-consistency-read-models`).
- El cálculo se **congela** al iniciar la liquidación: si sigue vivo mientras se desembolsa, un
  pago que entra en el medio cambia el número entre el cálculo y la orden.
- La suma de deducciones **nunca supera la bolsa bruta** (invariante en base, regla 91.2.3).

## 2. Validaciones previas — todas, en el servidor

`validacion_pre_entrega`, ejecutadas y registradas antes de ordenar nada:

| Validación | Qué corta |
|---|---|
| El turno corresponde a este cupo y a este periodo | Entrega al que no le toca |
| El periodo está cerrado para aportes | Liquidar con plata en camino |
| No hay una entrega previa para este turno | Doble entrega |
| Deuda y coberturas consultadas y congeladas | Neto mal calculado |
| Cuenta bancaria **verificada** y fuera del periodo de enfriamiento | Fraude por cambio de cuenta |
| El beneficiario no tiene restricción que lo impida | Pagar a un expediente abierto que lo prohíbe |
| Saldo real disponible ≥ neto | Descubierto |

Cada validación deja registro con su resultado. Si una falla, la entrega queda en estado de
revisión con el motivo; **nunca se continúa "porque el resto está bien"**.

## 3. La cuenta bancaria del beneficiario

Es el objetivo favorito del fraude: cambiar la cuenta justo antes de cobrar.

1. **Verificación de titularidad** antes de habilitarla (según lo que permita el proveedor:
   micro-depósito, validación de titular, documento). Una cuenta sin verificar no recibe.
2. **Periodo de enfriamiento** tras alta o cambio: no se desembolsa a una cuenta modificada hace
   menos de N horas. El valor es configurable y **no se saltea por pedido de soporte**.
3. Cambio de cuenta ⇒ **notificación al usuario por un canal distinto** al que hizo el cambio, y
   registro auditado (`audit-trail-history`).
4. La cuenta se guarda cifrada y se muestra enmascarada (`data-privacy-financial`).
5. Cambio de cuenta + solicitud urgente de desembolso = **señal de alerta**, revisión manual.

## 4. Orden de desembolso: idempotencia o pagás dos veces

```ts
// orden_desembolso lleva clave_idempotencia UNIQUE; la MISMA clave viaja al proveedor
const orden = await crearOrdenIdempotente({ entregaId, neto, moneda, cuentaId, clave });
const r = await proveedor.transferir({ ...orden, idempotencyKey: orden.clave });
```

- **Una orden por entrega**, con `UNIQUE`. Reintentar la creación devuelve la misma orden.
- La clave viaja al proveedor: su reintento no duplica la transferencia.
- `intento_desembolso` registra cada intento con su resultado; muchos intentos, **una** orden.
- **Timeout sin respuesta ≠ fallo.** La plata puede haber salido. Nunca reintentar a ciegas: se
  **consulta el estado** de la orden por su clave y se decide (`resilience-patterns`).
- Si el proveedor **no soporta idempotencia**: no hay reintento automático. Se consulta estado y
  se escala a revisión manual. Esto se declara en el diseño, no se descubre en producción.

## 5. Estados y el punto de no retorno

```
CALCULADA → VALIDADA → ASENTADA → ORDENADA ═══▶ ENVIADA → CONFIRMADA
                │           │          ║              └─▶ RECHAZADA_BANCO → reintento
                └───────────┴──────────╨── punto de no retorno
```

- Antes de `ORDENADA`: todo se compensa (`saga-distributed-transactions`).
- Después: solo se corrige con **devolución** (`devolucion_fondo`) o gestión con el banco.
- `RECHAZADA_BANCO` (cuenta cerrada, datos mal): la plata vuelve, se registra la incidencia
  (`incidencia_entrega`), se corrige la cuenta y se reintenta con **orden nueva**, no reusando
  la anterior.

## 6. Confirmación de recepción

- `confirmacion_recepcion`: el beneficiario confirma que recibió. Cierra el ciclo y es evidencia
  ante una disputa posterior.
- Se pide por un canal con token de propósito único (`payments-qr-integration` §3).
- **La falta de confirmación no bloquea la contabilidad** —el dinero salió, el asiento existe—
  pero sí dispara seguimiento: una entrega sin confirmar a los N días es una alerta.
- Si el beneficiario dice que no recibió y el proveedor dice que sí: expediente de incidencia
  con evidencia de los dos lados (`dispute-resolution`). Nunca se resuelve por confianza.

## 7. Controles anti-fraude del momento de pagar

- Límite por operación y por día (`money-movement-safety` §5).
- **Doble control** por encima del umbral: quien ordena no es quien aprueba, impuesto por datos.
- Alerta por patrones: misma cuenta bancaria en varios beneficiarios distintos, cambio de cuenta
  reciente, monto muy por encima del histórico del grupo, beneficiario con KYC vencido
  (`kyc-identity-verification`).
- **Todo desembolso manual o excepcional** se audita con actor, motivo y aprobación.

## Anti-patrones

- Desembolsar sin congelar el cálculo.
- Neto sin deducciones desglosadas.
- Bolsa bruta sumada desde la tabla de pagos o desde una proyección.
- Cuenta bancaria sin verificar, o sin periodo de enfriamiento.
- Saltear el enfriamiento "porque el usuario llamó y era urgente".
- Reintentar una transferencia tras un timeout sin consultar estado.
- Reusar la orden rechazada para un reintento.
- Marcar entregado sin respuesta del proveedor.
- Doble control implementado solo en la UI.
- Cambiar la cuenta sin notificar por otro canal.

## Checklist

- [ ] Bolsa bruta desde el mayor y cálculo congelado al iniciar.
- [ ] Deducciones línea a línea, con origen, y su suma no supera la bruta.
- [ ] Todas las validaciones previas ejecutadas y registradas; una falla detiene la entrega.
- [ ] Cuenta verificada, con enfriamiento no saltable y notificación de cambio por otro canal.
- [ ] Orden única por entrega con clave de idempotencia propagada al proveedor.
- [ ] Timeout tratado como estado desconocido: se consulta, no se reintenta a ciegas.
- [ ] Estados con el punto de no retorno explícito; rechazo bancario genera orden nueva.
- [ ] Confirmación de recepción solicitada y seguida.
- [ ] Límites, doble control por datos y alertas de patrón activos.
- [ ] Asientos de la liquidación cuadrados.

## Evidencia / DoD

1. Liquidación completa pegada: bruta, cada deducción, neto, y sus asientos cuadrados.
2. Salida de la prueba de doble orden con la misma clave: una sola transferencia.
3. Salida del intento de desembolso a cuenta dentro del enfriamiento: rechazado.
4. Salida de la simulación de timeout del proveedor: consulta de estado, sin duplicar.
5. Registro de auditoría de un desembolso con doble control.
