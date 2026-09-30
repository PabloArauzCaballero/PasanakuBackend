---
name: contribution-calendar
description: Gate de diseño del calendario del pasanaku — generación de periodos y turnos a partir del reglamento, fechas de vencimiento con días no hábiles y zona horaria, ventanas de gracia y cuándo nace la mora, un turno de cobro por periodo sin solapamientos, permutas que no rompen el calendario, recordatorios programados, y qué pasa cuando el grupo se atrasa. Usar al generar o tocar el calendario de un grupo, al calcular un vencimiento, al programar recordatorios, al mover un turno, o ante un problema de fechas, husos horarios o mora disparada un día antes.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Calendario de aportes y turnos

El calendario es donde el reglamento se vuelve fechas concretas, y las fechas concretas deciden
quién está en mora y quién cobra. Un error de un día acá genera una sanción injusta o una entrega
adelantada.

Servicio dueño: `grupos` (M2). Entidades: `periodo`, `turno`, `dia_no_habil`,
`solicitud_permuta`. El dinero que cuelga de esto, en `pagos` (`payments-qr-integration`).

## 1. El calendario se genera, no se escribe a mano

Entradas: reglamento aceptado (frecuencia, monto, día de corte, ventana de gracia), cantidad de
cupos, fecha de inicio y orden del sorteo revelado (`rosca-group-lifecycle` §3).

Salida: N periodos y N turnos, generados de una vez al pasar a `EN_CURSO`, **determinista**:
mismas entradas ⇒ mismo calendario. Si generarlo dos veces da distinto, tenés un bug que va a
aparecer en el peor momento.

Invariantes verificadas al generar:

- Cantidad de periodos = cantidad de cupos (salvo que el reglamento diga otra cosa, y entonces
  lo dice explícitamente).
- **Un turno de cobro por periodo**, y cada cupo cobra exactamente una vez.
- Sin solapamientos ni huecos en la secuencia de periodos.
- Toda obligación de aporte cuelga de un periodo existente.

## 2. Fechas: zona horaria, no "la fecha del servidor"

- Todo instante se guarda en **`TIMESTAMPTZ`** (UTC). La zona del grupo se guarda aparte.
- Un vencimiento es **"fin del día en la zona del grupo"**, no un instante UTC arbitrario. En
  Bolivia (UTC−4), "vence el 10" son las 04:00Z del 11.
- **Prohibido `new Date()` para decidir si algo venció** sin pasar por la zona del grupo. Es el
  bug que marca mora un día antes y sanciona a alguien que pagó a tiempo.
- El cálculo de vencimiento vive **en un solo lugar**, testeado con casos de borde: fin de mes,
  año bisiesto, día 31 en meses de 30, y el cambio de hora si alguna zona lo aplica.

```ts
// ❌ el vencimiento depende de dónde corre el proceso
if (new Date() > obligacion.venceEn) marcarMora();

// ✅ una función, con la zona del grupo, testeada
if (estaVencida(obligacion, grupo.zonaHoraria, ahora())) marcarMora();
```

## 3. Días no hábiles y corrimiento

`dia_no_habil` es un catálogo (`terminology-value-sets`), no una lista en el código.

- La política de corrimiento se declara: **siguiente hábil**, **anterior hábil** o **sin
  corrimiento**. Sale del reglamento.
- Aplica al **vencimiento**, y por separado se decide si aplica al **desembolso** (que depende
  del banco, no del grupo).
- Un feriado agregado después de generar el calendario **no mueve vencimientos pasados**. Si
  mueve los futuros, es un cambio de calendario y se notifica.

## 4. Ventana de gracia y nacimiento de la mora

```
vencimiento ──[ventana de gracia]──▶ MORA ──[escalones]──▶ INCUMPLIMIENTO
```

- La ventana de gracia sale del reglamento aceptado; **nunca** de una constante.
- La mora la declara un **job idempotente** que corre una vez por día por grupo
  (`background-jobs-scheduling`), no un `if` en cada lectura: si nace de una lectura, el estado
  depende de quién miró y cuándo.
