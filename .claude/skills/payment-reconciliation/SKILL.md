---
name: payment-reconciliation
description: Conciliación bancaria y con la pasarela — importar extractos de forma idempotente, emparejar movimientos con pagos por referencia y por reglas con tolerancia, clasificar y gestionar excepciones (sobrante, faltante, duplicado, no identificado, comisión), partidas conciliatorias en tránsito, la conciliación como proceso diario con dueño, y por qué un emparejamiento a mano sin registro es peor que una diferencia abierta. Usar al construir o tocar la importación de extractos, el motor de emparejamiento, la gestión de excepciones o el cierre de conciliación, y al investigar un pago que figura en el banco y no en el sistema, o al revés.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Conciliación bancaria y con la pasarela

Conciliar es responder una pregunta simple: **¿lo que dice el banco coincide con lo que dice el
sistema?** Si nadie la contesta todos los días, la respuesta se descubre en un reclamo, y para
entonces reconstruir tres semanas de movimientos cuesta días.

Distinta de la conciliación **interna** entre servicios (`distributed-data-integrity`): esta
compara contra un tercero.

Servicio dueño: `pagos` (M3). Entidades: `extracto_bancario`, `movimiento_bancario`,
`conciliacion`, `excepcion_conciliacion`.

## 1. Importar el extracto: idempotente y crudo

1. El archivo o la respuesta de la API se guarda **tal cual** (`extracto_bancario`), con hash,
   origen, periodo y quién lo importó. Sin el crudo no podés reprocesar cuando encuentres un bug
   en el parser.
2. **Idempotencia por (cuenta, periodo, hash)**: importar dos veces el mismo extracto no duplica
   movimientos. Esto pasa siempre: alguien reimporta "por las dudas".
3. Cada `movimiento_bancario` lleva su clave natural del banco (id de transacción, o la
   combinación que el banco garantice única). Si el banco no da nada único, se declara la clave
   sustituta y su riesgo.
4. Los importes se parsean a decimal exacto con su moneda (`money-movement-safety` §1). Un
   parser que usa `parseFloat` sobre un CSV bancario es un bug de dinero esperando.
5. Cambio de formato del banco ⇒ versión del parser y reproceso posible desde el crudo.

## 2. Emparejar

Por niveles, del más confiable al menos:

| Nivel | Criterio | Confianza |
|---|---|---|
| 1 | Referencia única del cobro presente en el movimiento | Automático |
| 2 | Id de la pasarela + monto exacto + fecha dentro de ventana | Automático |
| 3 | Monto exacto + fecha + cuenta origen conocida | Automático con marca de regla |
| 4 | Monto con tolerancia (comisión conocida) + ventana | Sugerencia, requiere confirmación |
| 5 | Nada de lo anterior | Excepción |

- Cada emparejamiento guarda **con qué regla y con qué confianza** se hizo. Cuando algo salga
  mal, la pregunta va a ser "¿por qué se emparejaron estos dos?".
- **Un movimiento se empareja con un solo pago y viceversa.** Unicidad en base, no en el código.
- La tolerancia se declara y se justifica (comisión del proveedor, redondeo del banco). Una
  tolerancia abierta empareja cosas que no van juntas y esconde faltantes.
- El emparejamiento automático corre en un job idempotente; reejecutarlo no rompe lo ya
  conciliado.

## 3. Excepciones: clasificadas, no "pendientes"

| Tipo | Qué es | Acción |
|---|---|---|
| **No identificado** | Entró plata que no corresponde a ninguna orden | Investigar; puede ser un pago sin referencia. **No acreditar a ojo** |
| **Faltante** | El sistema tiene un pago que el banco no muestra | ¿En tránsito, o acreditamos algo que no entró? |
| **Sobrante** | El monto del banco es mayor al esperado | Reembolso o aplicación, según reglamento |
| **Parcial** | Monto menor | La obligación **no** se cumple; queda saldo |
| **Duplicado** | Dos movimientos para un cobro | Reembolso; revisar idempotencia aguas arriba |
| **Comisión** | Diferencia por costo del proveedor | Asiento a cuenta de comisiones, no "ajuste" |
| **Reverso del banco** | El banco revirtió | Reversa contable y expediente |

