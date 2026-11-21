---
tags:
  - arquitectura
  - adr
  - dinero
  - garantia
  - producto
titulo: "ADR-056 — Tu garantía vuelve y la puntualidad vale plata (modelo A + D)"
estado: aceptada
fecha: 2026-11-21
---

# ADR-056 — Tu garantía vuelve y la puntualidad vale plata (modelo A + D)

> La empresa **nunca** pone plata por quien no paga. El faltante lo cubre un **fondo del grupo**, formado con
> un aporte chico de cada participante, en una cuenta segregada que nunca es de la empresa. Quien no paga queda
> debiéndole al fondo; al cierre del ciclo cada uno recupera su aporte menos su parte de lo incobrable. La
> puntualidad da beneficios concretos: turnos tempranos, menos aporte al fondo y la parte de los recargos.
> Reemplaza a [[ADR-051 Garantía del pozo, comisión y autogestión|ADR-051]].

> **Constancia de aceptación (veraz).** Redactado el 2026-11-21 a partir de las decisiones de Pablo, dueño
> del producto, en [`DECISIONES.md`](../trabajo/2026-11-20-auditoria-consolidado/DECISIONES.md): P-01/P-02
> (modelo A + D), P-03, P-04, P-05, P-17, la fila «Parámetros financieros», «Ingreso de la empresa» y las tandas
> 5, 6 y 7. **No agrega reglas de negocio.** Lo que DECISIONES no resuelve queda como `DECISION_REQUIRED`.
> **Pendiente de la aprobación de Pablo en el PR**, del contador (nombres y naturaleza de las cuentas) y de
> Cumplimiento (preguntas L-01, L-02 y L-03 de
> [`PREGUNTAS-LEGALES.md`](../trabajo/2026-11-21-plan-correcciones/PREGUNTAS-LEGALES.md)). **No habilita dinero
> real** (P-14: todo sigue condicionado al dossier H15). Los importes de los ejemplos son **sintéticos**.

## Contexto

ADR-051 (2026-10-11) fijó que **la empresa aporta el faltante**: una reserva empresarial por ciclo
(`CU23ReservarRespaldo`, `CU23CubrirConRespaldo`), con la deuda del moroso como derecho de recuperación de la
empresa (D-02, D-03, D-15) y la comisión como cargo separado del pozo (D-04). Ese diseño trasladaba el riesgo de
mora a la empresa y la obligaba a tener capital y liquidez inmovilizados por grupo. El 2026-11-20 la regla del
producto cambió: **la empresa nunca pone plata por quien no paga** (P-04: eliminar todo el respaldo; inventario
en `docs/trabajo/2026-11-20-auditoria-consolidado/CONSOLIDADO.md` §6).

Además, varias piezas que ADR-051 dejaba vivas contradicen las decisiones nuevas:

- el **aval entre participantes** (`CU26EjecutarAval`) — P-03 lo elimina;
- el **fondo mutual heredado** (`CU23CubrirIncumplimiento`, canal `FONDO_GARANTIA`, `politica_cobertura`), que
  ADR-051 D-15 dejó como legado sin asiento propio — lo reemplaza el modelo A;
- el **recargo por mora que va a la reserva de respaldo** (cuentas 9.x) — P-05 lo manda al perjudicado;
- la **comisión** sin forma decidida — tanda 7: porcentaje único sobre la cuota, en el mismo QR.

## Decisión

### 1. Modelo A — «Tu garantía vuelve»

| # | Regla | Origen |
|---|---|---|
| A1 | Cada participante hace un **aporte al fondo del grupo** con cada cuota. El porcentaje es un parámetro **por nivel de puntualidad** | P-01/P-02, pieza D |
| A2 | El fondo es **del grupo**, en una **cuenta segregada** que nunca es de la empresa. Su saldo se lee **del mayor** | P-01/P-02, regla 91.1.5 |
| A3 | Al vencer el plazo de gracia, el fondo **cubre automáticamente** la cuota impaga, idempotente por obligación y con límite igual al saldo del fondo verificado dentro de la transacción. Sin saldo, no se cubre y la entrega espera con el motivo visible | P-01/P-02, plan H3.S3.M2 |
| A4 | **Subrogación:** quien no pagó queda debiéndole al fondo. Lo que paga después se imputa **primero** a esa deuda; lo recuperado vuelve al fondo | P-01/P-02, plan F3.S1.M6 |
| A5 | **Cierre del ciclo:** a cada participante se le devuelve lo que aportó al fondo. La deuda que no se pudo recuperar se descuenta de esa devolución **en partes iguales**. Cada devolución es un desembolso a cuenta verificada (ADR-055 E8) | P-01/P-02 |
| A6 | El castigo de lo incobrable al cierre **no borra la deuda** del moroso | Plan H3.S3 (diseño de cuentas) |
| A7 | **Retiro sin haber cobrado:** se le devuelve lo aportado (cuotas y aporte al fondo) al final del ciclo; su cupo lo puede tomar un reemplazo invitado | Tanda 7 D-L3-3 |
| A8 | **Reemplazo** por invitación votado por el grupo y **disolución** votada, con liquidación calculada desde el mayor | Tanda 7 D-L2-2 |
| A9 | Sin aval entre participantes, sin subasta ni cesión de turno, sin permuta y sin condonación por voto (lo incobrable se reparte según A5) | P-03, tanda 7 D-L2-1/D-L3-1/D-L3-2 |
| A10 | La empresa **no cubre, no adelanta y no es acreedora** del moroso. No existen cuentas de respaldo ni de caja de la empresa en el circuito del grupo | P-04 |

