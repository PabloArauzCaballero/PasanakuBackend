---
tags:
  - arquitectura
  - adr
titulo: "ADR-049 — Doble aprobación de retiros"
estado: aceptada
fecha: 2026-09-22
---

# ADR-049 — Doble aprobación de retiros

> Carril PR2 ([[PR2-nucleo-financiero|PR2-nucleo-financiero]]), hito
> H3. Referencia CU-11 y `R-SEG-04`.

## Contexto

`orden_retiro` tenía, antes de este carril, una columna `requiere_doble_aprobacion`
sin ninguna restricción que la hiciera valer: nada impedía que una orden marcada
como necesitando doble aprobación terminara `PAGADA` sin que un segundo humano la
hubiera mirado. La tabla tampoco guardaba quién había solicitado el retiro, así que
ni siquiera había con qué comparar un eventual aprobador — la segregación de
funciones (cuatro ojos) era una intención documentada, no una regla exigible.

Cerrar esto exige dos cosas que ninguna es suficiente por sí sola:

1. **Un CHECK en la base** (`ck_retiro_doble_aprobacion`) que impida que una fila
   quede en un estado terminal ("pagable") sin `aprobada_por` distinto de
   `solicitada_por`.
2. **Una máquina de estados en la aplicación** que decida, en el momento de
   solicitar, si la orden nace `AUTORIZADA` (puede seguir sola) o `EN_REVISION`
   (necesita que alguien más la mire), y que exponga una operación explícita de
   aprobar/rechazar que solo alguien con el permiso correcto — y que no sea quien
   la pidió — pueda ejecutar.

Un CHECK solo en la base rechaza el `INSERT`/`UPDATE` malo, pero con un mensaje de
restricción genérico que no le sirve a un cliente HTTP para saber qué hacer. Una
regla solo en la aplicación es bypasseable por cualquier vía que escriba
directamente en la tabla (otro caso de uso, una migración, una consola). Regla 65
del carril lo resume: la base tiene la última palabra, la aplicación adelanta el
rechazo con un mensaje útil.

Decisión previa **Q-01** (ya cerrada en el encargo de este carril, no se reabre
acá): el endpoint es `POST /billetera/retiros/{ordenId}/aprobacion`, el permiso es
`RETIRO_APROBAR`, y el rol que lo tiene es `TESORERIA`.

## Decisión

**`EstadoDeRetiro` es un enum de dominio puro (`PENDIENTE`, `EN_REVISION`,
`AUTORIZADA`, `EN_PROCESO`, `PAGADA`, `RECHAZADA`, `REVERSADA`) con su propia tabla
de transiciones válidas, y `CU11RetirarSaldo` es el único punto de la aplicación que
decide cuándo una orden pasa de un estado a otro.**

### La bifurcación en `solicitar`

Al crear la orden, `CU11.solicitar` calcula si el monto supera
`aportaya.retiro.doble-aprobacion-desde` (configurable, `5000.00` en el ambiente de
pruebas) y decide el estado inicial:

| Condición | Estado inicial |
| --- | --- |
| Bajo el umbral | `AUTORIZADA` — en la misma transacción, nunca pasando por `PENDIENTE` primero (**Q-02**, ya decidido) |
| Sobre el umbral (`requiereDobleAprobacion = true`) | `EN_REVISION` — espera a un segundo aprobador |

### Aprobar y rechazar: `EN_REVISION → {AUTORIZADA, RECHAZADA}`

Dos operaciones nuevas en `CU11RetirarSaldo`, `aprobar` y `rechazarRevision`,
comparten una implementación (`resolverAprobacion`) que:

1. Lee la orden y comprueba `orden.solicitadaPor().equals(ctx.usuarioId())` —
   **antes** de tocar la base — y si coincide, rechaza con
   `AP-CU11-10 RETIRO_AUTOAPROBACION_PROHIBIDA`. Es la primera capa.
