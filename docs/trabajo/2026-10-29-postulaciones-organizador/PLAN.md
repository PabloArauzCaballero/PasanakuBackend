# Plan — Bandeja de postulaciones a organizador en el backoffice (CU-90)

- Fecha: 2026-10-29 · Repos afectados: monorepo Pasanaku (apps/backoffice) · Predecesor: `2026-10-28-admision-directa-backoffice` (H3.S1.M2 esperando el despliegue de TEST, fuera de mi control)
- Rama: `claude/postulaciones-organizador-20261029` (desde `origin/test` 310124ed)
- Resultado observable: un operador `ADMIN_PLATAFORMA` entra a Operación › «Postulaciones a organizador», ve la bandeja,
  abre una postulación y la aprueba o rechaza con motivo; si ya está aprobada, la confirma o la revoca.
- Kill-test: con una postulación PENDIENTE en la base, no aparece en la bandeja o el botón no la resuelve → no está hecho.

## Hallazgos
- Backend completo y sin pantalla: `servicios/organizador/.../web/RutasDeHabilitacion.java:38-88`, todos `@Permiso("ADMIN_PLATAFORMA")`.
  `GET /organizadores/postulaciones/bandeja` (cursor `despuesDeFecha`/`despuesDeId`, `estados`, `limite` ≤ 100),
  `GET …/{id}/expediente`, `POST …/{id}/resolucion` (APROBAR/RECHAZAR), `POST …/{id}/revision` (CONFIRMAR/REVOCAR),
  ambos con `Idempotency-Key` y `revisionEsperada` (`organizador.yaml:165-270`).
- Cada fila trae `acciones: [RESOLVER|REVISAR]`: la UI ofrece solo lo que el servidor declara.
- Tipos generados disponibles: `clientes/angular/organizador/model/{paginaDeBandeja,resumenDeHabilitacion,expedienteDeHabilitacion,decisionDeHabilitacion,entradaResolucionDeHabilitacion,entradaRevisionDeHabilitacion}.ts`.
- Patrón a copiar: `rutas/admisiones/` (bandeja + pantalla de expediente + contenedor smart y vista dumb del formulario), `nucleo/menu.ts:93`, `nucleo/secciones.ts:53`, `app.routes.ts:95`.
- TEST 2026-10-29: 0 filas en `organizador.solicitud_organizador`.

## Alcance
- IN: ruta `/postulaciones-organizador` (bandeja + expediente), permiso de sección `ver:postulaciones` → `ADMIN_PLATAFORMA`, entrada en el menú de Operación.
- OUT: `aprobarPostulacion` (camino viejo, `organizador.yaml:268`); campo `medidos` (opcional, sin fuente en la UI); la pantalla vieja `cumplimiento/organizadores/:id/habilitacion` (ítem 1 del backlog, aparte); backend.
- Ambigüedades: ninguna de negocio; el contrato fija las reglas.

## H1 — El operador resuelve postulaciones desde el backoffice
**CA:** Dada una postulación PENDIENTE, cuando el operador la abre y la aprueba con motivo, entonces el servidor recibe `POST …/resolucion` con `{decision, motivo, revisionEsperada}` y clave, y la pantalla muestra la decisión registrada.
**DoD:** specs dirigidos + menú⇔guardias + typecheck en verde; ejercicio en TEST cuando haya una postulación.
**Estado:** A MEDIAS — ver REPORTE.md

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Dominio `cu90-postulaciones.ts`: bandeja, expediente, resolver, revisar | URLs, cuerpos y clave según contrato | spec del dominio verde | HECHO |
| H1.S1.M2 | Bandeja (tabla + filtro por estado + paginación por cursor) | lista, vacío, error y carga | spec de la bandeja verde | HECHO |
| H1.S1.M3 | Expediente + formulario (RESOLVER o REVISAR según `acciones`) | correcto / límite (motivo 1000) / inválido (AP-CU90-04 autoaprobación, AP-CU90-08 revisión) | spec de la pantalla verde | HECHO |
| H1.S1.M4 | Ruta, permiso de sección y entrada de menú | menú⇔guardias consistente | `menu-y-guardias.spec.ts` verde | A MEDIAS |
| H1.S1.M6 | (agregada) Traducciones AP-CU90-03/04/05/07/08 en `nucleo/errores.ts` | el mensaje del servidor no se muestra crudo | spec de la pantalla (AP-CU90-04) | HECHO |
| H1.S1.M5 | Typecheck + regresión de backoffice | 0 errores nuevos | salida en evidencia/ | HECHO |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| TEST sin postulaciones | no se puede ejercitar la resolución real | se ejercita contra HTTP simulado (declarado) y se registra; crear una postulación requiere un usuario participante real |