**Diseño de cuentas — a validar con el contador** (`DECISION_REQUIRED` sobre los nombres y la naturaleza
del plan de cuentas; se transcribe del plan de correcciones, H3.S3):

| Hecho | Debe | Haber |
|---|---|---|
| Aporte al fondo con la cuota | Caja custodia por liquidar | Fondo del grupo (pasivo con el participante) |
| Cobertura de una cuota impaga | Fondo del grupo | Pozo del grupo |
| Nace la deuda del moroso | Cuenta por cobrar al moroso (a favor del fondo) | Fondo del grupo: cobertura por recuperar |
| El moroso paga su deuda | Caja custodia | Cuenta por cobrar al moroso |
| Devolución al cierre | Fondo del grupo | Caja custodia → desembolso a cada participante |
| Incobrable al cierre | Fondo del grupo (reparto en partes iguales) | Cuenta por cobrar al moroso (castigo, que no borra la deuda) |

Todo asiento cumple la regla 91: importes en decimal exacto con su moneda ISO 4217, `CHECK monto >= 0`,
cuadre por asiento garantizado en la base, mayor *append-only* y `clave_idempotencia` con índice `UNIQUE`.
Ningún asiento del fondo toca cuentas de la empresa.

### 2. Pieza D — «La puntualidad vale plata»

| # | Regla | Origen |
|---|---|---|
| D1 | La **calificación** se calcula **solo con hechos**: pagos confirmados por QR y su fecha, ciclos completos y expedientes firmes. No hay calificaciones entre personas. Dos personas con los mismos hechos tienen la misma calificación, y un error se puede reclamar | Tanda 5 «Calificación y gamificación» |
| D2 | **Niveles** con beneficios concretos: porcentaje menor de aporte al fondo y prioridad de turno. Nombres y umbrales son parámetros. En el primer ciclo todos empiezan en el nivel inicial («Semilla») | Tanda 5 |
| D3 | **Orden de turnos por nivel:** los niveles más altos eligen primero; dentro del mismo nivel, sorteo verificable con compromiso y revelación. En el primer ciclo, como todos están en el nivel inicial, el orden sale entero del sorteo | Tanda 5 «Orden de turnos» |
| D4 | Rachas (recuperables por debido proceso), insignias y el logro de grupo «Grupo perfecto» | Tanda 5 |
| D5 | La **reputación negativa solo nace de un expediente** `FIRME` (regla 91.4), nunca de un cálculo automático | Tanda 5, regla 91.4 |
| D6 | Se construye sobre el servicio `transparencia` existente | Tanda 5 |

### 3. Recargo por mora — va al perjudicado

| # | Regla | Origen |
|---|---|---|
| R1 | El recargo se calcula **automáticamente** al vencer el plazo de gracia, con tasa y tope parametrizados | Tanda 7 AMB-L1-1 |
| R2 | Se acredita **al perjudicado** (quien tuvo que esperar), **nunca** a la empresa ni al fondo | P-05 |
| R3 | Es **anulable**: si el descargo prospera, se revierte con un **contra-asiento**. La sanción y la reputación sí esperan al expediente firme | Tanda 7 AMB-L1-1, regla 91.1.3 |

### 4. Redondeo

Las devoluciones del fondo, el reparto de lo incobrable y cualquier reparto en partes iguales se calculan en la
menor unidad de la moneda. **El residuo se asigna de a un centavo, empezando por el participante de mayor nivel
de puntualidad; si hay empate, decide el sorteo del grupo.** La suma repartida es siempre igual al total: nunca se
pierde ni se crea un centavo (tanda 7 D-L2-3, regla 91.6.5).

