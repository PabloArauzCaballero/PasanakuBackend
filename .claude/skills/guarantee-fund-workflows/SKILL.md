---
name: guarantee-fund-workflows
description: Gate de diseño del fondo de garantía — capitalización y propiedad del fondo, política de cobertura con límites y exclusiones, activación de una cobertura y su asiento, subrogación de la deuda al fondo, aval solidario entre participantes y su ejecución, recuperación con abonos append-only, castigo de deuda incobrable, devolución de remanentes al disolver, y la solvencia del fondo como métrica vigilada. Usar al modelar o tocar fondo, cobertura, subrogación, aval, recuperación, castigo o devolución, y antes de cerrar cualquier cambio que haga que el fondo pague por alguien.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Fondo de garantía, cobertura y avales

El fondo es lo que hace que un participante cumplidor **cobre su turno aunque otro no pague**. Es
la promesa central del producto y, si se diseña mal, es también la forma más rápida de quedarse
sin plata: un fondo que cubre sin límite, sin recuperar y sin vigilar su solvencia se vacía en
un ciclo.

Servicio dueño: `garantia` (M8). Reglas duras: **91** (dinero) y **91.4** (debido proceso).

## 1. El fondo tiene dueño, origen y límite

- `fondo_garantia` **no es plata de la empresa ni del organizador**: su origen (aporte extra por
  cupo, porcentaje retenido, capital semilla) está declarado y es visible en transparencia.
- Todo movimiento va a `movimiento_fondo`, **append-only**. El saldo se calcula desde ahí y se
  concilia contra el mayor (`distributed-data-integrity`).
- `politica_cobertura` es **configurable y versionada**: límite por evento, límite por
  participante, límite por grupo, tope global, carencia inicial y exclusiones.
- **Prohibido cubrir sin verificar el límite y el saldo disponible**, en el servidor, dentro de
  la transacción. El clásico: dos coberturas simultáneas que leen el mismo saldo suficiente
  (`concurrency-and-locking`).

## 2. Activación de una cobertura

```
obligación vencida → expediente de incumplimiento → (firme o criterio automático declarado)
   → verificar política, límite y saldo → cobertura_incumplimiento → movimiento de fondo
   → asiento contable → subrogación → deuda_participante
```

Reglas:

1. **La cobertura no perdona la deuda: la traslada.** Quien no pagó le debe al fondo
   (`subrogacion` + `deuda_participante`). Si no se registra la subrogación, el fondo regala
   plata y nadie se entera hasta que no alcanza.
2. Idempotente por obligación: una obligación se cubre **una sola vez**
   (`money-movement-safety`).
3. Genera asientos: el fondo baja, la obligación se satisface, nace la cuenta por cobrar.
4. El participante cubierto y el grupo **se enteran**: la cobertura es un hecho público dentro
   del grupo (`transparency-reputation`), sin exponer datos de más
   (`data-privacy-financial`).
5. Si la cobertura es automática, el criterio está **en el reglamento**, no en el código, y no
   saltea el debido proceso para las consecuencias (sanción, reputación).

## 3. Aval solidario

`aval_participante` es alguien que se compromete a responder por otro.

- **Aceptación explícita, probada y versionada**: token firmado, texto exacto aceptado, fecha
  (`consent-management`). Un aval que la persona no aceptó de forma demostrable **no se ejecuta**.
- Alcance declarado: monto máximo, periodos cubiertos, vigencia.
- `ejecucion_aval` requiere: expediente **firme**, cobertura o deuda existente, notificación
  previa al avalista y su propio derecho a descargo. Ejecutar un aval sin avisar es la forma más
  rápida de perder dos participantes en vez de uno.
- El avalista que paga **se subroga** a su vez: pasa a ser acreedor del avalado.

## 4. Recuperación

- `abono_recuperacion` es **append-only**: cada pago hacia la deuda con el fondo queda registrado
  y nunca se edita.
- Orden de imputación declarado (intereses/cargos → capital, o el que fije el reglamento) y
  aplicado en un solo lugar. Si cada pantalla imputa distinto, los saldos no van a coincidir.
