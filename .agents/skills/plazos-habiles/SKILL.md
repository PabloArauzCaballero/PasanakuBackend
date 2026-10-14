---
name: plazos-habiles
description: "Calcular y sostener plazos en AportaYa: calendario de días no hábiles por alcance, suma de días hábiles, plazos que se guardan al inicio y no se recalculan nunca, corrimiento a favor del cliente y control de vencimientos. Úsala cada vez que aparezca 'X días hábiles', al cargar feriados, al implementar un vencimiento legal, o cuando alguien proponga calcular la fecha límite al consultar."
---

# Plazos hábiles

Casi todos los plazos regulatorios de AportaYa se cuentan en **días hábiles**, y casi
todos los errores en esta área vienen de dos cosas: recalcular al consultar, y no
tener calendario.

## La regla que no se negocia

> **El plazo se calcula al inicio y se guarda** (`R-CON-01`). Jamás se recalcula al
> consultar.

Si el cálculo vive en una función que corre cada vez que alguien abre la pantalla, la
fecha límite de un reclamo puede cambiar entre dos consultas —por un feriado cargado
en el medio, por un cambio de zona horaria, por un bug— y el cliente ya vio la
anterior. Eso es peor que equivocarse una vez.

## Dónde se guarda cada plazo

| Plazo | Columna | Restricción |
| --- | --- | --- |
| Respuesta a reclamo | `reclamo_cliente.fecha_limite_respuesta` | `R-CON-01` |
| Prórroga de reclamo | `reclamo_cliente.fecha_limite_prorroga` | `R-CON-02` |
| Descargo de incumplimiento | `registro_incumplimiento.fecha_limite_subsanacion` | `R-GAR-01` |
| Remisión de reporte | `reporte_regulatorio.fecha_limite` | `R-UIF-05` |
| Respuesta a requerimiento | `requerimiento_autoridad.fecha_limite` | — |
| Reporte de incidente | `incidente_seguridad.fecha_limite_reporte` | `R-SEG-05` |
| Respuesta a disputa | `disputa_pago.fecha_limite_respuesta` | — |
| SLA de incidencia | `incidencia_entrega.fecha_limite_sla` | — |

Si estás agregando un plazo y no sabés en qué columna va, todavía no está diseñado.

## El calendario

[[dia_no_habil]] con `alcance`: `NACIONAL`, `DEPARTAMENTAL`, `PLATAFORMA` o `GRUPO`.
Único por fecha, alcance y grupo (`R-GRP-16`).

- **Un feriado sin fuente no se carga.** Decreto, resolución o calendario oficial, en
  la descripción.
- Alcance `GRUPO` exige `grupo_id`; `DEPARTAMENTAL` exige departamento. La base lo
  hace cumplir.
- **Sin calendario cargado, `sumarDiasHabiles` falla** (`CALENDARIO_VACIO`). No se
  cuentan días corridos "por defecto": eso produce plazos silenciosamente mal
  calculados, que es exactamente lo que se quiere evitar.
- Un control mensual avisa si el período siguiente está vacío. Un diciembre sin
  feriados cargados es un error de operación, no un año sin feriados.

## Feriado declarado después

```
plazo ya calculado y guardado  →  NO cambia
plazos que se abran desde ahora →  usan el calendario nuevo
```

Mover un plazo guardado sería reescribir el pasado, y para el cliente es peor: le
cambiamos la fecha que ya le habíamos dicho. La respuesta al usuario lo explica.

## Corrimiento a favor

Si el vencimiento cae en día no hábil **declarado antes del cálculo**, se corre al
**siguiente** hábil. Nunca al anterior. Y si eso mueve la fecha de un aporte, **se
notifica al grupo**: nadie debería enterarse de un cambio de fecha por su cuenta.

## Las funciones

```ts
sumarDiasHabiles(desde, dias, noHabiles) // pura, con pruebas de propiedad
siguienteHabil(fecha, noHabiles)         // pura
```

Casos de borde que **tienen que estar en las pruebas**: fin de año, feriados
consecutivos, plazo de un día, fecha de inicio que ya es no hábil, y cambio de mes.

Se devuelve además `diasSalteados` con la descripción de cada uno, para poder mostrar
*por qué* la fecha es esa, y `calendarioVersion`, para poder explicar el cálculo
meses después.

## Zona horaria

Todo en `TIMESTAMPTZ`, y el corte del día se evalúa en la zona de Bolivia. Un plazo
que vence "el viernes" vence al final del viernes en La Paz, no en UTC. Este detalle
ha costado plazos incumplidos en más de un sistema.

## Qué no hacer

- No calcular la fecha límite en el frontend.
- No usar `now()` en una consulta de listado para decidir si algo venció: se compara
  contra la columna guardada.
- No contar días corridos cuando la norma dice hábiles, ni al revés.
- No cargar un feriado sin fuente.
- No borrar un feriado cargado por error: se marca inactivo con motivo, porque hay
  plazos que se calcularon con él.

## Ver también

- [[CU-59 Mantener el calendario de días no hábiles]] · [[CU-52 Atender un reclamo en plazo]] ·
  [[CU-25 Declarar el incumplimiento con descargo y evidencia]] ·
  [[CU-43 Remitir los reportes mensuales a la UIF]]
- `R-CON-01` · `R-CON-02` · `R-GRP-16` · `R-GAR-01` · `R-UIF-05` en [[Restricciones]]
- Skills: `reclamos-consumidor`, `reportes-regulatorios`, `garantia-mora-cobranza`,
  `automatizacion-tareas`