2. Ejecuta un `UPDATE ... WHERE estado = 'EN_REVISION' AND solicitada_por <>
   aprobada_por` (`OrdenRetiroRepositorio.pasarAAutorizadaPorAprobacion` /
   `pasarARechazadaPorAprobacion`). La fila misma es el punto de sincronización:
   dos aprobadores simultáneos solo dejan pasar a uno, el `UPDATE` del segundo
   afecta cero filas porque el estado ya cambió, y recibe
   `AP-CU11-11` ("ya no está en revisión"). No hace falta un lock explícito.
3. Si el `UPDATE` afecta cero filas por cualquier motivo (ya resuelta por otro,
   ya no está en revisión), lanza `ErrorDeNegocio` — nunca un éxito silencioso.
4. Si rechaza, libera la retención asociada en la misma transacción.
5. Emite el evento de dominio (`retiro_autorizado` / `retiro_rechazado`) al outbox.

El permiso `RETIRO_APROBAR` (rol `TESORERIA`, **Q-05**) es la guardia de entrada en
el controlador — 403 antes de que la petición llegue al caso de uso. La segregación
solicitante-≠-aprobador es una guardia **distinta**, deliberadamente duplicada: vive
en el caso de uso (para dar un código de error útil) y en el CHECK de la base (para
que ninguna otra vía la esquive).

### El CHECK, actualizado

`ck_retiro_doble_aprobacion` (en `docs/Restricciones.md`, generado a
`sql/40_reglas/restricciones.sql` vía `scripts/extraer_sql.py`) ahora permite
`EN_REVISION` en la lista de "todavía sin aprobar", junto a `BORRADOR`,
`PENDIENTE` y `RECHAZADA`:

```sql
ALTER TABLE orden_retiro
  ADD CONSTRAINT ck_retiro_doble_aprobacion CHECK (
      NOT requiere_doble_aprobacion
   OR estado IN ('BORRADOR','PENDIENTE','EN_REVISION','RECHAZADA')
   OR (aprobada_por IS NOT NULL AND aprobada_por <> solicitada_por)
  );
```

Antes de H3, `EN_REVISION` no existía como estado explícito — la espera del segundo
aprobador se hubiera confundido con `PENDIENTE`, el mismo estado que usa un retiro
que ni siquiera necesita doble aprobación. Separarlos es lo que permite que la
consulta "¿qué está esperando aprobación?" sea una consulta por estado, no una
inferencia sobre otras columnas.

## Motivo

**Dos capas independientes, no una capa "reforzada".** La aplicación adelanta el
rechazo con un mensaje útil (`AP-CU11-10`/`AP-CU11-11`) porque el mensaje genérico
de una violación de CHECK no le dice a un cliente HTTP qué pasó. La base tiene la
última palabra porque la aplicación puede tener un bug, y un bug en `CU11` no puede
convertirse en un retiro sin cuatro ojos.

**`EstadoDeRetiro` como enum puro, sin Spring, sin base de datos.** La máquina de
estados es una regla de negocio que se puede probar con JUnit puro en milisegundos
(`EstadoDeRetiroTest`), separada de si Postgres está arriba o no. Que
`OrdenRetiroRepositorio` reciba el nombre del estado como `String` en vez del enum
es deliberado: esa clase es infraestructura, no le corresponde saber cuáles son las
transiciones válidas.

**El `UPDATE` condicionado por `WHERE estado = 'EN_REVISION'` en vez de un lock
explícito** (`SELECT ... FOR UPDATE`, advisory lock) porque es más simple, no
retiene una conexión más tiempo del necesario, y el resultado es el mismo: como
mucho una fila cambia de estado por cada transición real.

## Alternativas descartadas

