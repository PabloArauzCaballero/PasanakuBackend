---
tags:
  - caso-uso
  - modulo/10-billetera-custodia-y-dinero-electronico
codigo: CU-10
criticidad: alta
actores: [Usuario, Proveedor de pago, Punto de atención]
normas: [BCB RD 079/2022, UIF art. 52 inc. i, encaje 100%]
---

# CU-10 — Recargar saldo (cash-in)

> **Objetivo.** Que el dinero entre a la billetera **solo cuando el banco lo
> confirmó**, una sola vez, y que el mismo importe entre a la cuenta de custodia.

## Actores y disparador

- **Actor principal:** usuario.
- **Actores secundarios:** proveedor de pago / banco; punto de atención si es
  efectivo.
- **Disparador:** el usuario elige recargar, o deposita en un punto de atención.

## Precondiciones

1. [[cuenta_billetera]] en estado `ACTIVA` (o `LIMITADA`: recargar siempre está
   permitido porque aumenta la cobertura).
2. [[instrumento_fondeo]] verificado y con `titular_coincide=true`, salvo pago por
   QR de un tercero identificado.
3. La operación pasa [[CU-40 Evaluar límites antes de una operación]] contra
   `SALDO_MAXIMO` del nivel.

## Flujo principal

1. Se crea [[orden_recarga]] con `clave_idempotencia`, `monto_bruto`, `moneda`,
   proveedor y `expira_en`. Estado `PENDIENTE`.
2. Se genera el medio de cobro ([[qr_cobro]] o redirección al proveedor).
3. El proveedor confirma por [[webhook_pasarela]]. **Se valida firma e
   idempotencia antes de tocar saldo.**
4. **En una sola transacción**:
   - se crea [[pago]] conciliable y se enlaza a la orden;
   - se crea [[transaccion_billetera]] `tipo='RECARGA'` con su
     `clave_idempotencia`;
   - se escriben dos [[movimiento_billetera]]: crédito a la cuenta del usuario y
     débito a la cuenta técnica `PUENTE_CUSTODIA` (suma cero, `R-BIL-01`);
   - se actualiza `consumo_limite`;
   - se genera el [[asiento_contable]] espejo ([[CU-24 Registrar el asiento contable de una operación]]);
   - se emite `evento_dominio` `RECARGA_ACREDITADA`.
5. El motor de umbrales evalúa la operación → [[CU-41 Detectar umbral y registrar formulario PCC-01]]
   (carga de billetera acumulada) y [[CU-42 Detectar umbral y registrar ROG]].
6. Cuando el dinero llega efectivamente al banco, se registra
   [[movimiento_custodia]] y se concilia en [[CU-50 Conciliar la custodia y verificar el encaje]].

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 3a | Webhook repetido (misma `clave_idempotencia`) | Se responde 200 sin efecto: **no se acredita dos veces** (`R-BIL-06`) |
| 3b | Firma inválida | Se descarta y se registra el intento; alerta de seguridad |
| 3c | La orden expiró | `estado='EXPIRADA'`; si el dinero igual llegó, va a `SUSPENSO_NO_IDENTIFICADO` y se investiga |
| 4a | Falla a mitad de la escritura | La transacción entera revierte: no existe saldo sin contrapartida |
| 5a | Supera el umbral de la UIF | La acreditación **no se bloquea**, pero se exige el formulario y queda registrado |
| — | El pago excede `SALDO_MAXIMO` del nivel | Se acredita hasta el tope o se rechaza según política, y se ofrece [[CU-02 Elevar nivel de debida diligencia]] |

## Postcondiciones

- El saldo del usuario aumentó exactamente en `monto_acreditado`.
- Existe contrapartida contable y de custodia por el mismo importe.

## Contrato · `openapi/nucleo-financiero.yaml`

