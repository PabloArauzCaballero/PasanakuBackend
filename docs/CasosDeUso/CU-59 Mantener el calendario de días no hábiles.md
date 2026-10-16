---
tags:
  - caso-uso
  - modulo/02-grupos-cupos-turnos-y-gobernanza
codigo: CU-59
criticidad: alta
actores: [Operaciones, Sistema, Grupo]
normas: [ASFI plazos en días hábiles, Consumidor Financiero, Ley 393]
---

# CU-59 — Mantener el calendario de días no hábiles

> **Objetivo.** Que "cinco días hábiles" signifique exactamente lo mismo para el
> sistema, para el cliente y para el supervisor — y que un feriado declarado la
> semana pasada no corra ningún plazo ya calculado.

## Actores y disparador

- **Actor principal:** operaciones.
- **Disparadores:** publicación del calendario anual de feriados; feriado
  departamental o decretado; día no laborable por decisión propia; feriado local que
  afecta a un grupo concreto.

## Precondiciones

1. La fuente del feriado es verificable: decreto, resolución o calendario oficial.
   **Un feriado sin fuente no se carga.**
2. Existe la política que define qué plazos se cuentan en días hábiles y cuáles en
   corridos.

## Flujo principal

1. Se crea [[dia_no_habil]] con `fecha`, `descripcion`, `alcance` —`NACIONAL`,
   `DEPARTAMENTAL`, `PLATAFORMA` o `GRUPO`— y `grupo_id` cuando el alcance es de un
   grupo. La combinación de fecha, alcance y grupo es única.
2. El cálculo de plazos hábiles consulta este calendario. Lo usan, entre otros:
   - el plazo de respuesta de reclamos ([[CU-52 Atender un reclamo en plazo]], `R-CON-01`);
   - el plazo de descargo de un incumplimiento
     ([[CU-25 Declarar el incumplimiento con descargo y evidencia]]);
   - el plazo de remisión de reportes ([[CU-43 Remitir los reportes mensuales a la UIF]]);
   - el vencimiento de aportes cuando el reglamento del grupo así lo pacta.
3. **Los plazos ya calculados no se recalculan** (`R-CON-01`). Si se declara un
   feriado después de haber fijado una fecha límite, esa fecha **no se mueve**; el
   calendario nuevo rige para los plazos que se abran desde su carga. Mover un plazo
   guardado sería reescribir el pasado, y para el cliente sería peor: le cambiamos
   la fecha que ya le habíamos dicho.
4. La excepción es a favor del cliente: si el vencimiento cae en día no hábil
   declarado **antes** del cálculo, se corre al siguiente hábil. Nunca al anterior.
   **El día se cuenta en la zona del contrato, no en UTC** (H7.S1.M3). Un instante en UTC
   puede caer en otro día que el que vive la persona: las 22:00 del 1 de mayo en La Paz son las
   02:00 del 2 de mayo en UTC. Por eso el cálculo del vencimiento muestra siempre **tres fechas**:
   la de UTC, la **contractual** (el día calendario en la zona del contrato, por omisión
   `America/La_Paz`) y la de **liquidación** (el primer día hábil desde la contractual). Hábil
   significa ni feriado del calendario, ni sábado, ni domingo (la misma regla de
   `PlazoHabil`; ver la decisión abierta sobre el sábado en la ficha de ambigüedades del
   carril O2).
5. La carga masiva del calendario anual valida que no haya duplicados y que ninguna
   fecha esté en el pasado, y deja constancia de la fuente en la descripción.
6. Un control mensual avisa si el calendario del período siguiente está vacío: un
   diciembre sin feriados cargados es un error de operación, no un año sin feriados.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 1a | La fecha ya existe con el mismo alcance | Rechazo `FECHA_DUPLICADA`; para corregir la descripción se edita, no se agrega otra |
| 3a | Se declara un feriado retroactivo | Se carga con su fecha real, pero **los plazos vigentes conservan su vencimiento**; la respuesta lo explica |
| 4a | El feriado cae sobre el vencimiento de un aporte | Se corre al siguiente hábil y **se notifica al grupo**: nadie debería enterarse de un cambio de fecha por su cuenta |
| 1b | Feriado de alcance `GRUPO` sin `grupo_id` | Rechazo `ALCANCE_INCOMPLETO` |
| — | Feriado departamental | Solo aplica a usuarios y grupos de ese departamento; el cálculo toma el domicilio declarado |
| 4b | El instante cae en un día distinto en UTC y en la zona del contrato | Manda el día de la zona del contrato; se devuelven las dos fechas y la zona, para que ninguna se confunda con la otra |
| 4c | La liquidación cruza de año y el año siguiente no tiene calendario | Rechazo `CALENDARIO_VACIO` del año que falta; no se cuenta corrido por omisión |
| 1c | Zona horaria que no existe | Rechazo `ZONA_INVALIDA` |
| — | Media jornada | No es día no hábil: se maneja con horario de atención, no con el calendario de plazos |
| — | Se borra un feriado cargado por error | La fila se marca inactiva con motivo, no se elimina; hay que poder explicar por qué un plazo dio ese número |

