---
tags:
  - arquitectura
  - adr
titulo: "ADR-046 — Alcance del helper de idempotencia"
estado: aceptada
fecha: 2026-09-22
---

# ADR-046 — Alcance de la idempotencia

> Carril PR3-plataforma (Leo), H1. Extiende [[ADR-024 Autenticación y sesión distribuida]]
> (de ahí sale `ContextoSesion.usuarioId()`) y precede a
> [[ADR-047 Semántica de entrega del outbox.md|ADR-047]] (H2), que es el otro lado del
> mismo problema: publicar sin publicar dos veces.

## Contexto

`Idempotencia.java` existía con dos defectos que solo se notaban con dos usuarios o dos
operaciones compartiendo clave, algo que una prueba con un solo usuario nunca ejercita:

1. **`exigirNueva` buscaba la reserva por `(clave_idempotencia, operacion)`**, sin
   `usuario_id`. Con la misma clave, un segundo usuario recibía la respuesta del
   primero — `409`/replay cruzado entre cuentas.
2. **`guardarRespuesta` actualizaba por `clave_idempotencia` sola.** Si la misma clave
   servía a dos operaciones del mismo hecho (`grupos/CU59`..`CU68` la reutilizan así,
   comentado en espera de este ADR), guardar la respuesta de una tocaba también la
   fila de la otra.
3. **No se comparaba el hash de la solicitud.** Cualquier reserva previa —sin importar
   el cuerpo— se trataba como replay. Un cliente que reintentaba con la clave de otra
   operación por error recibía la respuesta ajena en vez de un `409`.
4. **No había vencimiento ni distinción entre "en curso" y "final".** Una reserva sin
   respuesta bloqueaba para siempre (nunca expiraba en la práctica, porque nada volvía
   a mirar `expira_en`), y una respuesta `5xx` guardada se repetía igual que un éxito,
   impidiendo un reintento real tras una falla transitoria.

Kill-test que expone (1) y (2), corrido antes del cambio: dos usuarios con la misma
`Idempotency-Key` sobre `/aportes` — el segundo recibía el `201` del primero.

## Decisión

**La identidad de una reserva es `(usuario_id, operacion, clave_idempotencia)` — las
tres columnas, siempre juntas, nunca un subconjunto.** Es el índice único de
`sql/30_indices/10_billetera_custodia.sql`, y el código ahora lo refleja exactamente:
`exigirNueva` filtra por las tres, `guardarRespuesta` también.

**`exigirNueva` compara el hash de la solicitud y devuelve una de cuatro cosas:**

| Situación | Resultado |
| --- | --- |
| No hay reserva previa para esa identidad | Se reserva (`INSERT … ON CONFLICT DO NOTHING`, la carrera la resuelve el índice único) |
| Hay reserva, mismo hash, **ya finalizada** | `OperacionRepetida` → `200` con la respuesta original íntegra |
| Hay reserva, mismo hash, **sin finalizar** (o la carrera del `INSERT` la ganó otro) | `IdempotenciaEnProceso` → `409` |
| Hay reserva, **otro hash** | `IdempotenciaConflicto` (`AP-CU00-01`) → `409` |
| Hay reserva pero **venció** (`expira_en < ahora`, reloj inyectado) o **guardaba una respuesta transitoria** (`codigo_http ≥ 500`) | Se reutiliza la fila para una reserva nueva, como si no existiera |

Una respuesta `≥ 500` no cuenta como "final" para replay: es la diferencia entre un
`422` (la regla dijo que no, y repetir el reintento sin cambiar nada seguiría diciendo
que no — se replay) y un `503` (el problema fue nuestro, y el cliente tiene que poder
volver a intentar sin quedar atrapado en el eco de una falla pasada).

`OperacionRepetida` sigue siendo `200`, no `409`: el cliente hizo lo correcto al
reintentar con el mismo cuerpo. `IdempotenciaConflicto` es la única idempotencia que da
`409` por un cuerpo distinto — separada de `IdempotenciaEnProceso` porque son
situaciones distintas para quien opera: una es "estás usando mal la clave", la otra es
"la operación sigue corriendo, esperá".