```ts
export const EntradaCU10 = z.object({
  claveIdempotencia: z.string().uuid(),
  cuentaBilleteraId: z.string().uuid(),
  monto:             MontoSchema,
  moneda:            MonedaSchema,
  medio:             z.enum(['QR','TARJETA','TRANSFERENCIA']),  // AGENTE retirado: la app no recarga en efectivo
  instrumentoFondeoId: z.string().uuid().optional(),
}).strict()

export const SalidaCU10 = z.object({
  ordenRecargaId: z.string().uuid(),
  estado:         z.enum(['PENDIENTE','ACREDITADA','RECHAZADA','EXPIRADA']),
  qr:             z.object({ payloadEmv: z.string(), expiraEn: z.string().datetime() }).nullable(),
  saldoDespues:   MontoSchema.nullable(),
}).strict()

export const ErroresCU10 = {
  LIMITE_EXCEDIDO: 'AP-CU10-01',
  SALDO_MAXIMO_ALCANZADO: 'AP-CU10-02',
  INSTRUMENTO_NO_VERIFICADO: 'AP-CU10-03',
  CUENTA_NO_OPERATIVA: 'AP-CU10-04',
  ORDEN_EXPIRADA: 'AP-CU10-05',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `LIMITE_EXCEDIDO` | Supera el techo del nivel de diligencia (R-LIM-01) |
| `SALDO_MAXIMO_ALCANZADO` | El saldo resultante excede el máximo del nivel |
| `INSTRUMENTO_NO_VERIFICADO` | El medio de fondeo no está verificado |
| `CUENTA_NO_OPERATIVA` | La cuenta está congelada o cerrada |
| `ORDEN_EXPIRADA` | Se intentó acreditar sobre una orden vencida |

> [!danger] `AGENTE` salió, y con él el efectivo
> La recarga en efectivo por corresponsal **no existe en el producto**:
> [[ADR-039 Sin efectivo · la plataforma no opera dinero físico]] la retiró del alcance el 20 de
> agosto de 2026. El único ingreso de fondos es electrónico.
>
> Con ella se fueron `punto_atencion`, `arqueo_punto_atencion` y la columna
> `orden_recarga.punto_atencion_id`; `instrumento_fondeo.tipo` perdió `AGENTE` y `EFECTIVO`, y
> [[CU-57 Operar un punto de atención y arquear el efectivo]] quedó **obsoleto con su número
> reservado**. Los umbrales UIF de concepto `EFECTIVO` **se conservan**: son la norma, no una
> función nuestra — simplemente nunca se disparan.

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `calcularAcreditacion` | Bruto menos costo del proveedor; devuelve el neto; puro |
| Átomo | `resolverConceptoUif` | Traduce el tipo de operación al concepto del instructivo |
| Molécula | `OrdenRecargaRepositorio` | Alta y cambio de estado de la orden |
| Molécula | `PasarelaAdaptador` | Genera el QR y consulta estado; idempotente por referencia |
| Molécula | `MovimientoBilleteraRepositorio` | Inserta el par de movimientos con contrapartida |
| Organismo | `CU10RecargarSaldo` | Transacción: pago, transacción de billetera, asiento y umbrales |
| Página | `POST /billetera/recargas` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `recarga.acreditada` | Evaluación de umbrales UIF, asiento contable y aviso | `BILLETERA_OPERAR` |
| `recarga.rechazada` | Aviso con el motivo en lenguaje llano | — |

## Interfaz

- **App:** *Cargar saldo*: monto, medio y QR a pantalla completa con la cuenta regresiva.
- **Backoffice:** Monitor de recargas por proveedor, con tasa de acreditación y tiempo medio.

## Restricciones aplicables

`R-BIL-01` · `R-BIL-02` · `R-BIL-06` · `R-BIL-10` · `R-BIL-19` · `R-BIL-20` · `R-BIL-22` ·
`R-LIM-01` · `R-LIM-02` · `R-AUD-01` · `R-AUD-03` · `R-AUD-05` · `R-AUD-10` ·
`R-UIF-02`

## Evidencia que deja

[[orden_recarga]] · [[pago]] · [[webhook_pasarela]] · [[transaccion_billetera]] ·
[[movimiento_billetera]] · [[asiento_contable]] · [[movimiento_custodia]] ·
[[respuesta_idempotente]] · [[registro_operacion_relevante]] (si aplica) ·
[[discrepancia_proveedor]]

## Criterios de aceptación

```gherkin
Dado un webhook de acreditación válido
Cuando se procesa por primera vez
Entonces el saldo_disponible aumenta en monto_acreditado
Y existen exactamente dos movimiento_billetera que suman cero

Dado el mismo webhook reenviado tres veces
Cuando se procesan
Entonces existe una sola transaccion_billetera
Y el saldo no cambia después del primer procesamiento

Dadas dos cuentas con la misma clave de idempotencia
Cuando cada una solicita una recarga
Entonces cada una recibe una orden distinta

Dado que el usuario acumula USD 1.000 en cargas en 3 días calendario
Cuando se acredita la última
Entonces existe un registro_operacion_relevante con formulario PCC-01

# Cotización: el precio se ve antes de confirmar
Dada una recarga de Bs 500 con un costo de Bs 5
Cuando se cotiza y luego se confirma citando esa cotización
Entonces la vista muestra base, comisión, impuesto, total, neto y vigencia
Y la orden cobra exactamente lo cotizado y tarifas registra la aceptación

Dada una operación gravada sin cotización citada
Cuando se solicita la recarga
Entonces se rechaza
Y no se crea la orden ni se envía nada al proveedor