| Alternativa | Por qué no |
| --- | --- |
| **Solo el CHECK, sin máquina de estados en la aplicación** | Rechaza el `INSERT` malo, pero el cliente HTTP recibe un error de restricción sin código de negocio ni distinción entre "ya resuelta" y "no puede auto-aprobar". |
| **Solo la aplicación, sin CHECK** | Cualquier escritura que no pase por `CU11` (otro caso de uso, una migración, un `UPDATE` manual en un incidente) podría dejar una orden `PAGADA` sin segundo aprobador. Es exactamente el hueco que este ADR cierra. |
| **`SELECT ... FOR UPDATE` explícito sobre la orden al aprobar** | Funciona, pero retiene una fila bloqueada mientras dura la transacción y agrega una forma más de interbloqueo. El `UPDATE` condicionado logra la misma exclusión sin el lock explícito. |
| **Un estado `PENDIENTE_APROBACION` en vez de reusar el nombre `EN_REVISION`** | Se prefirió `EN_REVISION` por ser más corto y menos atado a un único motivo posible de revisión (hoy es solo el umbral de monto; si mañana hay otro motivo, el nombre no cambia). |
| **Permitir que el propio solicitante apruebe si tiene el permiso `RETIRO_APROBAR`** | Rompe la garantía de cuatro ojos que es el propósito entero de la doble aprobación — alguien con el permiso más alto podría auto-aprobar cualquier retiro propio. |

## Consecuencias

**A favor**

- Ningún retiro que requiera doble aprobación puede llegar a `PAGADA` sin que un
  segundo humano, con el permiso correcto, lo haya autorizado — ni siquiera si
  `CU11` tiene un bug.
- Dos aprobadores simultáneos sobre la misma orden producen una sola transición:
  el segundo recibe un error de negocio, no un doble efecto ni una excepción de
  bajo nivel sin traducir.
- El mensaje de error distingue "sos quien la pidió" de "ya no está en revisión",
  lo que le permite al cliente HTTP decidir qué mostrar.

**En contra, y hay que asumirlo**

- La regla de segregación vive en dos lugares (Java y SQL) que tienen que decir
  lo mismo. Un cambio futuro en el umbral de una capa sin la otra las
  desincroniza — está mitigado por `CU11AprobacionConcurrenciaTest` y
  `CU11RechazosTest#rechazaRSEG04`, que prueban las dos capas por separado.
- El endpoint de aprobación exige un `Idempotency-Key` por convención de este
  módulo, pero **no** ofrece réplica idempotente completa: un reintento después de
  que la orden ya cambió de estado recibe el mismo error de negocio que un segundo
  aprobador distinto, no la respuesta original. Es una limitación declarada, no
  escondida: la garantía de seguridad (nunca doble efecto) se sostiene igual,
  porque el `UPDATE` condicionado la protege independientemente de la clave.

## Cómo se verifica

- [x] `EstadoDeRetiroTest` — la tabla de transiciones válidas, sin Spring ni base.
- [x] `CU11Test#criterio1` y `CU11AprobacionTest#solicitudGrandeQuedaEnRevision` —
      bajo el umbral nace `AUTORIZADA`, sobre el umbral nace `EN_REVISION`.
- [x] `CU11AprobacionTest#aprueba` / `#rechaza` — un aprobador distinto resuelve la
      orden y, si rechaza, libera la retención.
- [x] `CU11AprobacionTest#autoAprobacionProhibida` — el solicitante no puede
      aprobar su propia orden, con `AP-CU11-10`.
- [x] `CU11AprobacionTest#yaResuelta` / `#reintentoNoDuplica` — un segundo intento
      sobre una orden ya resuelta falla, sin doble efecto.
- [x] `CU11AprobacionConcurrenciaTest` — 20 aprobadores simultáneos sobre la misma
      orden, gana exactamente uno; aprobar y rechazar a la vez, gana exactamente
      uno de los dos.
- [x] `CU11RechazosTest#rechazaRSEG04` — el CHECK de la base rechaza una fila
      `AUTORIZADA` con `aprobada_por = solicitada_por`, sin pasar por la
      aplicación.
- [x] `BilleteraControllerWebTest.Aprobacion` — 403 sin `RETIRO_APROBAR`, 200 para
      cada desenlace, el desenlace del cuerpo decide qué método del caso de uso se
      llama, y un `ErrorDeNegocio` del caso de uso se propaga como 422 con su
      código.