## Postcondiciones

- Todo plazo hábil del sistema se calcula contra un calendario único y auditable.
- Ningún plazo guardado cambia por una carga posterior de calendario.

## Contrato · `openapi/grupos.yaml`

```ts
export const EntradaCU59 = z.object({
  fecha:       z.string().date(),
  descripcion: z.string().min(3).max(120),
  alcance:     z.enum(['NACIONAL','DEPARTAMENTAL','PLATAFORMA','GRUPO']),
  departamento: z.string().max(30).nullable(),
  grupoId:     z.string().uuid().nullable(),
  fuente:      z.string().max(120),
}).strict()

export const EntradaCalcularCU59 = z.object({
  desde: z.string().datetime(),
  dias:  z.number().int().min(1).max(365),
  alcance: z.enum(['NACIONAL','DEPARTAMENTAL','PLATAFORMA','GRUPO']),
  referenciaId: z.string().uuid().nullable(),
}).strict()

export const SalidaCU59 = z.object({
  fechaLimite: z.string().datetime(),
  diasSalteados: z.array(z.object({
    fecha: z.string().date(), descripcion: z.string(),
  })),
  calendarioVersion: z.string(),   // para poder explicar el cálculo después
}).strict()

export const EntradaVencimientoCU59 = z.object({   // GET /grupos/calendario/vencimiento
  instante: z.string().datetime(),                  // el instante contractual, en UTC
  alcance: z.enum(['NACIONAL','DEPARTAMENTAL','PLATAFORMA','GRUPO']),
  referenciaId: z.string().uuid().nullable(),
  zona: z.string().max(40).nullable(),              // IANA; por omisión America/La_Paz
}).strict()

export const SalidaVencimientoCU59 = z.object({
  instanteUtc: z.string().datetime(),
  zonaHoraria: z.string(),
  fechaEnUtc: z.string().date(),                    // el día que ve el reloj en UTC
  fechaContractual: z.string().date(),              // el día en la zona del contrato
  fechaDeLiquidacion: z.string().date(),            // primer día hábil desde la contractual
  corridaPorNoHabil: z.boolean(),
  diasSalteados: z.array(z.object({ fecha: z.string().date(), descripcion: z.string() })),
}).strict()

export const ErroresCU59 = {
  FECHA_DUPLICADA:    'AP-CU59-01',
  ALCANCE_INCOMPLETO: 'AP-CU59-02',
  FECHA_EN_EL_PASADO: 'AP-CU59-03',
  SIN_FUENTE:         'AP-CU59-04',
  CALENDARIO_VACIO:   'AP-CU59-05',
  ZONA_INVALIDA:      'AP-CU59-06',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `FECHA_DUPLICADA` | Ya existe esa fecha con el mismo alcance y grupo |
| `ALCANCE_INCOMPLETO` | Alcance `GRUPO` sin `grupoId`, o `DEPARTAMENTAL` sin departamento |
| `FECHA_EN_EL_PASADO` | Se intenta cargar una fecha vencida en la carga masiva anual |
| `SIN_FUENTE` | No se indicó el decreto, resolución o calendario que lo respalda |
| `CALENDARIO_VACIO` | Se pide calcular un plazo en un período sin calendario cargado: **se rechaza en vez de contar corridos por defecto** |
| `ZONA_INVALIDA` | La zona horaria pedida no es un identificador IANA conocido |

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `sumarDiasHabiles(desde, dias, noHabiles)` | El cálculo; puro, con pruebas de propiedad y casos de borde en fin de año |
| Átomo | `siguienteHabil(fecha, noHabiles)` | Corrimiento a favor; puro |
| Molécula | `CalendarioRepositorio` | Consulta por rango y alcance, con caché de vida corta |
| Organismo | `CU59CargarCalendario` | Transacción: carga masiva con validación y constancia de fuente |
| Página | `POST /calendario` · `GET /calendario/calcular` | Traduce y delega |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `calendario.actualizado` | Recarga del caché y aviso a los grupos con vencimientos afectados | `CALENDARIO_ADMINISTRAR` |
| — | Control mensual de calendario del período siguiente | — |

## Interfaz

- **App:** en la fecha de vencimiento de un aporte, si se corrió por feriado, se
  muestra por qué. La fecha nunca cambia en silencio.
- **Backoffice:** calendario anual por alcance, con la fuente de cada día y qué
  plazos vigentes lo tomaron en cuenta.

## Restricciones aplicables

`R-CON-01` · `R-CON-02` · `R-AUD-01` · `R-AUD-04` · `R-GRP-16`

## Procedencia de los feriados (H7.S1.M3)

Un feriado no se inventa. `FeriadoConProcedencia` (dominio de `grupos`) es el mecanismo: exige la
fuente, un alcance completo y una fecha no vencida, y deja la fuente escrita en la descripción;
uno **sintético** (de prueba, de simulacro) lleva la marca `[SINTETICO - no apto para produccion]`
y no se confunde con uno oficial. Quién **escribe** en `catalogo.dia_no_habil` es una decisión
abierta: ese esquema es de solo lectura para los servicios (ADR-029), así que hoy el calendario
entra por semilla (`seeders/minimos/12-calendario-habil.json`, 2026-2028, con la norma citada en la
descripción y **sin verificar contra la Gaceta Oficial**) y `CU59CargarCalendario` sigue sin
implementar. Detalle en `docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-O2/`.

## Evidencia que deja

[[dia_no_habil]] · [[bitacora_evento]] · `evento_dominio` ·
[[reclamo_cliente]] (en el plazo que guarda) · [[registro_incumplimiento]]

## Criterios de aceptación

```gherkin
Dado un plazo de 5 días hábiles desde el viernes y un lunes feriado nacional
Cuando se calcula la fecha límite
Entonces el lunes no cuenta y la fecha resultante lo refleja
Y diasSalteados enumera el feriado

