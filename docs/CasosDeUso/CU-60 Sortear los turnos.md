---
tags:
  - caso-uso
  - modulo/02-grupos-cupos-turnos-y-gobernanza
codigo: CU-60
criticidad: alta
actores: [Organizador, Participantes, Sistema]
normas: [Transparencia con el consumidor, RF-19]
---

# CU-60 — Sortear los turnos con esquema commit-reveal

> **Objetivo.** Asignar el orden de cobro de forma que **cualquier participante
> pueda recomputarlo y comprobar que no fue arreglado**. El orden de cobro es el
> punto de desconfianza número uno del pasanaku: quien cobra primero recibe un
> préstamo sin interés, quien cobra último hace un ahorro forzado.

## Actores y disparador

- **Actor principal:** el sistema, cuando el grupo se completa.
- **Actores secundarios:** organizador (convoca), participantes (aportan entropía).
- **Disparador:** el grupo queda `CONFORMADO` —porque alcanzó
  `cupos_ocupados = cupos_totales`, o porque el organizador lo inició con al menos el
  75 % de los cupos (`ceil(0,75 × cupos_totales)`) y el grupo se achicó a los ocupados—,
  todos firmaron el reglamento **y cada cupo ocupado tiene su primer aporte acreditado**
  (`R-GRP-20`, decisión del dueño 2026-10-30).

## Precondiciones

1. [[grupo]] en estado `CONFORMADO` y `modalidad_turnos = 'SORTEO_ALEATORIO'`.
2. Todos los [[cupo]] están `OCUPADO` y cada [[participante]] tiene
   [[aceptacion_reglamento]]. Un grupo iniciado al 75 % ya se achicó, así que acá
   «todos» es el 100 % de su tamaño nuevo.
3. No existe [[sorteo_turnos]] previo para el grupo (`R-GRP-05`).
4. Cada [[cupo]] `OCUPADO` tiene la [[obligacion_aporte]] del primer período en
   `ACREDITADO` ([[CU-21 Cobrar el aporte del período]]). El sorteo se hace con plata
   cobrada, no prometida (`R-GRP-20`). La primera obligación **nace al iniciar el grupo**: la
   operación de inicio (`POST /grupos/{grupoId}/inicio`, carril «arenas e inicio 75») abre el
   cobro del período 1 para cada cupo del grupo ya achicado, y el sorteo se habilita cuando todas
   están `ACREDITADO` por webhook verificado (decisión del dueño 2026-10-30).

## Flujo principal

### Fase 1 — compromiso (*commit*)

1. El sistema genera una semilla aleatoria criptográfica y **publica solo su
   hash**: se crea [[sorteo_turnos]] con `hash_semilla`, `metodo`,
   `fecha_compromiso` y estado `COMPROMETIDO`.
2. Se notifica a los participantes: *"el sorteo se ejecuta el día X; el compromiso
   ya está publicado y no se puede cambiar"*. La fila es *append-only*: el hash no
   admite `UPDATE`.
3. Cada participante puede aportar su propia entropía (un número o frase), que se
   concatena en `aportes_entropia`. **Nadie puede ver el aporte de otro antes del
   cierre**: se guarda su hash.

### Fase 2 — revelación (*reveal*)

4. Llegada la fecha, **en la misma transacción**:
   - se revela `semilla_revelada` y se verifica que
     `SHA256(semilla || entropias) = hash_semilla`;
   - se ordena la lista de cupos con el algoritmo declarado en `metodo`
     (Fisher-Yates con generador determinista sembrado por la semilla);
   - se crean los [[turno]] con `orden_asignado` y
     `criterio_asignacion = 'SORTEO'`;
   - el sorteo pasa a `REVELADO` y se registra `ejecutado_por`;
   - se emite `evento_dominio` `sorteo.revelado`.
5. Se publica el resultado con la semilla y el procedimiento, para que cualquiera
   lo recompute → [[CU-61 Verificar públicamente el sorteo]].
6. El grupo pasa a `EN_CURSO` y se abre el primer [[periodo]].

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 1a | El grupo usa `ORDEN_DE_INGRESO` o `ACUERDO_MANUAL` | No hay sorteo: los turnos se asignan por ese criterio y queda registrado en `criterio_asignacion` |
| 3a | Un participante no aporta entropía | El sorteo sigue: la entropía de los participantes **suma**, no condiciona |
| 4a | La verificación del hash falla | **El sorteo se anula**: se registra el incidente ([[incidente_operativo]]), se notifica a todos y se recomienza con semilla nueva. Jamás se publica un resultado cuyo hash no cierra |
| 4b | Se cae el proceso a mitad | La transacción revierte entera: o hay turnos completos o no hay ninguno |
| 1b | Un cupo ocupado no pagó la primera cuota | El compromiso se rechaza con `PRIMER_APORTE_PENDIENTE`; el organizador ve qué cupos faltan y el sorteo espera. No se sortea con plata prometida (`R-GRP-20`) |
| — | Alguien pide repetir el sorteo | Solo por [[acuerdo]] del grupo con quórum ([[CU-63 Proponer y votar un acuerdo]]), y queda el sorteo anterior visible |

