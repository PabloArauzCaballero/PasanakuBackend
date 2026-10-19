---
tags:
  - caso-uso
  - modulo/09-auditoria-reportes-y-cumplimiento
codigo: CU-131
criticidad: media
actores: [Sistemas, Auditoría]
normas: [ISO 27001 A.5.9 inventario de activos de información, protección de datos, control interno]
---

# CU-131 — Revisar cada columna del catálogo de datos

> **Objetivo.** Que lo que el catálogo dice de cada columna (tipo, sensibilidad, nota de
> negocio) tenga el veredicto de una persona — aprobada, rechazada con motivo o pendiente —
> como en la revisión de columnas del portal interno de Atlas.

## Actores y disparador

- **Sistemas** (permiso `CATALOGO_DATOS_EDITAR`): abre la ficha de una tabla, pestaña
  Columnas, y revisa una columna desde su cajón lateral.
- **Auditoría** (`AUDITORIA_LEER` o `TABLERO_SISTEMAS_VER`): ve el estado de revisión de
  cada columna.

## Precondiciones

1. La tabla y la columna figuran en el catálogo de datos vigente.

## Flujo principal

1. Quien consulta pide el catálogo. A cada columna se le superpone su
   [[revision_columna]]; sin ella, figura **sin revisar**.
2. Quien revisa elige la decisión (aprobada, rechazada o necesita revisión), opcionalmente
   la confianza (baja, media, alta) y un motivo; al rechazar, el motivo es obligatorio.
3. Guarda enviando la `version` que leyó. El servidor comprueba que la columna está en el
   catálogo y guarda la revisión con la versión siguiente **solo si la guardada sigue
   siendo la que se leyó**.
4. En la misma transacción queda un evento `CAMBIO_CONFIGURACION` en la bitácora.

## Flujos alternativos

| Paso | Condición | Respuesta |
| --- | --- | --- |
| 3 | La columna no está en el catálogo | 422 `AP-CU131-01`; no se crea ninguna fila |
| 2 | Rechazo sin motivo | 422 `AP-CU131-02`; la columna sigue como estaba |
| 3 | Otra persona guardó antes con la misma versión | 422 `AP-CU131-03`; la revisión guardada no cambia |
| 2 | Quien guarda no tiene `CATALOGO_DATOS_EDITAR` | 403; puede ver, no revisar |
| 2 | Decisión o confianza fuera de los valores, campo desconocido, motivo demasiado largo | 400 `AP-VAL-02` |

## Postcondiciones

La columna muestra su revisión y la bitácora tiene quién, cuándo y qué.

## Contrato · `openapi/auditoria.yaml`

```ts
export const EntradaCU131 = z.object({
  decision: z.enum(['APROBADA','RECHAZADA','NECESITA_REVISION']),
  confianza: z.enum(['BAJA','MEDIA','ALTA']).nullable(),
  motivo: z.string().max(500).nullable(),
  version: z.number().int().min(0),
}).strict()
```

`PUT /indicadores/catalogo-de-datos/{esquema}/{tabla}/columnas/{columna}/revision` → la revisión guardada.
`GET /indicadores/catalogo-de-datos` devuelve cada columna con su `revision`, si la tiene.

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `RevisionDeColumna.exigeMotivo()` | Rechazar sin motivo no vale; puro |
| Molécula | `RevisionColumnaRepositorio` | Leer todas; guardar con la versión como condición |
| Organismo | `CU131RevisarColumna` | Valida contra el catálogo, guarda y deja la bitácora |
| Página | `PUT …/columnas/{columna}/revision` | Traduce y delega |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| Bitácora `CAMBIO_CONFIGURACION` | — | `CATALOGO_DATOS_EDITAR` |

## Interfaz

- **App:** sin pantalla.
- **Backoffice:** ficha de tabla → pestaña *Columnas*: cifras (columnas, con datos
  personales, financieras, descritas, aprobadas), filtros por sensibilidad y revisión, tabla
  con la revisión de cada columna y la acción «Revisar», que abre el cajón lateral.

## Restricciones aplicables

Ninguna regla nombrada propia.

## Evidencia que deja

[[revision_columna]] · [[bitacora_evento]]

## Criterios de aceptación

```gherkin
Dada una columna del catálogo sin revisión
Cuando se consulta el catálogo de datos
Entonces la columna figura sin revisar

Dado un operador con permiso para editar el catálogo de datos
Cuando aprueba una columna con confianza alta
Entonces el catálogo la muestra aprobada y la bitácora registra la revisión

Dada una columna que se rechaza sin motivo
Cuando se intenta guardar la revisión
Entonces se rechaza con AP-CU131-02 y la columna sigue sin revisar

Dada una columna que no está en el catálogo
Cuando se intenta revisarla
Entonces se rechaza con AP-CU131-01 y no se crea ninguna fila
```

## Ver también

[[CU-130 Gobernar la ficha de cada tabla del catálogo de datos]]
