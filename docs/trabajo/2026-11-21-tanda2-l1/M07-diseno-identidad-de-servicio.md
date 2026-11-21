# F1.L1.M07 · Identidad de servicio para las lecturas de `aportes` que hacen otros servicios

- Fecha: 2026-11-21 · Estado: **A MEDIAS** (diseño escrito; implementación coordinada con el coordinador
  antes de empujar, porque toca tres servicios de otros lotes) · Regla: 98.6 (confianza cero entre servicios).

## El problema (BOLA, regla 90.1.3)

Tres lecturas de `aportes` responden a cualquier sesión que tenga un permiso genérico, sin mirar de quién es el
recurso:

| Ruta de `aportes` | Hoy exige | Quién la llama hoy | Con qué token |
|---|---|---|---|
| `GET /aportes/grupos/{grupoId}/morosos` | `BILLETERA_VER` | `grupos` · `HechosPorHttp:186` | el de la sesión de la persona (incluso un postulante que todavía no es miembro) |
| `GET /aportes/grupos/{grupoId}/primera-cuota` | `BILLETERA_VER` | `grupos` · `PrimerAportePorHttp:41` | el de la sesión |
| `GET /aportes/periodos/{periodoId}/recaudo` | `ENTREGA_EJECUTAR` | `entregas` · `RecaudoPorHttp:50` y `garantia` · `RecaudoPorHttp:51` | el de la sesión o el que tenga el proceso |

Cualquier participante con `BILLETERA_VER` puede preguntar los morosos de un grupo que no es suyo.

## Lo que ya existe (no se inventa nada)

- `plataforma/comun-web` · `CredencialDeServicio` y `CredencialDeServicioConfigurada`: token de una **cuenta de
  servicio** en `identidad` (ADR-024, único emisor). Se renueva sola y está vacío si falta. Quien llama **falla
  cerrado**.
- Ya la usan `grupos` hacia `transparencia` (`EtapasPorHttp`, rol `SERVICIO_GRUPOS`), `entregas` hacia el libro
  (`SERVICIO_ENTREGAS`) e `inversiones` (`SERVICIO_INVERSIONES`). Cada servicio declara solo los nombres de sus
  propiedades (`ConfiguracionDeCredencialDeServicio`).
- Del lado que recibe, `nucleo-financiero` ya valida la identidad de servicio con `@Permiso("SERVICIO_…")` e
  `IdentidadDeServicio`.

## Diseño: expandir → migrar → contraer (regla 98.1.3)

**Paso 1 · `aportes` acepta las dos formas (aditivo, compatible).**
- `morosos` y `primera-cuota` aceptan `SERVICIO_GRUPOS`, o una persona que **pertenezca al grupo**. La pertenencia
  se consulta con `PertenenciaDelParticipante`, que ya existe desde #441.
- `recaudo` acepta `SERVICIO_ENTREGAS` o `SERVICIO_GARANTIA`, o `ENTREGA_EJECUTAR` como hasta hoy, transitorio.
- Matriz negativa web: otro usuario, otro grupo, rol de otro servicio, sin token.

**Paso 2 · los que llaman pasan a su credencial de servicio.**
- `grupos`: `HechosPorHttp.morosos` y `PrimerAportePorHttp` agregan `Authorization: Bearer <credencial>`, igual que
  `EtapasPorHttp:125`. Sin credencial no sale la petición y el resultado es VACÍO, que para decidir un ingreso es
  «no entra».
- `entregas`: `RecaudoPorHttp` usa la `CredencialDeServicio` que el servicio ya tiene.
- `garantia`: **no tiene cuenta de servicio**. No existe el rol `SERVICIO_GARANTIA`
  (`seeders/minimos/10-roles-y-permisos.json`) ni sus propiedades. Hace falta:
  1. el rol `SERVICIO_GARANTIA` en las semillas (compartido);
  2. su cuenta de servicio en las semillas de dev;
  3. `ConfiguracionDeCredencialDeServicio` en `garantia`, copiada de la de `entregas` (solo cambian los nombres);
  4. las variables `GARANTIA_…_CUENTA_SERVICIO_*` en `scripts/generar_compose.py`, en Coolify y en el inventario de
     rotación de secretos.

**Paso 3 · contraer.** Cuando los tres consumidores lleven credencial de servicio en TEST (se ve en los logs de
acceso por rol), `recaudo` deja de aceptar `ENTREGA_EJECUTAR`. `morosos` y `primera-cuota` dejan de aceptar sesiones
que no pertenecen al grupo; esto último ya pasa desde el paso 1.

## Qué falta decidir / coordinar

- Dueños: `grupos` (L3), `entregas` y `garantia` (L2). El paso 2 toca sus archivos: **se coordina con el
  coordinador antes de empujar**.
- Crear la cuenta de servicio de `garantia` agrega dos variables secretas al despliegue (Coolify, Pablo).
- Sin esos cambios, el paso 1 ya corta el acceso de un usuario a grupos ajenos. Pero `grupos` sigue preguntando con
  el token del postulante, y un postulante todavía no pertenece al grupo: **el paso 1 sin el paso 2 rompe el ingreso
  a grupos**. Por eso los pasos 1 y 2 de `grupos` van juntos.