## Postcondiciones

- Cada cupo tiene exactamente un turno y cada turno un orden único (`R-GRP-06`).
- El resultado es reproducible por terceros con los datos publicados.

## Contrato · `openapi/grupos.yaml`

```ts
export const EntradaCU60 = z.object({
  claveIdempotencia: z.string().uuid(),
  grupoId:           z.string().uuid(),
  fase:              z.enum(['comprometer', 'revelar']),
  entropiaParticipante: z.string().max(120).optional(),
}).strict()

export const SalidaCU60 = z.object({
  sorteoId:      z.string().uuid(),
  estado:        z.enum(['COMPROMETIDO', 'REVELADO']),
  hashSemilla:   z.string().length(64),
  semilla:       z.string().length(64).nullable(),   // solo tras revelar
  ordenAsignado: z.array(z.object({
    cupoNumero: z.number().int(), orden: z.number().int(),
  })),
}).strict()

export const ErroresCU60 = {
  GRUPO_NO_CONFORMADO:      'AP-CU60-01',
  SORTEO_YA_EXISTE:         'AP-CU60-02',
  FECHA_DE_REVELADO_FUTURA: 'AP-CU60-03',
  HASH_NO_VERIFICA:         'AP-CU60-04',   // aborta y notifica
  REGLAMENTO_SIN_FIRMAR:    'AP-CU60-05',   // version vigente del reglamento
  PLANTEL_O_CALENDARIO_CAMBIO: 'AP-CU60-06', // al revelar: no se sortea sobre otra cosa
  YA_REVELADO:              'AP-CU60-07',   // el reintento HTTP devuelve el original
  SORTEO_ANULADO:           'AP-CU60-08',
  CALENDARIO_AUSENTE:       'AP-CU60-09',
  NO_ADMINISTRA_EL_GRUPO:   'AP-CU60-10',
  PRIMER_APORTE_PENDIENTE:  'AP-CU60-11',   // R-GRP-20
  APORTES_SIN_RESPUESTA:    'AP-CU60-12',   // R-GRP-20, fallo cerrado
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `GRUPO_NO_CONFORMADO` | Quedan cupos libres o participantes sin aceptar: no se sortea un grupo a medio armar |
| `SORTEO_YA_EXISTE` | El grupo ya tiene un sorteo comprometido; no se vuelve a comprometer |
| `FECHA_DE_REVELADO_FUTURA` | Se pidió revelar antes de la fecha comprometida: el compromiso sería inútil |
| `HASH_NO_VERIFICA` | La semilla revelada no reproduce el hash. **Aborta, no asigna turnos y abre incidente** |
| `REGLAMENTO_SIN_FIRMAR` | Algún ocupante no aceptó la **versión vigente** del reglamento (al iniciar el grupo se publica una nueva; la firma de la anterior no vale). `detalle.cuposSinFirma` trae los números de cupo |
| `PLANTEL_O_CALENDARIO_CAMBIO` | `AP-CU60-06` · Al revelar, el plantel ocupado o el calendario ya no son los congelados en el compromiso: se rechaza sin anular y el sorteo sigue `COMPROMETIDO` |
| `YA_REVELADO` | `AP-CU60-07` · El sorteo ya tiene su resultado vigente; revelar de nuevo no sortea otra vez (el reintento HTTP devuelve el original) |
| `SORTEO_ANULADO` | `AP-CU60-08` · El sorteo quedó anulado por un hash que no verificó; no se repite sin una decisión de backoffice |
| `CALENDARIO_AUSENTE` | `AP-CU60-09` · Falta el calendario del grupo: cada cupo necesita su período para crear los turnos |
| `NO_ADMINISTRA_EL_GRUPO` | `AP-CU60-10` · Quien llama no administra el grupo de la ruta |
| `PRIMER_APORTE_PENDIENTE` | `AP-CU60-11` · Algún cupo ocupado no tiene la obligación de la cuota 1 en `PAGADO` (`R-GRP-20`). `detalle.cuposPendientes` trae los números de cupo. Lo afirma `aportes` (`GET /aportes/grupos/{grupoId}/primera-cuota`), nunca quien sortea |
| `APORTES_SIN_RESPUESTA` | `AP-CU60-12` · `aportes` no contestó (caído, circuito abierto o respuesta ilegible). Se deniega: lo único que no se puede suponer es que la plata está (`R-GRP-20`) |

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `barajarDeterminista(semilla, cupos)` | Fisher-Yates puro y reproducible; sin IO |
| Átomo | `verificarCompromiso(semilla, entropias, hash)` | Comparación de hash, pura |
| Molécula | `SorteoRepositorio` | Lee y escribe `sorteo_turnos` y `turno` |
| Organismo | `CU60SortearTurnos` | Abre la transacción y ordena las dos fases |
| Página | `POST /grupos/:id/sorteo` | Traduce HTTP y delega |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `sorteo.comprometido` | Notificación a todos los participantes con la fecha | `GRUPO_ADMINISTRAR` |
| `sorteo.revelado` | Notificación con el orden + publicación del bloque de transparencia ([[CU-72 Sellar el bloque de transparencia]]) | — |

## Interfaz

- **App:** pantalla *Tu turno*, con la cuenta regresiva al revelado, el campo para
  aportar entropía y, después, el orden completo con el botón **Verificar**.
- **Backoffice:** detalle del grupo → pestaña *Sorteo*, con hash, semilla y el
  recomputo del orden.

## Restricciones aplicables

`R-GRP-05` · `R-GRP-06` · `R-GRP-18` · `R-AUD-01` · `R-AUD-04`

## Evidencia que deja

[[sorteo_turnos]] (*append-only*) · [[turno]] · [[bloque_transparencia]] ·
`evento_dominio` · [[bitacora_evento]] ·
[[snapshot_sorteo]]

## Criterios de aceptación

```gherkin
Dado un grupo conformado con 6 cupos
Cuando se ejecuta la fase de compromiso
Entonces existe un sorteo_turnos COMPROMETIDO con hash_semilla de 64 caracteres
Y no existe ningún turno todavía