### 5. Comisión de la empresa

- **Porcentaje único, igual para todos, sobre la cuota.** Sin descuentos ni exenciones (P-17, tanda 7
  D-L5-1/D-L5-2, «Ingreso de la empresa»).
- Se cobra **en el mismo QR de la cuota, como línea aparte**, y se asienta aparte. **Nunca sale del pozo** ni del
  fondo del grupo.
- Se confirma solo por el evento de pago (ADR-055 E12). Se factura con un proveedor de facturación autorizado
  (DECISIONES tanda 8; ADR-058).
- No existe comisión del organizador ni hay organizador (regla 91.5, ADR-055 E5).

### 6. Parámetros financieros

- Porcentaje del fondo por nivel, días de gracia, tasa y tope de recargo, porcentaje de comisión, nombres y
  umbrales de los niveles, umbral de solvencia del fondo, enfriamiento tras cambio de cuenta y demás:
  **configurables y obligatorios, sin valor por defecto**. Si falta uno, la operación que lo usa falla con un
  error que lo nombra y el servicio no arranca sin ellos (regla 90.3.4, regla 97.5.4).
- **Se editan solo desde el portal de administración (backoffice).** Ninguno queda fijo en el código ni en un
  archivo de configuración. Están versionados, con fecha de vigencia y bitácora de quién cambió qué (tanda 6).
- **Cuatro ojos:** una persona propone el cambio y otra distinta lo aprueba, con las dos identidades tomadas del
  token (tanda 7).
- **Vigencia solo para grupos nuevos:** cada grupo **congela sus parámetros al iniciar**, como parte del
  reglamento que aceptaron sus integrantes. Un cambio no afecta a un grupo en curso (tanda 7).
- Los **valores** los fijan Pablo y Cumplimiento, con procedencia registrada (plan F3.S8.M3). Este ADR no
  propone ningún número.

## Motivo

- **La confianza es lo único que vende un pasanaku** (regla 91). Con el modelo A, el participante sabe que su
  aporte al fondo **vuelve**, y que lo que se pierde por un moroso se reparte de forma conocida de antemano.
- **La empresa no asume riesgo de crédito ni custodia**: no necesita capital inmovilizado por grupo, no es
  acreedora de nadie y la regla 91.5 («el organizador no es caja») se extiende a la propia empresa.
- **La puntualidad tiene un precio positivo**, no solo un castigo: quien paga a tiempo aporta menos al fondo,
  elige turno antes y cobra el recargo cuando otro lo hace esperar.
- **Una sola fuente de verdad:** el saldo del fondo, la deuda del moroso y las devoluciones salen del mayor, no de
  sumas ad hoc (regla 91.1.5).

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| Respaldo de la empresa (ADR-051 D-02/D-03/D-15) | P-04: eliminar todo |
| Aval entre participantes | P-03: lo cubre el fondo del grupo |
| B — turno con precio | Descartada por ahora (P-01/P-02) |
| C — colchón propio de cada participante | Descartada por ahora (P-01/P-02) |
| E — fondo que rinde | Descartada por ahora (P-01/P-02). Si el banco paga intereses, no se reparten; su destino es `DECISION_REQUIRED` (ADR-058, PX-3) |
| Recargo para la empresa o para el fondo | P-05: va al perjudicado |
| Comisión fija en bolivianos o con descuentos | Tanda 7: porcentaje único; P-17: sin descuentos |
| Parámetros con valor por defecto en el código | Regla 97.5.4 y decisión «Parámetros financieros» |

## Consecuencias

### Qué deja de regir de ADR-051

| Decisión de ADR-051 | Estado |
|---|---|
| D-02 naturaleza del respaldo (compromiso de la empresa) | **Superada**: no hay respaldo de la empresa (A10) |
| D-03 un solo camino de cobertura con la reserva empresarial | **Superada**: el único camino es el fondo del grupo (A3) |
| D-04 comisión como cargo separado del pozo | **Superada en la forma**: porcentaje único sobre la cuota, en el mismo QR, línea aparte (§5). Se conserva que nunca sale del pozo y que no hay comisión del organizador |
| D-12 sin grupos sin organizador | Ya la había reemplazado ADR-054; ahora no existe organizador (ADR-055 E5) |
| D-15 reserva empresarial única; fondo mutual como legado | **Superada**: el fondo mutual viejo se retira (F2.M9) y lo reemplaza el modelo A |
| D-16 sin tope monetario inventado | **Se conserva** como principio en §6 |

