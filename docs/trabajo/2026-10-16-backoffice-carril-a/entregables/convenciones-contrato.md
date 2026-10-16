# Convenciones de contrato para el backoffice — carril A · A0

> Estado: **propuesto** (A-1). Todo lo de abajo describe lo que el código YA hace, con la cita
> `ruta:línea` que lo prueba; nada es una convención nueva. Las citas las comprueba
> `python3 docs/trabajo/2026-10-16-backoffice-carril-a/entregables/matriz.py --citas`.
> Sirve de ficha común para que el carril B consuma cualquier contrato nuevo sin adivinar.

## 1. Rutas, versión y permisos

- Base `/api/v1`, un prefijo reservado por servicio; una ruta fuera de su prefijo es un rechazo
  (`servicios/nucleo-financiero/src/main/resources/openapi/nucleo-financiero.yaml:5`).
- El permiso se declara por operación con `@Permiso`; el del método gana sobre el de la clase
  (`plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/seguridad/GuardiaDePermiso.java:85`).
  `@PermisoAlternativo` acepta otros códigos para la misma operación (ejemplo: lecturas de
  Contabilidad, `servicios/erp/src/main/java/bo/aportaya/erp/web/PermisosDeContabilidad.java:1`).
- Todo operador lleva `app.rol = BACKOFFICE`
  (`servicios/identidad/src/main/java/bo/aportaya/identidad/web/SesionesController.java:55`);
  lo que distingue a un oficial de un analista es el **permiso** del token, no el rol. RLS trata
  `BACKOFFICE` como privilegiado (`sql/40_reglas/restricciones.sql:1326`).
- Un código que el catálogo no tiene **no se inventa** (S-8): ver `matriz-autorizacion.md`.

## 2. Errores

| Estado | Cuándo | Prueba |
| --- | --- | --- |
| 400 | El cuerpo, un parámetro o un enum no cumple el contrato; campos no declarados | `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/errores/ManejadorGlobalDeErrores.java:144` |
| 401 | Sin sesión | guardia de sesión de `comun-web` |
| 403 | Sin el permiso (`codigo: AP-SEG-01`), igual exista o no el recurso | `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/errores/ManejadorGlobalDeErrores.java:101` |
| 409 | Lo rechaza la **base** (restricción traducida) o hay una carrera de idempotencia | `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/errores/ManejadorGlobalDeErrores.java:121` |
| 422 | Regla de negocio, con su `AP-CU<NN>-<nn>` | `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/errores/ManejadorGlobalDeErrores.java:54` |

**Atención (A-5):** varios contratos prometen 409/404 para "ya resuelto"/"no existe" pero el
código emite 422 con su `AP-…`; la UI debe decidir por `codigo`, no por el estado HTTP.

## 3. Idempotencia

- Toda operación con efecto exige `Idempotency-Key` (UUID) por cabecera
  (`servicios/nucleo-financiero/src/main/resources/openapi/nucleo-financiero.yaml:755`).
- Tres implementaciones existentes, en orden de preferencia:
  1. **Registro de respuesta** por (usuario, operación, clave) con candado consultivo:
     `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/RegistroDeIdempotencia.java:33`
     (usado por la aprobación de retiros).
  2. **Por estado guardado**, cuando el esquema no tiene `respuesta_idempotente`: el reintento
     idéntico del mismo actor devuelve el recibo y otra cosa es conflicto de negocio
     (`servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU52AtenderReclamo.java:1`,
     `servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU44DecidirComoRevisor.java:1`).
  3. Respuesta repetida de la plataforma: `200` con el cuerpo original
     (`plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/errores/ManejadorGlobalDeErrores.java:68`).
- La UI **no** dice "aprobado" hasta recibir la respuesta; reintentar con la **misma** clave
  tras perder la respuesta es seguro.

## 4. Listados (bandejas)

- Paginación por **cursor** (keyset), nunca por OFFSET: parámetros `despuesDe<Campo>` +
  `despuesDeId`, respuesta `items` + `proximo<Campo>` + `proximoId` (nulos en la última página).
  Patrón original: `servicios/organizador/src/main/resources/openapi/organizador.yaml:112` y
  `servicios/organizador/src/main/java/bo/aportaya/organizador/infraestructura/HabilitacionRepositorio.java:54`.
- Orden determinista con desempate por `id`; el cursor a medias es un error de negocio del CU.
- `limite` con mínimo 1 y máximo declarado en el contrato (25 por omisión en las bandejas).
- **Respuesta mínima** en la fila: sin datos de la persona, sin textos libres ni montos de
  terceros; eso va en la ficha con su propio permiso.

## 5. Importes, fechas y zona

- Importes como **cadena decimal** con dos decimales y su moneda (`Dinero`)
  (`servicios/nucleo-financiero/src/main/resources/openapi/nucleo-financiero.yaml:1127`).
  Donde el modelo no guarda moneda (A-7) se publica la cadena y se declara el hueco; no se supone BOB.
- Fechas `date-time` ISO-8601; "hoy" y los plazos se evalúan en `America/La_Paz`
  (`plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/Reloj.java:26`).
- Los plazos se **guardan** al crear y no se recalculan al consultar; `vencido` solo compara el
  guardado con la hora.

## 6. Correlación y traza

- Cada petición lleva `X-Request-Id` (lo pone NGINX o el filtro)
  (`plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/traza/Traza.java:16`).
- Cada operación marca su CU con `Traza.marcarCasoDeUso("CU-NN", …)`
  (`plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/traza/Traza.java:60`).
  **Riesgo anotado:** el segundo argumento se guarda en el MDC con la clave de *usuario*, pero el
  repo le pasa el id del recurso o una etiqueta; nunca un dato personal. Renombrarlo es de
  `plataforma` (micro-PR), no de este carril.
