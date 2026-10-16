---
name: collections-delinquency
description: Gate de diseño de mora, cobranza y sanciones — el incumplimiento como expediente y no como bandera, escalones de cobranza con su estrategia configurable, promesas de pago y planes de regularización, matriz de sanción por tipo × severidad × reincidencia, plazo de descargo y estado FIRME antes de ejecutar, apelación en dos instancias, alertas tempranas, y los límites éticos y legales de la cobranza. Usar al modelar o tocar mora, gestión de cobranza, incumplimiento, deuda, sanción, restricción de usuario o apelación, y antes de automatizar cualquier consecuencia negativa para un participante.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Mora, cobranza y sanciones

Todo lo de esta skill le hace daño a una persona real: le cobra, le restringe la cuenta, le baja
la reputación o le ejecuta un aval. Por eso el diseño empieza por el **debido proceso**, no por
la automatización. Un sistema que sanciona rápido y se equivoca destruye más valor que uno que
tarda un día más.

Servicio dueño: `garantia` (M8). Regla dura: **91.4**.

## 1. El incumplimiento es un expediente, no una bandera

```
registro_incumplimiento
  ├── evidencia_incumplimiento        (qué demuestra el hecho)
  ├── historial_estado_incumplimiento (append-only, la línea de tiempo)
  ├── descargo_participante           (su versión, con plazo)
  ├── gestion_cobranza → accion_cobranza, promesa_pago
  ├── cobertura / subrogación / deuda_participante
  ├── sancion → apelacion_sancion
  └── impacto en reputación (consecuencia, no registro)
```

Un booleano `es_moroso` no sirve: no guarda **por qué**, **desde cuándo**, **qué se hizo**, **qué
dijo la persona** ni **quién decidió**. Cuando el participante reclame —y va a reclamar— el
expediente es lo único que sostiene la decisión.

## 2. Estados y el punto FIRME

```
DETECTADO → NOTIFICADO → EN_DESCARGO → { RESUELTO_A_FAVOR | FIRME }
                                            │
                                   FIRME → EJECUTADO → { RECUPERADO | CASTIGADO }
                                            └─▶ APELADO → { REVOCADO | CONFIRMADO }
```

- **Nada con consecuencia patrimonial o reputacional ocurre antes de `FIRME`.** Ni ejecución de
  aval, ni sanción, ni baja de score.
- `FIRME` requiere: notificación efectiva registrada + plazo de descargo vencido o descargo
  resuelto. El plazo sale del reglamento aceptado (`rosca-group-lifecycle` §2).
- Las transiciones se validan en el servidor y quedan en el historial append-only
  (`state-machines-workflows`, `audit-trail-history`).
- **Prohibido un endpoint que marque `FIRME` directamente.**

## 3. Cobranza escalonada

`estrategia_cobranza` es **configurable y versionada**, no un `switch` en el código:

| Escalón | Momento | Acción | Canal |
|---|---|---|---|
| 0 | Antes del vencimiento | Recordatorio | Push / WhatsApp |
| 1 | Vencido, dentro de la gracia | Aviso amable con enlace de pago | WhatsApp |
| 2 | Vencida la gracia | Notificación formal + apertura de expediente | WhatsApp + correo |
| 3 | +N días | Oferta de plan de regularización | Contacto directo |
| 4 | +M días | Aviso de cobertura del fondo y subrogación | Formal |
| 5 | Firme | Ejecución: cobertura, aval, sanción | Formal |

- Cada `accion_cobranza` se registra: cuándo, por qué canal, con qué resultado. Sin registro no
  se puede demostrar que se notificó, y sin notificación demostrada no hay `FIRME`.
- El escalón **se detiene al pagar**, en todos los canales y de inmediato. Seguir cobrándole a
  alguien que ya pagó es el error que más reclamos genera.
- Idempotencia por (expediente, escalón): un reintento del job no manda dos veces
  (`notifications-delivery`).

## 4. Promesas y planes

- `promesa_pago`: fecha y monto comprometidos. Pausa el escalón hasta esa fecha.
- Promesa incumplida ⇒ se registra y **acelera** el escalón; no se borra ni se reemplaza en
  silencio. El historial de promesas incumplidas es información legítima para el grupo.
- `plan_regularizacion`: cuotas con su propio calendario y sus propias obligaciones. Cada cuota
  es una obligación real, con su cobro y su asiento, no una anotación.
