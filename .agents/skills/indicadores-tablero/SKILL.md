---
name: indicadores-tablero
description: "Medir AportaYa sin discutir de dónde salió el número: definición versionada por indicador, metas fijadas antes del período, cálculo sobre la réplica de lectura, provisorio marcado como tal, supresión por mínimo de casos, dueño por familia y reproducibilidad. Úsala al crear un KPI, al armar un tablero, al preparar una sesión de comité, o cuando dos áreas reporten cifras distintas de lo mismo."
---

# Indicadores y tablero

El problema que resuelve esta skill no es técnico: es que dos personas lleguen a una
reunión con números distintos de la misma cosa.

> **Un indicador es una definición, no una consulta.** Si dos lugares del sistema lo
> recalculan, ya hay dos indicadores.

## Reglas duras

1. **Sin período cuadrado no hay indicador definitivo.** Un número sobre datos sin
   cuadrar es una opinión. Se puede publicar, pero **marcado como provisorio**
   (`R-BIL-12`, `R-AUD-07`).
2. **La meta se fija antes del período.** `SIN_META_DEL_PERIODO` bloquea el semáforo.
   Poner la meta después de ver el resultado no es medir.
3. **Se calcula en la réplica de lectura.** Un tablero nunca compite con el camino
   del dinero (skill `lecturas-proyecciones`).
4. **Reproducible**: cada [[indicador_kpi]] guarda con qué definición y qué período
   se calculó. Un número de hace un año tiene que volver a salir igual.
5. **Un solo lugar de cálculo.** [[CU-92 Evaluar el desempeño del organizador]] y el
   tablero leen los mismos indicadores; no los recalculan cada uno.

## Familias y dueños

Cada familia tiene **un dueño con nombre**, y ese dueño escribe la explicación
cuando su indicador está en rojo:

| Familia | Ejemplos |
| --- | --- |
| Negocio | grupos activos, participantes, volumen aportado, entregas |
| Riesgo | morosidad, coberturas consumidas, alertas abiertas |
| Cumplimiento | reportes en plazo, alertas sin conclusión, cobertura de capacitación |
| Operación | cierres cuadrados, incidencias con SLA vencido, disponibilidad |
| Finanzas | ingresos devengados y cobrados, encaje, costo por proveedor |

**Un indicador que no cumple su meta no se maquilla**: se muestra en rojo con su
variación y queda pendiente la explicación del dueño para la sesión de comité.

## Privacidad por agregación

```ts
suprimirPorMinimo(valor, casos, minimo)   // casos < minimo → null + leyenda
```

Un promedio de tres personas identifica a las tres. Por debajo del mínimo, el valor
**no se muestra** y se dice por qué (`R-SEG-03`). Esto aplica sobre todo a los
indicadores de dimensión `GRUPO` y `ORGANIZADOR`.

## Cambios de definición

Se versiona y se recalcula la serie con la nueva definición, **manteniendo también
la vieja**. El corte se señala en el gráfico.

> Los saltos de serie se explican, no se ocultan. Un tablero donde una métrica mejoró
> un 40 % porque cambió la fórmula es peor que no tener tablero.

## Variación e indicadores nuevos

- Indicador nuevo sin historia: se muestra **sin variación**, no con cero. No hay
  comparación posible todavía y fingir que sí desinforma.
- `variacion(actual, anterior)` maneja explícitamente el cero y la ausencia; es una
  función pura con pruebas propias, porque dividir por cero en un tablero es el bug
  más tonto y más frecuente.

## Provisorio vs definitivo

| Estado | Cuándo | Cómo se muestra |
| --- | --- | --- |
| Provisorio | período abierto | con la marca, siempre |
| Definitivo | período cerrado y cuadrado | sin marca |
| Corregido | dato corregido tras publicar | se recalcula y **queda registrado que se corrigió**, con fecha |

## Pedidos a medida

No van al tablero: van a [[CU-58 Definir, programar y exportar un reporte]]. El
tablero se llena de excepciones si se acepta cada pedido puntual, y entonces deja de
ser el lugar donde todos miran lo mismo.

## Qué no hacer

- No publicar un provisorio sin marcarlo.
- No calcular un indicador en el frontend.
- No exponer valores con muestra por debajo del mínimo.
- No borrar la serie anterior al cambiar una definición.
- No poner metas retroactivas.
- No usar el tablero para mostrarle a un participante la salud de su grupo: eso es
  [[CU-97 Anticipar el riesgo con alertas tempranas]], con otro lenguaje y otro
  destinatario.

## Ver también

- [[CU-98 Publicar el tablero de indicadores]] · [[CU-92 Evaluar el desempeño del organizador]] ·
  [[CU-51 Ejecutar el cierre diario]] · [[CU-94 Elevar una decisión al comité de gobierno]]
- `R-SEG-03` · `R-AUD-07` · `R-BIL-12` en [[Restricciones]]
- Skills: `lecturas-proyecciones`, `observabilidad`, `alertas-riesgo-temprano`,
  `organizador-habilitacion`, `gobierno-comites`