`CU00` en `AP-CU00-01`/`AP-CU00-02`: no hay un caso de uso dueño de la idempotencia —es
infraestructura transversal—, así que se usa `00` en vez de inventarle un caso de uso
que no existe, siguiendo el mismo patrón que `AP-SES-01`/`AP-SEG-01`/`AP-VAL-0n` ya
usan para lo que tampoco es de un CU.

## Qué operaciones adoptan el helper

**Solo las de efecto financiero o irreversible.** No toda operación necesita esta
tabla: una consulta no tiene nada que idempotizar, y una operación reversible con su
propia clave de negocio (p. ej. un estado que solo avanza) ya es naturalmente segura de
reintentar sin esta capa.

Los siete comentarios de esqueleto que ya citaban `idempotencia.exigirNueva(...)`
(`grupos/CU59` calendario, `CU60` sorteo de turnos, `CU62` permuta, `CU63` resolver
acuerdo, `CU65` retiro de participante, `CU68` aceptar ingreso; `identidad/CU09`
restablecer operador) **adoptan el helper**: los seis de `grupos` mueven turnos y
acuerdos de un grupo real (irreversible una vez que otro participante actuó sobre el
resultado), y `CU09` emite una credencial nueva (irreversible: la vieja queda inválida).
Los resuelve Marcelo en su barrido de código muerto, con esta decisión como base — este
carril no toca `servicios/**`.

## Qué NO se decidió acá

- **La vigencia (`aportaya.idempotencia.vigencia`, hoy `Duration.ofDays(1)` fijo en
  código) todavía no sale a configuración.** Es H1.S2.M1, con su propia prueba de
  `testBarrido` (`SinUmbralLiteral`). Este ADR ya deja el `Reloj` inyectado
  (`Idempotencia(esquema, reloj)`), que es el prerequisito.
- **Qué esquemas además de `nucleo_financiero` tienen `respuesta_idempotente`.** Es
  H1.S2.M3, vía `scripts/generar_ddl.py` y micro-PR al troncal, una vez que Marcelo
  aplique la lista de arriba a los CU reales.

## Alternativas descartadas

| Alternativa | Por qué no |
| --- | --- |
| Guardar el hash pero no compararlo, solo para auditoría | Es exactamente el defecto (3): el cliente que se equivoca de clave recibe la respuesta de otro, y nadie lo nota hasta que alguien lee la bitácora. |
| Un único código de error para conflicto y "en proceso" | Son acciones distintas para el cliente: uno es un defecto propio (no reintentar con ese cuerpo), el otro es "esperá y reintentá". Fusionarlos le quita al cliente la información que necesita para decidir. |
| Repetir también las respuestas `5xx` | Convierte una falla transitoria del servidor en una falla permanente para el cliente: reintentar jamás progresa hasta que la fila expira. |
| DELETE + INSERT para reutilizar una reserva vencida/transitoria, en vez de UPDATE in situ | Mismo resultado, pero dos sentencias contra el índice único en vez de una, sin ninguna ventaja: se prefiere el UPDATE porque es una sola operación atómica. |

## Cómo se verifica

- [x] `IdempotenciaRepositorioTest` — 10 casos (Testcontainers PostgreSQL, esquema
      `nucleo_financiero`): nueva, replay, conflicto, otro usuario, otra operación, 50
      hilos, expirada, rollback, fallo tras reservar, reintento tras error transitorio.
- [x] `ManejadorGlobalDeErroresWebTest` — `IdempotenciaConflicto` → `409 AP-CU00-01`,
      `IdempotenciaEnProceso` → `409 AP-CU00-02`.
- [ ] `./gradlew testBarrido` con la regla de `clave_idempotencia` sin otra condición
      en la misma cadena (H1.S3.M2).

## Ver también

[[ADR-024 Autenticación y sesión distribuida]] · [[ADR-047 Semántica de entrega del outbox.md|ADR-047 Semántica de entrega del outbox]] · `docs/auditoria-produccion/carriles/PR3-plataforma.md`