### Trabajo que se desprende (plan de correcciones)

- **F2:** F2.M1–M3 (retirar el respaldo en código, consumidor, esquema y cuentas 9.x), F2.M5 (aval), F2.M9
  (fondo mutual viejo).
- **F3:** H3.S3 (modelo A: M1 a M8), H3.S7 (pieza D), H3.S4.M3 (orden por nivel y sorteo), H3.S8 (parámetros en
  el backoffice, comisión y congelamiento por grupo).
- **Riesgo:** si Cumplimiento clasifica el fondo segregado como captación (L-01), el modelo A se rediseña. Por eso
  F3.S3.M7 (opinión de Cumplimiento) va antes de implementar H3.S3; F1 y F2 no dependen de eso.

### Consecuencias financieras (ejemplo sintético)

Grupo de 10, cuota Bs 500, aporte al fondo del 2 % (porcentaje ilustrativo, **no** es un parámetro propuesto):
cada participante aporta Bs 10 por periodo al fondo. Un participante deja de pagar dos cuotas y, suponiendo que
el fondo tiene saldo cuando vence cada una, el fondo las cubre (Bs 1.000): el pozo se entrega completo y el moroso
queda debiendo Bs 1.000 al fondo. Si al cierre recuperó Bs 400, lo incobrable son Bs 600. Cada uno de los nueve
participantes puntuales aportó Bs 100 al fondo en el ciclo; si el reparto en partes iguales fuera entre esos nueve,
a seis les tocarían Bs 66,67 y a tres Bs 66,66 (Bs 600,00 = 9 × 66,66 + 0,06; los seis centavos de residuo se
asignan según §4), y cada uno recuperaría Bs 33,33 o Bs 33,34. Entre quiénes se reparte exactamente y qué pasa con la devolución del propio moroso son
`DECISION_REQUIRED` 6. La empresa no movió un centavo en ningún momento.

## Cómo se verifica

- Este documento: `python3 scripts/verificar_boveda.py` (indexado, seis secciones, estado válido) y
  `grep -n "^estado" ADR-051` → `superada por ADR-056`.
- El modelo: las microtareas de H3.S3 traen sus asientos pegados, la prueba de concurrencia de la cobertura, la
  prueba de reversa del recargo, la prueba de redondeo con divisiones inexactas y la consulta de cuadre del fondo
  en cero al cierre (regla 91.6). El kill-test del plan: ninguna fila nueva de `movimiento_contable` contra las
  cuentas 9.x.

## DECISION_REQUIRED

No se resuelven acá porque DECISIONES no las cubre.

1. **Plan de cuentas:** nombres, códigos y naturaleza de las cuentas del modelo A (lo valida el contador).
2. **Quién es «el perjudicado» cuando el fondo cubrió a tiempo y nadie esperó**, o cuando son varios: P-05 dice
   «quien tuvo que esperar». ¿Se acredita al beneficiario del turno afectado, se reparte, o no hay recargo si nadie
   esperó? (Pablo).
3. **«Grupo perfecto»:** el plan dice que el fondo se devuelve antes. ¿Antes de qué hito exactamente? (Pablo).
4. **Valores** de todos los parámetros de §6 (Pablo y Cumplimiento, plan F3.S8.M3).
5. **Validaciones legales** L-01 (fondo segregado y captación), L-02 (cobertura y subrogación sin expediente
   previo) y L-03 (recargo al perjudicado; tasa o tope legal).
6. **Reparto de lo incobrable:** «en partes iguales», ¿entre todos los participantes o solo entre quienes no deben
   nada? ¿La devolución del aporte al fondo del propio moroso se compensa primero con su deuda? (Pablo).

## Referencias

- [`DECISIONES.md`](../trabajo/2026-11-20-auditoria-consolidado/DECISIONES.md) ·
  [`CONSOLIDADO.md`](../trabajo/2026-11-20-auditoria-consolidado/CONSOLIDADO.md) §6 ·
  [plan de correcciones](../trabajo/2026-11-21-plan-correcciones/PLAN.md) (H2, H3.S3, H3.S4, H3.S7, H3.S8) ·
  [`PREGUNTAS-LEGALES.md`](../trabajo/2026-11-21-plan-correcciones/PREGUNTAS-LEGALES.md).
- [[ADR-051 Garantía del pozo, comisión y autogestión|ADR-051]] (superada) ·
  [[ADR-055 Sin aprobaciones en el circuito|ADR-055]] · [[ADR-054 Grupos sin organizador humano|ADR-054]].
- Reglas 91 (dinero) y 98 (microservicios).
