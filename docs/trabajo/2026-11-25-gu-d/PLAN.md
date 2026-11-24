# Plan — Gobierno de usuarios · Lote D (backoffice Angular)

- Fecha: 2026-11-25 · Repos afectados: PasanakuBackend (`apps/backoffice`, `packages/simulado/ejemplos/identidad`) · Rama: `claude/gu-d-backoffice` (PR a `dev`) · Predecesor: `2026-11-24-gu-a`, `2026-11-24-gu-e` (mergeados); contrato de B = PR #516 (`claude/gu-b-cu08`, aún sin mergear)
- Resultado observable: una persona con `ACCESOS_ADMINISTRAR` o `USUARIOS_INTERNOS_*` ve la sección propia **«Personal y accesos»** y desde ahí da de alta, suspende, reactiva, desbloquea y da de baja a un empleado, asigna y revoca roles **uno por uno** (ámbito, vigencia, justificación) y revisa los accesos vivos / por vencer / sin TOTP; cada acto pide motivo + segundo factor; quien recibiría 403 no ve la sección.
- Kill-test: entrar con `INTERNAL_IDENTITY_ADMIN` (solo `ACCESOS_ADMINISTRAR`+`USUARIOS_INTERNOS_*`) y no ver «Personal y accesos» / rebotar al abrir la pantalla; o entrar con `RESPONSABLE_SEGURIDAD` y ver la sección (recibiría 403).