- `plan_regularizacion` y `promesa_pago` para acuerdos de pago; incumplir una promesa es un hecho
  registrable, no un borrón y cuenta nueva.
- `acuerdo_quita` (condonación parcial) **requiere acuerdo del grupo** y su asiento
  correspondiente: condonar es una pérdida contable, no un `UPDATE` que baja el saldo.

## 5. Castigo de deuda incobrable

`castigo_deuda` reconoce contablemente que no se va a cobrar. **No borra la deuda.**

- Requiere: criterio declarado (antigüedad, gestiones agotadas), autorización con doble control
  y asiento de pérdida.
- La deuda castigada **sigue visible** en el historial del participante
  (`historial_incumplimiento_usuario`) y sigue pesando en su reputación según el reglamento.
- Si después paga, se registra la recuperación de una deuda castigada; no se "deshace" el
  castigo.

## 6. Devolución y disolución

- `devolucion_fondo`: al cerrar o disolver un grupo, el remanente se devuelve según la regla del
  reglamento (proporcional a lo aportado, descontando deudas).
- El cálculo se hace **desde el mayor**, no desde un saldo materializado (regla 91.1.5).
- Cada devolución es un desembolso con todas las reglas de `disbursement-payouts`: cuenta
  verificada, enfriamiento, idempotencia.
- Un grupo no se cierra con el fondo sin conciliar (`financial-close-reporting`).

## 7. Solvencia: la métrica que evita el colapso

Vigilada y alertada, no revisada cuando alguien se acuerda:

| Indicador | Por qué |
|---|---|
| Saldo disponible / exposición máxima teórica | Si baja del umbral, el fondo no puede cumplir su promesa |
| Coberturas activas y su antigüedad | Plata prestada que no vuelve |
| Tasa de recuperación | Si tiende a cero, la cobertura es un subsidio |
| Deuda castigada acumulada | La pérdida real del modelo |
| Concentración: cuánto del fondo está comprometido con un solo participante o grupo | Riesgo de que uno lo vacíe |

Umbral superado ⇒ alerta con dueño y, si el reglamento lo prevé, **suspensión de nuevas
coberturas** antes de quedar en cero. Quedarse sin fondo en silencio es peor que no tener fondo.

## Anti-patrones

- Cubrir sin registrar la subrogación: el fondo regala plata.
- Cobertura sin verificar límite y saldo dentro de la transacción.
- Ejecutar un aval sin aceptación probada o sin avisar al avalista.
- Condonar con un `UPDATE` sobre el saldo de la deuda.
- Castigar la deuda y borrar el historial.
- Saldo del fondo materializado y nunca conciliado contra el mayor.
- Política de cobertura hardcodeada.
- Sin métrica de solvencia ni alerta.
- Doble cobertura de la misma obligación por falta de idempotencia.
- Cobertura automática que además dispara sanción sin plazo de descargo.

## Checklist

- [ ] Origen, propiedad y política del fondo declarados y versionados.
- [ ] Movimientos del fondo append-only; saldo calculado y conciliado contra el mayor.
- [ ] Cobertura idempotente por obligación, con límite y saldo verificados en transacción.
- [ ] Toda cobertura genera subrogación y deuda; nada se regala.
- [ ] Avales con aceptación probada, alcance y ejecución con expediente firme y notificación.
- [ ] Imputación de abonos definida en un solo lugar; abonos append-only.
- [ ] Quitas por acuerdo, con su asiento de pérdida.
- [ ] Castigo con criterio, doble control y asiento; la historia se conserva.
- [ ] Devoluciones calculadas desde el mayor y desembolsadas con las reglas de payout.
- [ ] Métricas de solvencia con umbral, alerta y dueño.

## Evidencia / DoD

1. Asientos de una cobertura: fondo, obligación y cuenta por cobrar, cuadrados.
2. Prueba de doble cobertura de la misma obligación: efecto único.
3. Prueba de cobertura por encima del límite: rechazada, con el motivo.
4. Registro de aceptación del aval, con versión del texto y fecha.
5. Salida del indicador de solvencia y del umbral configurado.