Dado un reclamo con fecha límite ya guardada
Cuando se declara un feriado dentro de ese plazo
Entonces la fecha límite del reclamo no cambia

Dado un vencimiento de aporte que cae en feriado declarado antes del cálculo
Cuando se genera la obligación
Entonces el vencimiento se corre al siguiente día hábil y se notifica al grupo

Dado un período sin calendario cargado
Cuando se pide calcular un plazo hábil
Entonces se rechaza con CALENDARIO_VACIO

Dado un vencimiento a las 22:00 del 1 de mayo de 2026 hora de La Paz (02:00 UTC del 2 de mayo) y el 1 de mayo feriado nacional
Cuando se calcula el vencimiento local en America/La_Paz
Entonces la fecha contractual es el 1 de mayo y la de liquidación el lunes 4 de mayo
Y la respuesta muestra también la fecha en UTC, el 2 de mayo, y los días que no contaron

Dado el mismo instante calculado en la zona UTC
Cuando se calcula el vencimiento local
Entonces la fecha contractual es el 2 de mayo y la de liquidación el lunes 4 de mayo
Y las dos zonas dan fechas contractuales distintas para el mismo instante

Dado un vencimiento a las 23:30 del 31 de diciembre de 2026 hora de La Paz (03:30 UTC del 1 de enero de 2027, feriado)
Cuando se calcula el vencimiento local
Entonces la fecha contractual y la de liquidación son el 31 de diciembre
Y el feriado del 1 de enero en UTC no corre nada

Dado un vencimiento el Viernes Santo de 2027, feriado nacional
Cuando se calcula el vencimiento local
Entonces la liquidación es el lunes siguiente
Y diasSalteados enumera el feriado, el sábado y el domingo

Dado un feriado local de un grupo sobre un vencimiento de ese grupo
Cuando se calcula el vencimiento local con el alcance GRUPO
Entonces la liquidación se corre al día hábil siguiente
Y el mismo instante con alcance NACIONAL no se corre

Dado un vencimiento en un año sin calendario cargado
Cuando se calcula el vencimiento local
Entonces se rechaza con CALENDARIO_VACIO
```

## Ver también

[[CU-25 Declarar el incumplimiento con descargo y evidencia]] · [[CU-43 Remitir los reportes mensuales a la UIF]] · [[CU-52 Atender un reclamo en plazo]] · [[CU-21 Cobrar el aporte del período]]
