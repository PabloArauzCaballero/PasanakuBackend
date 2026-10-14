---
name: transparency-reputation
description: Gate de diseño de la transparencia y la reputación — el panel del grupo calculado desde el mayor y no desde sumas ad-hoc, qué se muestra a quién según el reglamento aceptado, la reputación como consecuencia de hechos registrados y no como campo editable, score explicable con sus componentes, eventos de reputación append-only, certificados verificables, y los daños de exponer la mora de una persona sin base. Usar al construir el panel de transparencia, el score, un certificado, un badge, o cualquier pantalla que muestre el comportamiento de un participante a otros.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Transparencia y reputación

Lo único que un pasanaku vende es **confianza**, y la confianza se sostiene con dos cosas: que
las cuentas del grupo se puedan ver, y que el comportamiento pasado signifique algo. Las dos
tienen el mismo peligro: exponer de más, o exponer algo que no es cierto.

Servicio dueño: `confianza` (M6). Entidades: `bloque_transparencia`, `evento_reputacion`,
`componente_score`, `certificado_reputacion`.

## 1. El panel se calcula desde el mayor

- Cada cifra del panel sale de `asiento_contable` / `movimiento_contable`, **nunca** de un
  `SUM()` sobre la tabla de pagos ni de un contador materializado (regla 91.1.5).
- Si viene de un modelo de lectura, **dice a qué momento corresponde** y avisa si está atrasado
  (regla 98.4, `eventual-consistency-read-models`).
- Cada cifra se puede **abrir**: de un total a los movimientos que lo componen
  (`financial-close-reporting` §4). Un panel de transparencia que no se puede auditar es
  decoración.
- `bloque_transparencia` guarda la publicación con su momento y su contenido, para que nadie
  discuta después qué mostraba el panel la semana pasada.

## 2. Qué se muestra y a quién

Lo define el **reglamento aceptado** por ese grupo, no una decisión de producto:

| Dato | Visible para |
|---|---|
| Total aportado y total entregado del grupo | Todos los del grupo |
| Calendario, turnos y quién cobra cuándo | Todos los del grupo |
| Sorteo: commit, semilla y verificación | Todos, incluso público si el grupo es público |
| Estado agregado de cumplimiento (cuántos al día) | Todos los del grupo |
| **Mora de una persona identificada** | **Solo si el reglamento aceptado lo prevé**, con el mínimo dato |
| Deuda, expediente, score individual | El propio participante; los demás, nunca en detalle |
| Datos de contacto y documento | Nadie, salvo lo mínimo operativo |

Reglas:

- La decisión **se verifica en el servidor** y por grupo (`authz-access-control`). No alcanza con
  no pintar la columna.
- Estar en el mismo grupo no da derecho a ver el detalle financiero ajeno
  (`data-privacy-financial` §2).
- Exponer la mora de alguien que no aceptó ese reglamento es un daño real, con consecuencias
  legales y sociales. Se trata con el mismo cuidado que un desembolso.

## 3. La reputación es consecuencia, no registro

```
hecho (aporte puntual, mora, cobertura usada, expediente firme, entrega confirmada)
  → evento_reputacion (append-only, con su origen)
     → componente_score (qué aporta y cuánto)
        → score (calculado, nunca escrito a mano)
```

- **Prohibido un endpoint que setee el score.** Si hace falta corregirlo, se corrige el hecho o
  se emite un evento de reputación de corrección, con motivo y actor.
- Cada evento apunta al **registro que lo originó** (la obligación, el expediente, la entrega).
  Un evento de reputación sin origen rastreable no se puede defender ante un reclamo.
- Solo generan reputación negativa los hechos **firmes** (regla 91.4). Una mora en descargo no
  baja el score todavía.
- Revertida una sanción, **se emite el evento inverso** (`dispute-resolution` §6): el score se
  recompone por el mismo camino por el que bajó, y el historial muestra las dos cosas.

## 4. El score tiene que ser explicable

- `componente_score` guarda de qué salió cada punto: puntualidad, antigüedad, grupos
  completados, coberturas usadas, incumplimientos firmes.
- El participante **ve su propio desglose** y qué puede hacer para mejorarlo. Un número opaco
  que decide si puede entrar a un grupo y que nadie le puede explicar es indefendible.
- La fórmula está **versionada**: un cambio de fórmula no reescribe la historia, entra en
  vigencia con fecha y se puede comparar.
- El score **orienta**, no sanciona. No existe la sanción automática por score
  (`collections-delinquency` §6).
- Cuidado con los sesgos: un score que castiga ser nuevo condena a todo participante nuevo a no
  poder empezar. El diseño declara cómo entra alguien sin historial.

## 5. Certificados verificables

`certificado_reputacion` es lo que un participante muestra afuera ("completé 4 pasanakus sin
atrasos").

- **Verificable por un tercero** sin exponer más datos: identificador, firma o código, y una
  página de verificación que confirma lo mínimo.
- Con fecha de emisión y de validez: la reputación de hace dos años no dice lo mismo que la de
  hoy.
- Contiene **solo lo que el participante autorizó publicar** (`consent-management`).
- Revocable si el hecho que lo sostenía se revirtió.

## 6. Lo que no se hace

- **Rankings públicos de deudores.** Es una picota digital; el daño excede cualquier beneficio y
  expone a la empresa.
- Notificar al grupo la mora de alguien **antes** del plazo de descargo.
- Puntajes que se muestran a terceros sin consentimiento.
- Usar la reputación de Pasanaku para decisiones fuera de Pasanaku sin autorización expresa.

## Anti-patrones

- Panel calculado con `SUM()` sobre pagos.
- Cifra que no se puede abrir.
- Mostrar el panel con datos de una proyección atrasada sin decirlo.
- `UPDATE score SET valor = …`.
- Evento de reputación sin origen rastreable.
- Bajar el score por una mora que todavía está en descargo.
- Sanción revertida sin restaurar la reputación.
- Fórmula de score sin versionar.
- Score opaco que el usuario no puede ver ni entender.
- Exponer mora individual sin base en el reglamento aceptado.
- Certificado que revela más de lo autorizado.

## Checklist

- [ ] Todas las cifras del panel salen del mayor y se pueden abrir.
- [ ] Si vienen de una proyección, muestran su momento y avisan del desfase.
- [ ] Qué ve cada actor sale del reglamento aceptado y se verifica en el servidor.
- [ ] No se expone detalle financiero individual sin base reglamentaria.
- [ ] La reputación se genera por eventos append-only con origen rastreable.
- [ ] Ningún camino permite escribir el score directamente.
- [ ] Solo hechos firmes generan reputación negativa.
- [ ] Revertir una sanción emite el evento inverso.
- [ ] Score explicable, con desglose visible para el titular y fórmula versionada.
- [ ] Certificados verificables, con vigencia, mínimos y revocables.
- [ ] Sin rankings públicos de deudores.

## Evidencia / DoD

1. Una cifra del panel recorrida hasta sus asientos.
2. Salida del intento de ver el detalle financiero de otro participante: 403.
3. Historial de eventos de reputación de un caso, con su origen.
4. Prueba de reversión: sanción revocada y evento inverso emitido, con el score recompuesto.
5. Desglose del score tal como lo ve el participante.