Dada una cotización vencida
Cuando se solicita la recarga citándola
Entonces se rechaza porque venció
Y no se crea la orden, no se envía nada al proveedor y tarifas no registra ninguna aceptación

Dada una cotización emitida para Bs 500
Cuando se solicita otro importe con la misma clave citando esa cotización
Entonces se rechaza porque la cotización no corresponde al importe
Y el nuevo importe obtiene su propia cotización en vez de reutilizar la anterior

Dada una cotización que nunca se emitió para esta solicitud
Cuando se solicita la recarga citándola
Entonces se rechaza
Y no se crea la orden

Dada una cotización vigente que tarifas no acepta registrar
Cuando se solicita la recarga citándola
Entonces se rechaza
Y no se crea la orden ni se envía nada al proveedor

Dado tarifas caído
Cuando se cotiza o se solicita una recarga
Entonces ambas se rechazan: no se cobra cero por no saber
Y no se crea ninguna orden

Dada una operación gratuita de Bs 500
Cuando se cotiza y se solicita sin citar cotización
Entonces la vista es gratuita, no hay cotización que citar y se acredita el importe completo
Y citar una cotización se rechaza

Dado un costo que iguala o supera el importe de la recarga
Cuando se cotiza
Entonces se rechaza porque no hay recarga que acreditar

Dado un retiro gravado con cotización vigente
Cuando se cotiza, se confirma y se solicita citándola
Entonces la orden de retiro guarda la cotización y su neto descuenta el costo
Y un retiro gravado sin cotización se rechaza

# Discrepancias con el proveedor
Dada una confirmación del proveedor con firma inválida
Cuando se intenta confirmar la recarga
Entonces el saldo y los movimientos no cambian y la orden sigue PENDIENTE
Y se abre una discrepancia FIRMA_INVALIDA con una alerta correlacionada

Dada la misma evidencia hostil del proveedor repetida tres veces
Cuando se intenta confirmar cada vez
Entonces la discrepancia se registra una sola vez y la alerta no se repite
Y el saldo no cambia

Dado un proveedor que informa un importe distinto del de la orden
Cuando se intenta confirmar la recarga
Entonces no acredita ni crea movimientos
Y la discrepancia MONTO_DISTINTO deja el importe esperado y el informado

Dado un proveedor que responde por otra referencia
Cuando se intenta confirmar la recarga
Entonces no acredita
Y queda registrada una discrepancia REFERENCIA_DISTINTA

Dada una recarga ya acreditada en el libro
Cuando el proveedor ahora dice rechazado o confirma con otra transacción
Entonces se registra un estado contradictorio por cada respuesta
Y el saldo y los movimientos quedan intactos

Dada una orden de recarga ya cerrada en el libro
Cuando el proveedor la confirma
Entonces se registra una discrepancia de estado contradictorio
Y no se acredita

Dado un proveedor que rechaza legítimamente la recarga
Cuando se intenta confirmar, también al repetir
Entonces la orden queda RECHAZADA con un solo evento
Y no se abre ninguna discrepancia ni se toca el libro

# Idempotencia: la clave es de cada billetera
Dados dos titulares que usan la misma clave de idempotencia
Cuando cada uno solicita su recarga
Entonces cada uno tiene su propia orden: la clave se ampara en la billetera
Y repetir la clave de uno devuelve su orden, no la del otro

# Contra el proveedor real
Dada una recarga pendiente en el proveedor real
Cuando se intenta confirmar antes de que el proveedor resuelva
Entonces no acredita
Y cuando el proveedor confirma se acredita una sola vez y repetir devuelve lo mismo

Dado un proveedor real que se cae con una recarga pendiente
Cuando se intenta confirmar y luego vuelve con su archivo
Entonces con el proveedor caído no se acredita porque el resultado es desconocido
Y al volver conserva la operación y la confirmación se acredita una sola vez

Dado un proveedor real que rechaza la recarga
Cuando se intenta confirmar
Entonces la orden queda RECHAZADA
Y no se acredita nada

Dada una confirmación firmada con otro secreto contra el servidor real
Cuando se intenta confirmar la recarga
Entonces no acredita y deja una discrepancia FIRMA_INVALIDA
Y con el secreto correcto la misma confirmación sí acredita

Dada una recarga confirmada contra el proveedor real
Cuando se revisa el log del proveedor
Entonces no contiene la clave de acceso, la clave de control ni la firma de la corrida
```

## Ver también

[[CU-11 Retirar saldo]] · [[CU-40 Evaluar límites antes de una operación]] · [[CU-50 Conciliar la custodia y verificar el encaje]] · [[CU-57 Operar un punto de atención y arquear el efectivo]] · [[CU-99 Dar de alta un proveedor de pago y enrutar el cobro]]
