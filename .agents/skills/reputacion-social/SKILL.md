---
name: reputacion-social
description: "La capa social de la reputación de AportaYa: insignias con criterio publicado, reseñas entre participantes con convivencia comprobada y moderación, y certificados de reputación verificables que el titular controla. Úsala al tocar insignias, reseñas, certificados o cualquier cosa que exponga el historial de una persona hacia afuera."
---

# Reputación social: insignias, reseñas y certificados

`sorteo-transparencia` cubre el **puntaje**: cómo se calcula y por qué es explicable.
Esta skill cubre lo que se **muestra**, que es donde se hace daño.

## El principio

> La reputación es **del usuario**. Sale de la plataforma solo por decisión suya, y
> nunca en forma negativa.

Consecuencias que no se negocian:

- **No hay insignias negativas.** Lo negativo va al historial interno, que no es
  público.
- **No existe endpoint para consultar la reputación de un tercero.** Si alguien
  quiere acreditarla, emite su certificado. Es una decisión de diseño, no una
  limitación.
- **El comentario individual de una reseña no sale del grupo** ni entra al
  certificado.

## Insignias

| Regla | Detalle |
| --- | --- |
| Criterio **verificable con datos** | no hay insignias otorgadas a dedo; no hay endpoint de otorgamiento manual |
| Una por usuario (`R-REP-05`) | el reintento del outbox no duplica |
| Se notifica **con el motivo** | "la ganaste" sin decir por qué es una calcomanía |
| Cambio de criterio → se versiona | **quien ya la tiene la conserva** |
| Revocar escribe `revocada_en` y motivo | la fila no se borra, y se le explica al usuario |
| **No otorgan derechos económicos** | si algo debe abaratar, es [[segmento_comercial]] con su propio criterio |

Mezclar reconocimiento con tarifa vuelve opacos a los dos. Ver
[[CU-36 Segmentar comercialmente y aplicar precio diferenciado]].

En la interfaz, la parte que sirve es la **barra de progreso**: cuánto falta para la
siguiente, no la vitrina de las obtenidas.

## Reseñas

Dos precondiciones que la base hace cumplir (`R-REP-06`):

1. **Convivencia comprobada**: autor y evaluado compartieron grupo y período.
2. **Nadie se reseña a sí mismo.**

Y una que es de diseño, no de base:

> **No se reseña en medio del ciclo.** Reseñar a alguien que todavía te tiene que
> pagar presiona su comportamiento de pago; eso no es información, es coerción.

| Situación | Cómo se trata |
| --- | --- |
| Autor expulsado del grupo | puede reseñar, **pero pesa menos** y se marca la condición |
| Comentario con teléfono o documento | se retiene, se publica sin el dato y se avisa al autor |
| Muchas negativas del mismo grupo tras un conflicto | el conjunto pasa a revisión: una pelea grupal no es una evaluación |
| Reseña rechazada | **se explica el motivo**; moderar en silencio es censurar |
| Reseña que es en realidad un reclamo | se deriva a [[CU-52 Atender un reclamo en plazo]] |

El evaluado puede responder una vez y reportar. **No puede borrar.**

Peso en el modelo: **acotado**. La opinión pesa menos que el hecho de haber pagado.
Los datos duros mandan.

## Certificados

```
snapshot_reputacion (congelado) → certificado_reputacion → url_publica + codigo_verificacion
```

| Regla | Por qué |
| --- | --- |
| Se emite **desde el snapshot**, no de un cálculo al vuelo | dos emisiones del mismo snapshot dicen lo mismo |
| El titular **elige qué incluye** | lo que no elige, no aparece |
| **Tiene `expira_en`** | un certificado de reputación sin vencimiento miente al mes siguiente |
| Solo lo emite el titular (`R-SEG-03`) | no hay emisión por terceros |
| Verificación pública sin sesión | recomputa el hash y valida la firma |

La sutileza que evita una fuga: **un código inexistente y uno revocado devuelven lo
mismo**. Distinguirlos convertiría la verificación en un oráculo para averiguar si
alguien tiene cuenta.

Derecho de supresión ([[CU-07 Ejercer derechos sobre datos personales]]): los
certificados se revocan y la URL deja de resolver.

## Qué no hacer

- No exponer el puntaje de riesgo de nadie, ni al propio interesado como número
  suelto (ver `alertas-riesgo-temprano`).
- No crear una insignia por ausencia de algo: es estigma disfrazado.
- No dejar publicar una reseña sin moderación.
- No emitir certificados sin vencimiento.
- No usar el promedio de reseñas como si fuera un dato duro en decisiones de dinero.
- No borrar una insignia revocada ni una reseña moderada: se marcan.

## Ver también

- [[CU-74 Otorgar y revocar una insignia]] · [[CU-75 Emitir un certificado de reputación verificable]] ·
  [[CU-76 Reseñar a un participante y moderar la reseña]] · [[CU-71 Recalcular el puntaje de reputación]]
- `R-REP-05` · `R-REP-06` · `R-SEG-03` en [[Restricciones]]
- Skills: `sorteo-transparencia`, `emparejamiento-ingreso`, `reclamos-consumidor`,
  `alertas-riesgo-temprano`