- Un plan aceptado **no borra la mora pasada**: cambia la forma de pago, no la historia.

## 5. Sanciones proporcionales

Matriz declarada `tipo × severidad × reincidencia` (`terminology-value-sets`), no criterio ad hoc:

| | Primera | Segunda | Tercera |
|---|---|---|---|
| Atraso leve (dentro de gracia) | Sin sanción | Advertencia | Restricción de ingreso a grupos nuevos |
| Atraso con cobertura del fondo | Advertencia | Restricción temporal | Restricción prolongada |
| Abandono | Restricción prolongada | Exclusión | Exclusión |

- La sanción **es proporcional y tiene vencimiento**. Una restricción sin fecha de fin es una
  expulsión disfrazada.
- `restriccion_usuario` y `lista_restriccion_interna` se aplican en el servidor, en el momento
  de intentar la acción (entrar a un grupo, tomar otro cupo), no escondiendo botones.
- **Apelación en dos instancias**, con plazos, y la ruta existe en el código
  (`dispute-resolution`). Una apelación que solo está en el reglamento no es una apelación.

## 6. Alertas tempranas

Mejor prevenir que cobrar. `alerta_temprana` + `score_riesgo_incumplimiento`:

- Señales: atraso en el periodo anterior, pago sobre la hora repetido, promesa incumplida,
  primer grupo del participante, cupo múltiple recién tomado.
- Uso legítimo: ofrecer un plan **antes** del vencimiento, avisar al organizador de un riesgo
  del grupo, ajustar el escalón.
- Uso prohibido: **decidir una sanción con un score**. El score orienta la gestión; la sanción
  sale de hechos probados en el expediente.
- El score es explicable: `componente_score` guarda de qué salió cada punto. Un score que nadie
  puede explicarle al participante es indefendible.

## 7. Límites de la cobranza

No son opcionales ni "de negocio": son condición de corrección.

- **Horarios y frecuencia** de contacto acotados y configurables; nada de mensajes a cualquier
  hora ni varias veces por día.
- **Nunca exponer la deuda de alguien a terceros** que el reglamento no autorice. Publicarla en
  el grupo solo si el reglamento aceptado lo prevé, y con el mínimo dato
  (`data-privacy-financial`).
- **Prohibido contactar a los contactos personales** del deudor para presionar, salvo avalistas
  formalmente aceptados y por el canal previsto.
- **Prohibido el lenguaje intimidatorio** en plantillas. Se revisa como parte del código
  (`ux-writing-microcopy`).
- Todo contacto queda registrado y es auditable.

## Anti-patrones

- `es_moroso` booleano sin expediente.
- Sanción automática sin plazo de descargo.
- Marcar `FIRME` sin notificación demostrada.
- Escalón que sigue después de pagar.
- Estrategia de cobranza hardcodeada.
- Promesa incumplida que se borra al renegociar.
- Sanción sin vencimiento.
- Score que decide la sanción.
- Restricción aplicada solo ocultando botones en el frontend.
- Mensajes de cobranza intimidatorios o fuera de horario.
- Apelación que existe en el reglamento y no en el código.

## Checklist

- [ ] El incumplimiento se modela como expediente con evidencia e historial append-only.
- [ ] Nada con consecuencia ocurre antes de `FIRME`.
- [ ] `FIRME` exige notificación registrada y plazo de descargo cumplido.
- [ ] Estrategia y escalones configurables y versionados.
- [ ] Cada acción de cobranza queda registrada; el escalón se detiene al pagar.
- [ ] Jobs de cobranza idempotentes.
- [ ] Promesas y planes con obligaciones reales; el historial no se borra.
- [ ] Matriz de sanción declarada, proporcional y con vencimiento.
- [ ] Restricciones aplicadas en el servidor.
- [ ] Apelación en dos instancias implementada.
- [ ] Score explicable y sin poder decisorio sobre sanciones.
- [ ] Límites de horario, frecuencia, privacidad y lenguaje aplicados.

## Evidencia / DoD

1. Línea de tiempo de un expediente completo, desde detección hasta firme, pegada.
2. Salida del intento de sancionar sin plazo cumplido: rechazado.
3. Prueba de que el escalón se detiene al pagar, con el registro del corte.
4. Job de cobranza ejecutado dos veces: sin notificaciones duplicadas.
5. Salida de una restricción aplicada en el servidor ante el intento de la acción.