- El job es **reejecutable sin efectos dobles**: correrlo dos veces el mismo día no duplica
  cargos ni eventos (`money-movement-safety` §2).
- Pasar a mora dispara notificación y cobranza escalonada (`collections-delinquency`); pasar a
  incumplimiento abre **expediente con debido proceso** (regla 91.4), nunca automáticamente
  sanción.

## 5. Permutas y cambios de turno

- Una permuta es un **acuerdo** (`rosca-group-lifecycle` §4), no una acción del organizador.
- Al aprobarse: se intercambian los turnos, **no los cupos ni las obligaciones ya generadas**.
- Queda registrada con quién, cuándo y el acuerdo que la autorizó; el panel de transparencia la
  muestra (`transparency-reputation`). Un cambio invisible en el orden de cobro destruye la
  confianza aunque sea legítimo.
- **Prohibido permutar un turno ya cobrado o en curso de entrega.**

## 6. Recordatorios

- Se programan desde el calendario: N días antes, el día del vencimiento, y los escalones de
  mora (`notifications-delivery`).
- El job de recordatorios es **idempotente por (obligación, tipo, fecha)**: un reintento no
  manda dos WhatsApp. Un participante que recibe el mismo recordatorio tres veces deja de leer
  los recordatorios.
- Se cancelan al pagar. Un recordatorio de algo ya pagado es peor que ninguno.
- El recordatorio lleva el enlace de pago rápido con token de un solo uso
  (`payments-qr-integration` §3).

## 7. Cuando el grupo se atrasa

El calendario ideal supone que todos pagan. Hay que decidir y declarar:

| Situación | Decisión que hay que tomar (y escribir en el reglamento) |
|---|---|
| Falta plata para la entrega del periodo | ¿Cubre el fondo (`guarantee-fund-workflows`), se prorratea, o se posterga? |
| Se postergó una entrega | ¿Se corre todo el calendario o solo ese turno? |
| Un cupo queda vacante a mitad | ¿Se reemplaza (`traspaso_cupo`) o el grupo sigue con menos? |

**Prohibido resolver esto en el código sin que esté en el reglamento.** Es dinero de terceros:
la regla la pone el contrato, no el desarrollador (`anti-hallucination-guard`).

## Anti-patrones

- Calendario cargado a mano o "ajustado" con `UPDATE`.
- `new Date()` comparado directo contra un vencimiento.
- Vencimiento como `DATE` sin zona: mora un día antes para media población.
- Feriados hardcodeados.
- Mora calculada al leer, distinta según quién consulta.
- Job de mora no idempotente.
- Permuta ejecutada por el organizador.
- Recordatorio duplicado, o mandado después de pagar.
- Decidir en el código qué pasa si falta plata.

## Checklist

- [ ] Calendario generado de forma determinista desde el reglamento y el sorteo.
- [ ] Un turno por periodo; cada cupo cobra una vez; sin huecos ni solapamientos.
- [ ] Vencimientos calculados en un solo lugar, con la zona del grupo, con tests de borde.
- [ ] Días no hábiles en catálogo, con política de corrimiento declarada.
- [ ] Ventana de gracia del reglamento; mora declarada por job idempotente.
- [ ] Incumplimiento abre expediente, no sanción automática.
- [ ] Permutas por acuerdo, visibles en transparencia, no sobre turnos ya cobrados.
- [ ] Recordatorios idempotentes y cancelados al pagar.
- [ ] Qué pasa si falta plata está en el reglamento, no en el código.

## Evidencia / DoD

1. Calendario generado dos veces con las mismas entradas: salida idéntica.
2. Tests de vencimiento en los bordes (fin de mes, feriado, zona horaria), con su salida.
3. Job de mora ejecutado dos veces: sin efectos duplicados, con la consulta pegada.
4. Salida del intento de permutar un turno ya cobrado: rechazado.
5. Registro de un recordatorio cancelado tras el pago.
