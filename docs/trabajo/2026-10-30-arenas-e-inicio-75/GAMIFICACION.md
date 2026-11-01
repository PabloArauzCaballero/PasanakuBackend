# Gamificación de arenas — ronda 2

Fuentes: skills `marketing-psychology`, `churn-prevention` y `onboarding` (repo
coreyhaines31/marketingskills, ya instaladas en `~/.claude/skills`), filtradas por las reglas
91 (dinero) y 91.4 (la reputación es consecuencia, no premio comprable).

## Filtro ético (no negociable)

| Se usa | Se descarta | Por qué |
|---|---|---|
| Progreso y metas cercanas | Recompensas por aportar **más** plata | Empuja a sobreendeudarse |
| Celebrar cumplir **a tiempo** | Rachas que castigan por un día | Presión que lleva a pedir prestado para no perder la racha |
| Prueba social con datos **reales** | Escasez inventada («quedan 2 cupos» falso) | Es mentir sobre un hecho |
| Aviso honesto de que la reputación baja | Culpa o miedo en los textos | Tono de cobranza, no de juego |
| Desbloqueos que son control de riesgo | Comprar arena o saltar de nivel | La arena tiene que reflejar el comportamiento real |
| — | Mostrar un saldo en cero como logro | Memoria `feedback_animaciones_sobre_dinero` |

## Mecánicas

| # | Mecánica | Principio | Dónde | Datos que usa |
|---|---|---|---|---|
| G1 | **Arranque con progreso**: quien recién llega ve Semilla con el primer paso ya marcado («Verificaste tu identidad ✓») | Endowed progress (onboarding) | Camino de arenas | estado KYC existente |
| G2 | **Próxima meta concreta**: «Te faltan 25 puntos para Brote: 2 aportes a tiempo más» | Goal-gradient | Barra de arena | puntaje + pesos del modelo (`peso_factor`), sin inventar equivalencias: si no se puede calcular, solo los puntos |
| G3 | **Lista de primeros pasos** (3 a 5): verificar identidad, unirte a tu primer grupo chico, firmar, primer aporte a tiempo, completar un grupo | Checklist + Zeigarnik | Inicio, hasta completarla | datos existentes; se puede cerrar |
| G4 | **Momento de subida de arena**: pantalla de celebración con la arena nueva y lo que desbloquea | Peak-end | Al detectar cambio de arena entre dos lecturas | `arena.orden` anterior y actual; respeta movimiento reducido |
| G5 | **Cierre de grupo como pico**: al completar un pasanaku, resumen «cumpliste los N aportes a tiempo» y puntos ganados | Peak-end + commitment | Detalle del grupo finalizado | eventos de reputación del grupo |
| G6 | **Aviso preventivo honesto**: «Tu aporte vence el viernes. Pagarlo a tiempo mantiene tu arena» | Loss aversion aplicada a algo real | Notificación + Inicio | calendario de aportes existente |
| G7 | **Tu arena es tu grupo de pares**: «En Feria hay N personas» y la arena mínima del grupo como sello | Unity + prueba social real | Camino de arenas, tarjeta del grupo | conteo agregado por arena (sin datos personales) |
| G8 | **Empezar chico**: la arena inicial sugiere grupos de 4 a 6 y ciclos cortos; los desbloqueos se muestran como «próximo nivel» | Foot-in-the-door + recomendación financiera | Crear grupo y unirse | catálogo de desbloqueos (solo `VIGENTE` se aplica) |
| G9 | **Recuperación después de bajar**: si alguien baja de arena, plan concreto para volver («2 aportes a tiempo te devuelven a Feria»), sin vergüenza | Churn prevention: rescate antes de perder al usuario | Camino de arenas | puntaje y tramos |
| G10 | **Invitar a pares**: compartir la invitación destaca que el grupo es de tu arena o superior | Loop de referidos + arena mínima | Invitar | arena mínima del grupo |

## Métricas para saber si funciona
- Activación: % que completa su primer grupo chico dentro de un ciclo.
- Puntualidad: % de aportes a tiempo en arenas bajas, antes y después.
- Retención: % que se une a un segundo grupo después de completar el primero.
- Riesgo: coberturas del fondo por cada 100 cupos en grupos de arena inicial. **Si sube, la gamificación está empujando mal.**

## Lo que exige confirmación
- G7 necesita un endpoint agregado por arena: contrato nuevo en transparencia.
- G2 «equivalente en aportes» depende de los pesos del modelo v1. Si no es exacto, se muestran solo puntos.
- Textos finales: revisión de `ux-writing-microcopy` con tono no punitivo.