Cada excepción tiene **dueño, antigüedad y estado**. Una excepción de más de N días es una
alerta: es plata cuyo destino nadie sabe.

> [!warning] Acreditar un "no identificado" porque el monto coincide con lo que alguien dice que
> pagó es el atajo que rompe la conciliación. Si no hay referencia, se investiga o se devuelve.

## 4. Partidas en tránsito

No toda diferencia es un error:

- Depósito hecho el día 30 que el banco acredita el 1.
- Desembolso ordenado y no impactado todavía.
- Comisiones que el banco cobra al cierre de mes.

Se registran como **partidas conciliatorias con fecha esperada de resolución**, y se vigila que
se resuelvan. Una partida en tránsito de dos meses ya no está en tránsito: está perdida.

## 5. El proceso diario

1. Importar extracto y movimientos de la pasarela.
2. Emparejar automático.
3. Revisar sugerencias (nivel 4) — una persona, con registro de quién confirmó.
4. Clasificar y asignar excepciones nuevas.
5. Cerrar la conciliación del día: saldo banco = saldo mayor ± partidas en tránsito.
6. Si no cuadra, **el día queda abierto** con la diferencia registrada
   (`financial-close-reporting`).

Automatizado y con dueño. "Cuando tengamos tiempo" no es una frecuencia.

## 6. Prohibido el ajuste a mano

- **Nada de `UPDATE` sobre el mayor** para hacer cuadrar (regla 91.1.3). Toda corrección es un
  asiento con motivo y actor.
- Un emparejamiento manual queda auditado: quién, cuándo, con qué evidencia y por qué.
- Si la diferencia es "chica", igual se registra: las diferencias chicas y sistemáticas son la
  firma de un bug de redondeo (`distributed-data-integrity` §3).

## 7. Métricas

| Métrica | Alerta |
|---|---|
| % conciliado automático | Si baja, algo cambió en el formato o en las referencias |
| Excepciones abiertas y su antigüedad | Cualquier excepción vieja |
| Monto total en excepción | Es plata sin dueño conocido |
| Días con conciliación cerrada | Cualquier día abierto |
| Tiempo desde el último extracto importado | Importación caída |

## Anti-patrones

- Importar sin guardar el crudo.
- Reimportar y duplicar movimientos.
- `parseFloat` sobre importes del extracto.
- Emparejar por monto y fecha sin referencia, automáticamente.
- Tolerancia amplia para "que cuadre".
- Excepciones en una bandeja "pendientes" sin dueño ni antigüedad.
- Acreditar un no identificado por coincidencia de monto.
- Ajustar el mayor para cerrar el día.
- Conciliar una vez por mes.
- Un movimiento emparejado con dos pagos.

## Checklist

- [ ] Extracto crudo guardado con hash, origen y quién importó.
- [ ] Importación idempotente por (cuenta, periodo, hash).
- [ ] Importes parseados a decimal exacto con moneda.
- [ ] Reglas de emparejamiento por nivel, con confianza y regla registradas.
- [ ] Unicidad movimiento ↔ pago impuesta en base.
- [ ] Tolerancias declaradas y justificadas.
- [ ] Excepciones clasificadas, con dueño, estado y antigüedad; alerta por vencidas.
- [ ] Partidas en tránsito con fecha esperada y seguimiento.
- [ ] Conciliación diaria automatizada; el día no cuadra ⇒ queda abierto.
- [ ] Toda corrección por asiento; emparejamientos manuales auditados.

## Evidencia / DoD

1. Salida de la importación del mismo extracto dos veces: sin duplicados.
2. Salida del emparejamiento automático con su tasa y las reglas aplicadas.
3. Listado de excepciones abiertas con tipo, monto, antigüedad y dueño.
4. Cierre de conciliación del día: saldo banco vs saldo mayor, con las partidas en tránsito.
5. Para una diferencia resuelta: el asiento de corrección, no un `UPDATE`.
