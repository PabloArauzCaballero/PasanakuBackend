# Plan — Regenerar catalogo-operaciones.json y operaciones.json del backoffice

- Fecha: 2026-11-27 · Repos afectados: PasanakuBackend (`servicios/auditoria/.../catalogo-operaciones.json`, `apps/backoffice/.../operaciones.json`) · Predecesor: `docs/trabajo/2026-11-26-pasarela-simulada-cu21-endpoint` (lo dejó pendiente)
- Resultado observable: el catálogo de operaciones que sirve `auditoria` y que muestra el backoffice coincide con los OpenAPI vigentes (incluye `emitirAvisoDePagoSimulado`).
- Kill-test: regenerar dos veces ⇒ mismos bytes; `git diff` solo con operaciones que existan (o dejaron de existir) en los OpenAPI.

## Hallazgos (con evidencia en `evidencia/analisis-del-diff.txt`)
- El generador es determinista y las dos copias (auditoría y backoffice) salen idénticas.
- Las ~11 000 líneas de diferencia NO son ruido: el archivo comprometido estaba al día a 349 operaciones; hoy son 396 (49 nuevas, todas con `operationId` en algún
  OpenAPI; 2 retiradas, ausentes de todos los OpenAPI) y 56 existentes con cambios reales (descripciones de negocio, campos de DTO, ejemplos).
- Nadie lo regeneraba: no hay paso de CI que lo compare (a diferencia de Postman).

## Alcance
- IN: regenerar los dos JSON con el generador, sin editarlos a mano.
- OUT: cambiar el generador, agregar un paso de CI (se propone en el reporte), tocar `flujos.json` (no cambia).

## H1 — Catálogo regenerado
**CA:** Dado el OpenAPI vigente, cuando se corre el generador, entonces los dos archivos quedan idénticos entre sí y solo difieren del comprometido por operaciones reales.
**DoD:** generador dos veces ⇒ `cmp` igual · análisis pegado · CI en verde (incluye frontend por tocar `apps/backoffice`). **Estado:** A MEDIAS

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.M1 | Investigar por qué el diff es grande | causa demostrada | `evidencia/analisis-del-diff.txt` | HECHO |
| H1.M2 | Regenerar con el generador (sin ediciones) | determinista, copias iguales | `cmp` | HECHO |
| H1.M3 | Push, PR contra `dev`, CI verde (frontend incluido), aviso | PR verde | checks del PR | A MEDIAS |
