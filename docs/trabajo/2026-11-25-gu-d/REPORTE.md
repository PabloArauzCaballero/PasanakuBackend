> **AVANCE: 19 / 20 — 95,0 %.**

# Reporte — Gobierno de usuarios · Lote D (backoffice)

- Fecha: 2026-11-25 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/gu-d-backoffice` · PR #524
- Peldaño de evidencia: `TESTED` en todo el alcance; `VERIFIED_FUNCTIONAL_ONLY` en las pantallas (E2E visual con backend simulado por `page.route`, datos sintéticos, 30 capturas 390/834/1440 × claro/oscuro). Contra el contrato real de #516 (ya en `dev`, mergeado en la rama) solo hay verificación por tipos/doble; falta H6.S1.M4 (CI verde + PR listo).

## Completado
| ID | Qué | Comando | Resultado |
|---|---|---|---|
| H1 | Sección «Personal y accesos» por permisos; INTERNAL_IDENTITY_ADMIN entra, RESPONSABLE_SEGURIDAD no la ve | `ng test` nucleo/layout/sistemas | PASS (`evidencia/H1-…`) |
| H2 | Dominio de gobierno, step-up (desafío ADMIN + TOTP → `evidenciaMfa`), errores AP-CU08-xx por campo, `ap-dialogo-de-acto` **compartido** en `rutas/sistemas/compartido/dialogo-de-acto.ts` | idem | PASS (`evidencia/H2-…`) |
| H3/H4/H5 | Usuarios internos (cursor, «Cargar más», BAJA_LABORAL, bloqueo indefinido, suspender/reactivar/desbloquear/baja con motivo+código, alta con step-up), asignar rol por asignación (rol asignable, ámbito, vigencia, justificación), revisión de accesos (informe + revocar), roles-internos | `yarn test:front` | 195 files / 1403 tests PASS (`evidencia/H6-test-front.txt`) |
| H6.S1.M1–M3 | Simulado (ejemplos de B ya en `dev`), E2E visual, capturas | `playwright test e2e/visual-personal-y-accesos.e2e.ts --workers=1` | 30 PASS; consola sin errores nuevos |
| Gates | typecheck, lint, a11y | `yarn typecheck` / `yarn lint` / `yarn test:a11y` | exit 0 / TODO OK / 47 files 98 tests PASS |

Bug de interfaz hallado y corregido con la prueba visual: el menú «Más» de la fila quedaba tapado por las celdas fijas de las filas de abajo y se medía estrecho junto al borde derecho → ahora va en `popover` (capa superior) y se mide en su ancho natural (`usuarios-internos/ubicar-menu.ts`).

## A medias
### H6.S1.M4 — Gates en CI y PR listo
- Qué anda: todo lo anterior en local; el único check rojo de CI de #524 era `adaptador-fuente-no-disponible.spec.ts` (siete→seis puertos al salir el de accesos): corregido y empujado.
- Qué no anda: nada conocido.
- Qué falta: CI verde sobre el último commit, sacar el PR de draft, aviso a la sesión principal.
- Dónde quedó: rama `claude/gu-d-backoffice`, compila.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H6.S1.M5 | TODO | Re-verificar contra el contrato real de #516 con una corrida del backoffice sobre el simulado generado de `dev` (hoy: doble `page.route`). |

## Evidencia
`evidencia/H1-seccion-por-permisos.txt`, `H2-dominio-y-step-up.txt`, `H6-typecheck.txt`, `H6-test-front.txt`, `H6-test-a11y.txt`, `H6-lint.txt`, `capturas/` (3 de 30 revisadas: diálogo móvil oscuro, revisión de accesos móvil claro, usuarios tablet oscuro; las 30 en `.playwright/backoffice/visual/personal-y-accesos/`, gitignored).

## No cubierto
Contra el servidor real de identidad; flujo completo del TOTP con un factor real; 27 de 30 capturas sin inspeccionar una por una (se revisaron las que aportan); lector de pantalla real.

## Desvíos del plan
Ver extensiones en el PLAN (secciones.ts, icono-de-seccion.ts, specs de layout/menu). Otra sesión empujó `0216ff84` a esta rama (accesos contra el informe, specs, `matriz-de-roles-personal.spec.ts`): se integró por merge.

## Riesgos residuales y deuda
- A 834 px los chips de «Estado» se cortan en la columna fija de acciones (la tabla desplaza horizontalmente): comportamiento previo del kit de tablas, no tocado.
- El menú «Más» se cierra con el scroll de la ventana (diseño previo).
- La ficha no lista asignaciones con id (contrato): revocar se hace desde «Revisión de accesos».

## Decisiones y ambigüedades
`TRASPASO.md`/`INVENTARIO.md` no están en el repo; se usaron las decisiones del encargo. Confirmar con la sesión principal.