## Hallazgos (hechos)
- `TRASPASO.md` / `INVENTARIO.md` citados en el encargo **no están en el repo** (ni en `dev` ni en ninguna rama `gu-*`): se trabaja con las decisiones copiadas en el encargo (ver Ambigüedades).
- La sección está hoy dentro de «Sistemas» (`menu.ts`, grupo «Personal y accesos» con 4 pantallas) y la guardia `soloRolesDeSistemas` (`guardia-rol-sistemas.ts`) solo admite roles `PLATAFORMA/SEGURIDAD` o códigos `ADMIN_PLATAFORMA/RESPONSABLE_SEGURIDAD` ⇒ `INTERNAL_IDENTITY_ADMIN` rebota, y `RESPONSABLE_SEGURIDAD` entra (por `SEGURIDAD_*`) pero el servidor responde 403 (pide `USUARIOS_INTERNOS_LEER`/`ACCESOS_ADMINISTRAR`).
- Permisos reales por endpoint (`UsuariosController`, `RolesController`, `PermisosController`, y `RutasDeAccesos` de B): leer = `USUARIOS_INTERNOS_LEER`|`ACCESOS_ADMINISTRAR` (roles/permisos: `ROLES_INTERNOS_LEER`/`PERMISOS_INTERNOS_LEER`|`ACCESOS_ADMINISTRAR`); mutar = `USUARIOS_INTERNOS_GESTIONAR`|`ACCESOS_ADMINISTRAR`; informe = `ACCESOS_ADMINISTRAR`|`USUARIOS_INTERNOS_LEER`.
- Step-up ya existe en identidad (`POST /sesiones/desafios` → `POST /sesiones/desafios/{id}/verificacion` con `factor{tipo:TOTP,valor}` → `{evidencia}`); el backoffice no lo usa todavía.
- El cliente generado `clientes/angular/identidad` sale de `identidad.yaml` de `dev`, que **no** tiene las operaciones nuevas de B. Para no depender de B: tipos y llamadas HTTP de las operaciones nuevas viven en `rutas/sistemas/dominio/accesos.ts` (doble declarado del contrato de #516); al entrar #516 se migra a los tipos generados.
- `UsuarioInternoDetalle` (B) trae `roles: string[]`, **sin `asignacionId`**; la revocación individual solo es posible desde el informe (`FilaDeAcceso.asignacionId`).

## Alcance
- IN: `apps/backoffice/src/app/rutas/sistemas/{accesos,usuarios-internos,roles-internos}`, `dominio/{usuarios-internos,accesos}.ts`, `textos.ts` (solo claves de estas pantallas), `guardia-rol-sistemas.ts`, `nucleo/menu.ts`, `nucleo/matriz-de-roles*`, `nucleo/explicaciones/explicaciones-sistemas.ts`, ejemplos curados `packages/simulado/ejemplos/identidad` de operaciones nuevas, `apps/backoffice/e2e/` de estas pantallas.
- Extensiones mínimas declaradas (necesarias para que la guardia por permiso funcione; no son «de paso»): `nucleo/secciones.ts` (permisos que abren `ver:sistemas`), `layout/icono-de-seccion.ts` (un trazo para la sección nueva), specs de menú/guardias que enumeran el menú.
- OUT: `servicios/**`, `identidad.yaml`, openapi, SQL (lotes B/C). Cualquier cambio de contrato se pide a la sesión principal.
- Ambigüedades: (1) TRASPASO/INVENTARIO ausentes → se usan las decisiones del encargo; confirmar con sesión principal. (2) La ficha no puede listar asignaciones con id (contrato B) → se pedirá `GET /usuarios-internos/{id}/asignaciones` o `usuarioId` en el informe; mientras, revocar se hace desde «Revisión de accesos». (3) Qué ve quien tiene solo `USUARIOS_INTERNOS_LEER`: pantallas de lectura sin botones de acción (el servidor decide igual).

## H1 — Sección propia «Personal y accesos» abierta por permisos
**CA:** Dado `INTERNAL_IDENTITY_ADMIN`, cuando entra, entonces ve «Personal y accesos» y abre sus pantallas; dado `RESPONSABLE_SEGURIDAD`, entonces no la ve y la URL directa lo devuelve; cada pestaña aparece solo si su permiso real la abre.
**DoD:** `menu-y-guardias.spec`, `menu.spec`, `matriz-de-roles.spec`, `guardia-rol-sistemas.spec` verdes; `yarn typecheck`.
**Estado:** HECHO (peldaño TESTED; verificación en navegador en H6)
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Entrada propia en `menu.ts` con requisitos por pantalla y trazo `personal` | La pestaña X aparece ⇔ el permiso real de X | `vitest menu.spec menu-y-guardias.spec` | HECHO (evidencia/H1-seccion-por-permisos.txt) |
| H1.S1.M2 | Guardia por permiso para las pantallas de personal; `soloRolesDeSistemas` sigue cerrada para el resto | INTERNAL_IDENTITY_ADMIN abre; RESPONSABLE_SEGURIDAD no | `vitest guardia-rol-sistemas.spec` | HECHO (evidencia/H1-seccion-por-permisos.txt) |
| H1.S1.M3 | Matriz de roles: `INTERNAL_IDENTITY_ADMIN`/`RESPONSABLE_SEGURIDAD` esperados + verify | matriz coincide con catálogo | `vitest matriz-de-roles.spec` | HECHO (evidencia/H1-seccion-por-permisos.txt) |

## H2 — Capa de datos nueva y segundo factor (step-up)
**CA:** Dado un acto de gobierno, cuando la persona escribe motivo y el código de su app, entonces el backoffice abre el desafío, lo verifica y manda `evidenciaMfa` con la acción; un código malo muestra el error en el campo y conserva lo escrito.
**DoD:** specs del dominio y del diálogo de step-up en los 3 niveles (correcto/límite/inválido), `test:front`.
**Estado:** TODO
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | `dominio/accesos.ts`: tipos y llamadas (asignar, revocar, baja, informe, roles asignables) + `evidenciaMfa`/`motivo` en alta/actualizar/desbloqueo | el cuerpo HTTP cumple el contrato de B | spec con `HttpTestingController` | TODO |
| H2.S1.M2 | `dominio/paso-de-seguridad.ts`: abrir desafío + verificar TOTP → evidencia | 200 → evidencia; 422 AP-CU04-04 → error de campo | spec 3 niveles | TODO |
| H2.S1.M3 | Mapa de errores AP-CU08-01…12 y AP-CU04-04/06 a mensajes accionables | nunca el crudo | spec | TODO |
| H2.S1.M4 | Componente `ap-campo-segundo-factor` (código 6 dígitos, etiqueta, error) reutilizable | a11y: etiqueta, `aria-describedby`, `inputmode` | spec + a11y | TODO |

## H3 — Usuarios internos: listado por cursor y actos de gobierno
**CA:** Dado el listado, cuando hay >50 usuarios, entonces «Cargar más» pide la página siguiente por cursor; suspender/reactivar/desbloquear/alta/baja piden motivo + código, y el diálogo dice qué pasa (suspender revoca todos los roles; reactivar vuelve sin roles; baja = `BAJA_LABORAL` irreversible); bloqueo indefinido se ve como «Bloqueado sin fecha de fin» con su motivo.
**DoD:** specs de pantalla + mutaciones; E2E con simulado; capturas.
**Estado:** TODO
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Listado por cursor (`limite`,`antesDe`,`antesDeId`), estados BAJA_LABORAL, bloqueo indefinido, 4 estados | segunda página concatena; vacío orienta; error accionable | spec | TODO |
| H3.S1.M2 | Alta con motivo + step-up | falla 422 AP-CU08-09/11 → error en su campo | spec | TODO |
| H3.S1.M3 | Suspender/Reactivar/Desbloquear con motivo + step-up y texto del efecto | idem | spec mutaciones | TODO |
| H3.S1.M4 | Baja del empleado (`POST /baja`) | último administrador / autobaja → error AP-CU08-04/06 accionable | spec | TODO |
| H3.S1.M5 | Ficha: estado, bloqueo, acciones por permiso, enlace a accesos | a11y verde | specs ficha | TODO |

## H4 — Asignación por asignación + selector de roles asignables
**CA:** Dado un usuario, cuando se asigna un rol, entonces se elige rol (solo asignables), ámbito (GLOBAL/GRUPO+id/ORGANIZACION), vigencia opcional, justificación ≥10 y código; PARTICIPANTE/ORGANIZADOR/ANUNCIANTE/SERVICIO_* no se ofrecen; yaExistia y requiereMfa se comunican.
**DoD:** specs 3 niveles; E2E.
**Estado:** TODO
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H4.S1.M1 | Reemplazar `modal-asignar-roles` (PUT deprecado) por `modal-asignar-rol` | cuerpo = `EntradaAsignacionDeRol` | spec | TODO |
| H4.S1.M2 | `GET /roles?asignablesAPersonalInterno=true` en el selector y en la pantalla de roles | no hay roles de participante | spec | TODO |

## H5 — Revisión de accesos (GU-08/GU-09)
**CA:** Dado el informe, cuando elijo VIVOS / POR_VENCER / SIN_TOTP, entonces veo la página con totales; puedo revocar una asignación con motivo ≥10, código y «cerrar sesiones».
**DoD:** spec + E2E + capturas.
**Estado:** TODO
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H5.S1.M1 | `pantalla-accesos` contra `GET /usuarios-internos/accesos/informe` (3 secciones, paginación por número) | 4 estados | spec | TODO |
| H5.S1.M2 | Revocar asignación (diálogo con motivo, cerrar sesiones, step-up) | AP-CU08-06 mapeado | spec | TODO |

## H6 — Doble (simulado), E2E, prueba visual, gates, entrega
**CA:** capturas 390/834/1440 × claro/oscuro inspeccionadas; consola y red sin errores nuevos; CI verde; PR a dev.
**DoD:** `yarn typecheck`, `lint`, `test:front`, `test:a11y`, E2E de las pantallas (1 worker), verify matriz; REPORTE con peldaño.
**Estado:** TODO
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H6.S1.M1 | Ejemplos curados del simulado para operaciones nuevas (ok/límite/inválido) | cumplen el contrato de B | `simulado` tests | TODO |
| H6.S1.M2 | E2E de personal y accesos | un worker, sin errores de consola | playwright | TODO |
| H6.S1.M3 | Capturas y revisión | 6 viewports×tema inspeccionados | `evidencia/` | TODO |
| H6.S1.M4 | Gates + REPORTE + PR | CI verde | salidas pegadas | TODO |

## Simulación (regla 65)
Todo lo de B se construye contra un doble: tipos/llamadas locales + ejemplos curados del simulado. **Verificado solo contra el doble** hasta que #516 entre a `dev`; entonces se re-verifica contra los ejemplos reales (microtarea H6.S1.M5, TODO).

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Uso de presupuesto corta la sesión | trabajo a medias | commits chicos + PLAN/REPORTE al día |
| Cliente generado sin ops de B | typecheck CI | tipos locales en `dominio/accesos.ts` |
