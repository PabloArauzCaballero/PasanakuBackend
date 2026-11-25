> **AVANCE: 7 / 20 — 35,0 %.**

# Reporte — Gobierno de usuarios · Lote D (backoffice) — TRASPASO A MEDIAS

- Fecha: 2026-11-25 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/gu-d-backoffice` (PR draft a `dev`)
- Peldaño de evidencia: H1 y H2 = `TESTED` (specs en verde, ver `evidencia/`). H3/H4 = `WRITTEN` (código escrito, **specs en rojo**, typecheck de app OK). H5/H6 = sin empezar.
- Todo lo de B (#516) se construyó contra un doble (tipos locales + HttpTestingController). #516 ya está en `dev` según la sesión principal: **falta mergear `origin/dev` y re-verificar contra el contrato real** (los ejemplos reales de `packages/simulado/ejemplos/identidad`).

## Completado
| ID | Qué | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1–M3 | Sección propia «Personal y accesos» por permisos; guardia por permiso; matriz de roles | `yarn ng test --include='src/app/{nucleo,layout}/*.spec.ts' --include='src/app/rutas/sistemas/*.spec.ts'` | 31 files / 321 tests PASS (`evidencia/H1-seccion-por-permisos.txt`) |
| H2.S1.M1–M4 | `dominio/gobierno-de-accesos.ts`, `paso-de-seguridad.ts`, `errores-de-gobierno.ts`, `usuarios-internos/dialogo-de-acto.ts` (reusa `ap-campo-otp`) | `yarn ng test --include=…/gobierno-de-accesos.spec.ts --include=…/dialogo-de-acto.spec.ts` | 11 + 5 PASS (`evidencia/H2-dominio-y-step-up.txt`) |

## A medias
### H3 — Usuarios internos (listado por cursor, actos de gobierno, ficha) y H4 — asignar rol por asignación
- **Qué anda:** código escrito y `tsc -p tsconfig.app.json` sin errores: `gestor-de-usuario-interno.ts` (editar sin step-up; suspender/reactivar/desbloquear/baja con `ap-dialogo-de-acto`, efecto dicho en claro), `modal-alta-usuario-interno.ts` (motivo + step-up, roles de `?asignablesAPersonalInterno=true`), `modal-asignar-rol.ts` (reemplaza al PUT `/roles`; se borró `modal-asignar-roles.ts`), `pantalla-usuarios-internos.ts` (cursor `limite/antesDe/antesDeId`, «Cargar más», BAJA_LABORAL, bloqueo indefinido, acciones solo si `USUARIOS_INTERNOS_GESTIONAR|ACCESOS_ADMINISTRAR`), `pantalla-usuario-interno-detalle.ts`. `dominio/usuarios-internos.ts` ya no tiene el PUT deprecado y acepta `evidenciaMfa`.
- **Qué no anda:** 22 tests rojos en `src/app/rutas/sistemas/usuarios-internos/*.spec.ts` (mutaciones, alta, detalle, a11y de listado y detalle, y `dialogo-de-acto.spec.ts` aparece fallando en la corrida conjunta — pasaba solo: revisar si es contaminación entre specs o regresión). Causa conocida de la mayoría: los specs esperan `GET /usuarios-internos` sin query y ahora es `GET /usuarios-internos?limite=50`; el alta ya no se confirma sin motivo+código; la ficha/menú de acciones cambió (ya no hay «Roles», hay «Asignar rol» y «Dar de baja»; las acciones piden permiso en `Sesion.permisos()`, los specs abren sesión sin él). **No es regresión de producto confirmada: los specs no se tocaron** (clasificación pendiente, regla 80.4).
- **Qué falta exactamente:** (1) actualizar esos specs a los 3 niveles y a la URL con `?limite=50`, con sesión que traiga `USUARIOS_INTERNOS_GESTIONAR`; agregar specs de: cursor/«Cargar más», BAJA_LABORAL, bloqueo indefinido, baja, asignar rol (rol/ámbito/vigencia/justificación, `yaExistia`, `requiereMfa`). (2) `yarn ng test --include='src/app/rutas/sistemas/usuarios-internos/*.spec.ts'` y `test:a11y` verdes. (3) Marcar H3/H4 HECHO en el PLAN con evidencia.
- **Dónde quedó:** rama `claude/gu-d-backoffice`, commit WIP; app compila, specs de usuarios-internos en rojo.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H5.S1.M1–M2 | TODO | `pantalla-accesos.ts` sigue contra el puerto viejo (`PUERTO_ACCESOS`/`dominio/accesos.ts`). Hay que llevarla a `GET /usuarios-internos/accesos/informe` (3 secciones VIVOS/POR_VENCER/SIN_TOTP, paginación por número; ya existe `informeDeAccesos()` en `gobierno-de-accesos.ts`) y a revocar con `ap-dialogo-de-acto` (`motivoRevocacion` 10–120, `cerrarSesiones`). |
| roles-internos | TODO | Usar `rolesAsignables()` en el listado/selector (la sesión F agrega crear/editar; yo solo el filtro). |
| H6.S1.M1–M5 | TODO | Ejemplos curados del simulado, E2E, capturas 390/834/1440 × claro/oscuro, `yarn typecheck/lint/test:front/test:a11y`, mergear `origin/dev` y re-verificar contra #516, REPORTE final y PR listo. |

## Evidencia
`evidencia/H1-seccion-por-permisos.txt`, `evidencia/H2-dominio-y-step-up.txt`.

## No cubierto
Navegador real, capturas, E2E, lint, a11y de lo nuevo, contra el contrato real de #516, `yarn test:front` completo.

## Desvíos del plan
- Extensiones mínimas fuera de la lista: `nucleo/secciones.ts`, `layout/icono-de-seccion.ts`, specs de `layout/` y `nucleo/menu.spec.ts` (consecuencia de sacar el grupo de Sistemas y crear la entrada `personal`).
- `dominio/usuarios-internos.ts` (capa de datos de estas pantallas) también editado.
- Se creó `ap-dialogo-de-acto` en vez de «campo segundo factor»: se reusó `ap-campo-otp` (regla 95.1).

## Riesgos residuales
- La ficha no puede listar asignaciones con id (contrato B: `UsuarioInternoDetalle.roles` es `string[]`): revocar un rol puntual solo desde «Revisión de accesos». Pedir a la sesión principal `GET /usuarios-internos/{id}/asignaciones` o filtro `usuarioId` en el informe.
- `PUERTO_ACCESOS`/`dominio/accesos.ts` quedarán sin uso al migrar H5.
- Entorno: `corepack` no baja yarn (proxy); se usó `@yarnpkg/cli-dist@4.18.0` por npm (`yarn4`). El cliente Angular se generó con `./gradlew generateOpenApiClients --no-parallel` reintentando por 429. `git checkout -- clientes/dart` y limpiar `packages/simulado/ejemplos` tras `simulado build`.

## Decisiones y ambigüedades
- TRASPASO.md / INVENTARIO.md no están en el repo: se usaron las decisiones del encargo. Confirmar con la sesión principal.
- Motivo mínimo por contrato: alta/asignar/revocar/baja 10, PATCH y desbloqueo 5.
