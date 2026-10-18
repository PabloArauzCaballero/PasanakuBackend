---
tags:
  - caso-uso
  - modulo/09-auditoria-reportes-y-cumplimiento
codigo: CU-130
criticidad: media
actores: [Sistemas, Auditoría]
normas: [ISO 27001 A.5.9 inventario de activos de información, protección de datos, control interno]
---

# CU-130 — Gobernar la ficha de cada tabla del catálogo de datos

> **Objetivo.** Que cada tabla del modelo tenga dicho si está en uso, si alguien ya
> revisó su descripción, quién responde por ella y qué retención le aplica — como el
> catálogo de datos del portal interno de Atlas — sin depender de la memoria de nadie.

## Actores y disparador

- **Sistemas** (permiso `CATALOGO_DATOS_EDITAR`): revisa la ficha de una tabla y la
  guarda; propone retirar las que ya no se usan.
- **Auditoría** (`AUDITORIA_LEER` o `TABLERO_SISTEMAS_VER`): consulta el catálogo con
  el estado de cada tabla.

## Precondiciones

1. La tabla figura en el catálogo de datos vigente (el que genera
   `scripts/catalogo_de_datos.py` desde el modelo).
2. Si se indica una retención, la [[politica_retencion]] existe.

## Flujo principal

1. Quien consulta pide el catálogo de datos. A cada tabla se le superpone su
   [[ficha_gobierno_tabla]]; si no tiene, figura **activa** y **detectada** (está en
   el modelo y nadie la revisó), sin responsable.
2. Quien edita abre la ficha de una tabla y cambia estado, revisión, responsable,
   retención, si es crítica para auditoría y la nota de revisión.
3. Guarda enviando la `version` que leyó. El servidor comprueba que la tabla está en
   el catálogo y guarda la ficha con la versión siguiente, **solo si la guardada sigue
   siendo la que se leyó** (la condición va en la escritura).
4. En la misma transacción queda un evento `CAMBIO_CONFIGURACION` en la bitácora con
   el valor anterior y el nuevo.

## Flujos alternativos

| Paso | Condición | Respuesta |
| --- | --- | --- |
| 3 | Otra persona guardó antes con la misma versión | 422 `AP-CU130-01`; la ficha guardada no cambia y la pantalla pide recargar |
| 3 | La tabla no está en el catálogo | 422 `AP-CU130-02`; no se crea ninguna fila |
| 3 | La política de retención indicada no existe | 422 `AP-CU130-03`; la ficha no cambia |
| 3 | Valor fuera de los permitidos, campo desconocido o texto demasiado largo | 400 `AP-VAL-02` |
| 2 | Quien guarda no tiene `CATALOGO_DATOS_EDITAR` | 403; puede consultar pero no editar |

## Postcondiciones

La ficha refleja lo guardado y la bitácora tiene quién lo cambió, cuándo y qué cambió.

## Contrato · `openapi/auditoria.yaml`

```ts
export const EntradaCU130 = z.object({
  estado: z.enum(['ACTIVA','PLANIFICADA','CANDIDATA_A_RETIRO','RETIRADA','DESHABILITADA']),
  estadoRevision: z.enum(['DETECTADA','NECESITA_REVISION','APROBADA','RECHAZADA']),
  responsable: z.string().max(80).nullable(),
  politicaRetencionId: z.string().uuid().nullable(),
  criticaParaAuditoria: z.boolean(),
  notaRevision: z.string().max(500).nullable(),
  version: z.number().int().min(0),
}).strict()
```

`PUT /indicadores/catalogo-de-datos/{esquema}/{tabla}/gobierno` → la ficha guardada.
`GET /indicadores/catalogo-de-datos` devuelve cada tabla con su `gobierno`.

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `FichaDeGobierno.porOmision()` | Activa, detectada, versión 0; puro |
| Molécula | `FichaGobiernoRepositorio` | Leer todas; guardar con la versión como condición |
| Organismo | `CU130GobernarFichaDeTabla` | Valida contra el catálogo, guarda y deja la bitácora |
| Página | `PUT …/gobierno` · `GET /indicadores/catalogo-de-datos` | Traduce y delega |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| Bitácora `CAMBIO_CONFIGURACION` | — | `CATALOGO_DATOS_EDITAR` (con segundo factor, como todo operador) |

## Interfaz

- **App:** sin pantalla.
- **Backoffice:** *Catálogo de datos* en el menú de Sistemas: métricas, filtros por
  estado, revisión y módulo, columnas de estado y revisión, y la ficha de cada tabla
  con su sección de gobierno editable.

## Restricciones aplicables

Ninguna regla nombrada propia. La sensibilidad de la tabla **no** se edita acá: se
deriva de sus columnas en el modelo, para que haya una sola fuente de verdad.

## Evidencia que deja

[[ficha_gobierno_tabla]] · [[politica_retencion]] · [[bitacora_evento]]

## Criterios de aceptación

```gherkin
Dada una tabla del catálogo sin ficha guardada
Cuando se consulta el catálogo de datos
Entonces la tabla figura activa y detectada, sin responsable

Dado un operador con permiso para editar el catálogo de datos
Cuando guarda la ficha de una tabla como candidata a retiro
Entonces el catálogo la muestra así y la bitácora registra el cambio

Dadas dos ediciones de la misma ficha leídas en la misma versión
Cuando la segunda se guarda después de la primera
Entonces se rechaza con AP-CU130-01 y la ficha conserva lo de la primera

Dada una tabla que no está en el catálogo
Cuando se intenta guardar su ficha
Entonces se rechaza con AP-CU130-02 y no se crea ninguna fila
```

## Ver también

[[CU-58 Definir, programar y exportar un reporte]]
