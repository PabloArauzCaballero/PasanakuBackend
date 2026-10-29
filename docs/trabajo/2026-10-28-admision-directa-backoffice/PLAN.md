# Plan — Backoffice decide admisiones + quitar pestañas e ícono de cabecera

- Fecha: 2026-10-28 · Repos afectados: monorepo Pasanaku (servicios/grupos, apps/backoffice, packages/ui) · Predecesor: ninguno
- Rama: `claude/admision-directa-backoffice-20261028` (worktree desde `origin/test`)
- Resultado observable: un operador con `ADMIN_PLATAFORMA` abre una solicitud de ingreso PENDIENTE sin propuesta del
  organizador y la acepta o rechaza desde el backoffice; las pantallas del backoffice ya no muestran la barra de
  pestañas de sección ni el ícono al lado del título.
- Kill-test: abrir en TEST la solicitud `6324f76a…` (0 decisiones) como operador → si no aparece el formulario o el
  POST falla, no está hecho.

## Hallazgos (descubrimiento)
- `rutas/admisiones/pantalla-de-admision.ts:39-55`: el formulario solo aparece con `propuestaVigente()`; sin historial
  muestra «El administrador del grupo todavía no propuso…».
- `CU68AceptarIngreso.java:120-126` y `sql/40_reglas/restricciones.sql:2033` (`ck_decision_ingreso_propuesta`): una
  RESOLUCION exige propuesta. Control de cuatro ojos: NO se toca.
- TEST corre con `ADMISION_HUMANA_OBLIGATORIA=false` (`scripts/generar_compose.py:246`): el organizador decide directo
  (`POST /grupos/solicitudes/{id}/decision`, `RutasDeSolicitudes.java:101`) y nunca hay propuesta.
- Base TEST (lectura): 2 solicitudes PENDIENTE, 0 filas en `grupos.decision_ingreso`.
- Barra de pestañas: `layout/pestanas-de-seccion.ts`, montada en `layout/shell-financiero.ts:75`.
- Ícono: `packages/ui/src/cabecera-de-pagina/cabecera-de-pagina.ts:24` (`.loseta`); `apps/web` no lo usa (0 usos con `icono`).

## Alcance
- IN: decisión directa de backoffice por la puerta existente `decidirSolicitudDeIngreso`; formulario directo en la
  pantalla de admisión; quitar `<ap-pestanas-de-seccion>` del shell; quitar la loseta e input `icono` de
  `CabeceraDePagina` y sus usos en backoffice.
- OUT: cambiar la restricción de cuatro ojos; flujo con el interruptor encendido (sigue igual); borrar el componente
  `pestanas-de-seccion` (queda sin uso, se anota como deuda); el resto de pantallas del backoffice (auditoría aparte).
- Ambigüedades registradas:
  - «Decisión directa» elegida por Pablo (2026-10-28) entre tres opciones: sin propuesta, backoffice decide por la
    puerta directa; con propuesta, sigue el flujo de cuatro ojos.
  - Ícono: la captura tacha el ícono de UNA pantalla; se asume que aplica a toda cabecera de página del backoffice
    (consistencia). Confirmar con Pablo.

## H1 — El operador de backoffice puede decidir una admisión sin propuesta
**CA:** Dada una solicitud PENDIENTE sin decisiones y el interruptor apagado, cuando un operador `ADMIN_PLATAFORMA` la
acepta o rechaza con motivo, entonces queda APROBADA/RECHAZADA, con cupo reservado al aceptar, y la pantalla lo confirma.
**DoD:** tests de `grupos` y de backoffice en verde + ejercicio real en TEST.
**Estado:** EN CURSO — falta H3.S1.M2 (despliegue y ejercicio en TEST)

### H1.S1 — Backend
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `OrganizadorDecideIngreso`: backoffice (`ADMIN_PLATAFORMA`/`BACKOFFICE`) pasa la guardia de organizador; nadie decide su propia solicitud | backoffice acepta; el propio solicitante con rol backoffice es rechazado | `./gradlew :servicios:grupos:test --tests '*CU68AceptarIngresoTest'` verde | HECHO |
| H1.S1.M2 | `decidirSolicitudDeIngreso` abre con `@PermisoAlternativo("ADMIN_PLATAFORMA")` + doc del contrato | 200 con ADMIN_PLATAFORMA; 403 participante sigue | `./gradlew :servicios:grupos:test --tests '*GruposSolicitudesWebTest'` verde | HECHO |

### H1.S2 — Frontend
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Dominio: `crearDecidirDirecto()` → `POST /grupos/solicitudes/{id}/decision` con clave | el POST lleva la clave y el cuerpo del contrato | `yarn vitest run` del spec de admisión verde | HECHO |
| H1.S2.M2 | Pantalla: sin historial y con permiso, se ofrece el formulario directo; con 422 AP-CU68-15 se explica que hay que esperar la propuesta | spec verde con los tres niveles (correcto/límite/inválido) | `yarn workspace backoffice vitest run rutas/admisiones` verde | HECHO |

## H2 — Sin pestañas de sección ni ícono de cabecera
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Quitar `<ap-pestanas-de-seccion>` del shell | el shell no renderiza la barra | spec del shell verde | HECHO |
| H2.S1.M2 | Quitar loseta/`icono` de `CabeceraDePagina` y sus usos | ninguna cabecera de página tiene `.loseta` | spec de cabecera verde + `tsc`/build de backoffice OK | HECHO |

## H3 — Verificación e integración
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Typecheck + tests backoffice y ui | 0 errores | salida pegada en evidencia/ | HECHO |
| H3.S1.M2 | PR a `test` y `dev`, desplegar TEST, ejercitar en navegador | solicitud resuelta en TEST + captura | captura + consulta a la base | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| `afuera.reputacion()` rechaza el token de backoffice | 500/403 al decidir | se verifica en TEST; si falla, se registra |
| Login en TEST desde el agente lo bloquea el clasificador | no se puede ejercitar por API | se ejercita por el navegador de Pablo |
