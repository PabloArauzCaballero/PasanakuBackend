---
name: alertas-riesgo-temprano
description: "Anticipar el incumplimiento en AportaYa sin castigar por pronóstico: métricas de grupo con umbral, score de riesgo con factores guardados, alertas tempranas que disparan acompañamiento, mensajes en hechos y nunca en probabilidades, y calibración del modelo contra desenlaces reales. Úsala al tocar scoring, métricas de grupo, alertas tempranas o cualquier cosa que estime qué va a pasar."
---

# Alertas tempranas: ver el problema antes

El objetivo es **ofrecer ayuda antes del incumplimiento**, no adelantar el castigo.
Toda la skill se apoya en una línea:

> **Una alerta temprana nunca restringe por sí sola.** Restringir exige causa
> consumada ([[CU-27 Restringir al deudor e incluirlo en la lista interna]]), no
> pronóstico.

## Las tres capas

| Tabla | Ámbito | Qué mide |
| --- | --- | --- |
| [[metrica_grupo]] | grupo y período | pago en término, atraso promedio, brecha de bolsa, rotación |
| [[score_riesgo_incumplimiento]] | usuario, opcionalmente por grupo | probabilidad y **factores principales** |
| [[alerta_temprana]] / [[alerta_riesgo]] | usuario · grupo · cartera | el hecho de que algo cruzó su umbral |

## Sin datos no se pronostica

`SIN_DATOS` **no es riesgo alto**. Un usuario nuevo no tiene historial, y tratarlo
como riesgoso por eso excluye a todo el mercado que se quiere atender. Es la misma
regla que en `emparejamiento-ingreso`, y se rompe con la misma facilidad.

## Los factores se guardan

`factores_principales` en JSON, siempre. Un puntaje sin factores no se puede
discutir, no se puede corregir y no se puede explicar a un supervisor. `R-REP-03`
tiene el mismo espíritu para reputación: el total es la suma de sus componentes.

## Cómo se le habla al usuario

```
mal   «Detectamos un riesgo alto de incumplimiento en tu perfil»
bien  «Te venció el aporte del 5 por Bs 500 y todavía no figura pagado.
       Podés pagarlo acá, o pedir un plan.»
```

Decirle a alguien que el sistema cree que va a incumplir es una profecía, no una
ayuda. **El mensaje va en hechos: qué vence, cuánto, y qué opciones tiene.** El
puntaje no se muestra nunca, ni siquiera al propio interesado.

Al organizador sí se le muestra la salud del grupo con sus métricas y umbrales: es
información operativa sobre su cartera, no sobre una persona.

## Qué dispara cada severidad

| Severidad | Acción |
| --- | --- |
| Baja | refuerzo de recordatorios ([[CU-81 Programar recordatorios de aporte]]) |
| Media | contacto con oferta de [[plan_regularizacion]] o [[promesa_pago]] |
| Alta | aviso al organizador con el detalle de su grupo |
| Crítica | preparación del [[plan_contingencia]] del grupo |

Una alerta alta sobre alguien **que está al día** se revisa antes de contactar:
molestar a quien cumple daña la relación y ensucia el modelo.

## Agrupar

Muchas alertas del mismo grupo se consolidan en **una** alerta de grupo. El
organizador recibe una conversación, no veinte avisos. La misma lógica que el tope
de mensajes de `notificaciones-consentimiento`.

## Cerrar con desenlace

`R-GAR-07`: una alerta no se cierra sin resultado. Los dos desenlaces son "la causa
desapareció" (pagó, la métrica volvió al rango) o "se materializó" (incumplimiento
declarado).

**Ese desenlace es lo que calibra el modelo.** Cuántas alertas terminaron en
incumplimiento y cuántas no es la única forma de saber si el modelo sirve.

> Un modelo que nunca se contrasta con lo que pasó es una superstición con decimales.

## Versionado

`version_modelo` en cada score. Cuando el modelo cambia, las decisiones tomadas con
la versión vieja quedan trazadas con la suya. Si el modelo empieza a fallar
sistemáticamente: se congela, se recalibra y se documenta.

## Qué no hacer

- No usar el score para negar servicio.
- No mostrar la probabilidad al usuario.
- No tratar "sin datos" como "riesgo alto".
- No calcular el score al vuelo en cada consulta: se calcula por cierre de período y
  se guarda con su versión.
- No cerrar alertas en lote sin desenlace.
- No meter datos sensibles ni categorías protegidas entre los factores.

## Ver también

- [[CU-97 Anticipar el riesgo con alertas tempranas]] ·
  [[CU-25 Declarar el incumplimiento con descargo y evidencia]] ·
  [[CU-27 Restringir al deudor e incluirlo en la lista interna]] ·
  [[CU-98 Publicar el tablero de indicadores]]
- `R-GAR-07` · `R-REP-03` · `R-SEG-03` · `R-RIS-01` en [[Restricciones]]
- Skills: `garantia-mora-cobranza`, `motor-de-reglas`, `indicadores-tablero`,
  `reputacion-social`, `observabilidad`
