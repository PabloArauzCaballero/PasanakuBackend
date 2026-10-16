---
name: emparejamiento-ingreso
description: "Cómo entra alguien a un grupo de AportaYa: postulación con puntaje explicable, criterio de emparejamiento con pesos versionados, propuestas de grupo que expiran, invitaciones con token de un solo uso, referencias personales verificadas y concentración de riesgo acotada. Úsala al tocar solicitud de ingreso, emparejamiento, invitaciones o el armado de grupos nuevos."
---

# Entrar a un grupo

Dos caminos, un mismo estándar de explicabilidad:

```
A) postulación a un grupo concreto  → solicitud_ingreso → organizador decide
B) emparejamiento sin grupo         → postulacion_emparejamiento → propuesta_grupo
                                       → aceptaciones → grupo materializado
C) invitación de un conocido        → invitacion (token único) → (A)
```

## El puntaje se explica o no sirve

`puntaje_compatibilidad` sale de [[criterio_emparejamiento]], cuyos **pesos son
datos con vigencia**: reputación, monto, geografía, historial compartido. Nunca
constantes en el código.

Y siempre se devuelve el **motivo legible** junto al número:

```ts
{ puntajeCompatibilidad: '0.82',
  motivoLegible: 'Monto compatible, gente de tu departamento y riesgo similar al tuyo' }
```

Al usuario se le muestra el motivo, no el número. Al organizador, ambos.

## Sin historial no es riesgo alto

La distinción más importante de esta skill:

| Situación | Nivel | Qué se ofrece |
| --- | --- | --- |
| Historial malo | `ALTO` | rechazo con el motivo y qué mejorar |
| **Sin historial** | `SIN_DATOS` | **grupos de monto bajo**, no exclusión |

Confundir "no sabemos" con "es riesgoso" excluye a todos los usuarios nuevos, que
son justamente el mercado. `SIN_DATOS` no es cero.

## Concentración de riesgo

`max_morosos_por_grupo` en el criterio vigente. Si el grupo llegó al tope, la
solicitud se rechaza **con motivo y alternativas**: proteger a los que ya están es
parte del servicio, no un filtro burocrático.

## Propuestas que expiran

[[propuesta_grupo]] tiene `expira_en`, `aceptaciones_recibidas` y su vínculo por
[[propuesta_postulacion]] (`acepto`, `respondido_en`).

- Expira sin las aceptaciones necesarias → se disuelve, los postulantes vuelven a la
  bolsa y se recompone con otro conjunto.
- Un postulante en dos propuestas: acepta **una**, y su aceptación libera las demás.
- Puede retirar la aceptación **hasta la materialización**; después es
  [[CU-65 Retirarse de un grupo]].
- **La propuesta abierta conserva el criterio con el que se armó** aunque el criterio
  cambie. El nuevo rige para las siguientes.

## Invitaciones

| Regla | Por qué |
| --- | --- |
| Token de un solo uso con vencimiento (`R-GRP-15`) | un enlace reenviado no es una llave permanente |
| El mensaje dice **quién** invita y a **qué** grupo, con monto y periodicidad | nadie acepta un compromiso de dinero sin saber cuál |
| **No revela datos de los integrantes** | quien invita ya conoce al invitado; el sistema no presenta a nadie |
| Tope de reenvíos (`envios_realizados`) | insistir tres veces es recordar; diez es acoso |
| Respeta [[lista_supresion]] (`R-NOT-03`) | y a quien invita se le dice "no fue posible", **sin el motivo** |
| Tope por destinatario y día (`R-NOT-02`), no por grupo | si no, tres grupos = tres veces el tope |

## Referencias personales

[[referencia_personal]] queda `verificada = false` hasta que **la referencia misma**
responda. Sin esa confirmación no cuenta como respaldo, y sobre todo:

> **Nadie queda de avalista por figurar en la agenda de otro.** Constituirse en
> [[aval_participante]] es un acto propio, con tope firmado.

Si la referencia pide no ser contactada: se elimina el vínculo y su teléfono va a
[[lista_supresion]].

## Antes de aceptar a alguien

1. Restricción `SIN_GRUPOS_NUEVOS` vigente → rechazo indicando **el monto que la
   levanta** (skill `garantia-mora-cobranza`).
2. KYC del nivel que el grupo exige (`R-UIF-09`).
3. Reputación mínima del reglamento.
4. Cupo libre, o lista de espera con puntaje.
5. [[aceptacion_reglamento]] **antes** de que el cupo quede firme.

## Qué no hacer

- No mostrar la identidad de los otros postulantes antes de materializar.
- No usar datos sensibles ni categorías protegidas como criterio.
- No dejar una solicitud pendiente sin resolución ni plazo (`R-GRP-14`).
- No permitir dos solicitudes pendientes del mismo usuario al mismo grupo.
- No convertir la invitación en un canal comercial masivo: se detecta y se limita.
- No rechazar a un usuario nuevo por falta de historial.

## Ver también

- [[CU-68 Postular a un grupo y ser emparejado]] ·
  [[CU-69 Invitar a un contacto y registrar sus referencias]] ·
  [[CU-20 Crear grupo y congelar tarifario]] · [[CU-66 Reemplazar a un participante moroso]]
- `R-GRP-14` · `R-GRP-15` · `R-UIF-09` · `R-NOT-02` · `R-NOT-03` en [[Restricciones]]
- Skills: `gobernanza-grupo`, `kyc-onboarding`, `garantia-mora-cobranza`,
  `notificaciones-consentimiento`, `reputacion-social`