Dado un sorteo comprometido cuya fecha de revelado llegó
Cuando se revela la semilla
Entonces SHA256(semilla || entropías) coincide con el hash publicado
Y existen 6 turnos con órdenes 1..6 sin repetir

Dada la misma semilla y los mismos cupos
Cuando un tercero recomputa el orden
Entonces obtiene exactamente el mismo resultado

Dado un intento de revelar con una semilla que no verifica
Cuando se ejecuta
Entonces no se crea ningún turno
Y el sorteo queda ANULADO con su incidente registrado

Dado un grupo conformado con sus cupos ocupados y su calendario cargado
Cuando se ejecuta la fase de compromiso
Entonces queda un snapshot con el plantel, el calendario y las reglas congelados, con un hash de 64 caracteres
Y el hash del snapshot entra en las entropías junto con el aporte recibido
Y la semilla queda sellada en el servidor y no aparece en el texto del compromiso

Dado un sorteo comprometido y un cupo que deja de estar ocupado antes de la revelación
Cuando se intenta revelar
Entonces se rechaza sin crear turnos y el sorteo sigue COMPROMETIDO, sin anularse
Y al restituir el plantel congelado la revelación procede con el mismo compromiso

Dado un sorteo comprometido y una fecha límite de pago que cambia después del compromiso
Cuando se intenta revelar
Entonces se rechaza y no se crea ningún turno

Dado un sorteo que ya fue revelado
Cuando se intenta revelar de nuevo
Entonces se rechaza y no se sortea otra vez
Y el resultado original sigue consultable con el mismo orden y la misma semilla
Y los turnos son los del primer sorteo

Dado un sorteo comprometido cuya semilla todavía no se reveló
Cuando se revela y un tercero reconstruye el resultado con el paquete publicado
Entonces antes de la revelación el paquete no trae semilla ni orden
Y después la semilla revelada verifica el hash comprometido con las entropías
Y barajar el orden original con esa semilla da exactamente el orden sorteado

Dado un sorteo comprometido con la semilla sellada en el servidor
Cuando se intenta revelar con unas entropías que no son las comprometidas
Entonces se rechaza sin revelar ni anular: el sorteo sigue COMPROMETIDO y no hay turnos
Y con las entropías comprometidas la revelación sí verifica

Dado un sorteo comprometido en un grupo sin calendario
Cuando se intenta revelar
Entonces se rechaza pero el sorteo sigue vivo en COMPROMETIDO
Y al existir el calendario la revelación procede y se crean los turnos

Dado un grupo conformado con un cupo que no pagó la cuota 1
Cuando se intenta comprometer el sorteo
Entonces se rechaza con AP-CU60-11 y el detalle nombra el cupo pendiente
Y no existe sorteo

Dado que aportes no responde
Cuando se intenta comprometer el sorteo
Entonces se rechaza con AP-CU60-12: sin confirmar la plata no se sortea

Dado un grupo cuyos tres participantes firmaron la versión vigente del reglamento
Cuando se compromete el sorteo
Entonces procede: la firma vigente alcanza

Dado un grupo cuyo reglamento firmado fue reemplazado por una versión nueva que nadie firmó
Cuando se intenta comprometer el sorteo
Entonces se rechaza con AP-CU60-05 y nombra los cupos sin firma: la firma de la versión anterior no vale
```

## Ver también

[[CU-20 Crear grupo y congelar tarifario]] · [[CU-61 Verificar públicamente el sorteo]] · [[CU-62 Permutar turnos entre participantes]] · [[CU-63 Proponer y votar un acuerdo]]
