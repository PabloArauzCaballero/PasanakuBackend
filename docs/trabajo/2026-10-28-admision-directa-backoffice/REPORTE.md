# Reporte — Backoffice decide admisiones + sin pestañas ni ícono de cabecera

> **AVANCE: 7 / 8 — 87,5 %.**

- Fecha: 2026-10-29 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/admision-directa-backoffice-20261028` (desde `origin/test`)
- Peldaño de evidencia alcanzado: backend `TESTED` (integración contra Postgres real) · frontend `TESTED` · TEST sin ejercitar todavía → el trabajo queda en `TESTED`, no `VERIFIED`.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Backoffice (`ADMIN_PLATAFORMA`/`BACKOFFICE`) pasa la guardia de quien decide; nadie decide su propia solicitud | `./gradlew :servicios:grupos:integrationTest --tests '*CU68AceptarIngresoTest'` | PASS 10/10 (3 nuevos) — `evidencia/grupos-tests.txt` |
| H1.S1.M2 | `decidirSolicitudDeIngreso` abre con `@PermisoAlternativo("ADMIN_PLATAFORMA")`; contrato documentado | `./gradlew :servicios:grupos:webTest --tests '*GruposSolicitudesWebTest'` | PASS 5/5 (1 nuevo; 403 del participante sigue) |
| H1.S2.M1 | `crearDecidirDirecto()` → `POST /grupos/solicitudes/{id}/decision` con `Idempotency-Key` | `ng test` admisiones | PASS — `evidencia/vitest-backoffice.txt` |
| H1.S2.M2 | Pantalla: sin propuesta ofrece decidir; estado desde la bandeja; 422 AP-CU68-15 explica que hay que esperar la propuesta | `ng test --include=rutas/admisiones/** --include=layout/shell-financiero.spec.ts` | PASS 42/42, a11y incluidos |
| H2.S1.M1 | Shell sin `<ap-pestanas-de-seccion>` | mismo comando | PASS |
| H2.S1.M2 | `CabeceraDePagina` sin loseta ni input `icono`; 60 pantallas sin el atributo | `ng test` cabecera (ui) | PASS 3/3 — `evidencia/vitest-ui.txt` |
| H3.S1.M1 | Typecheck backoffice y ui | `yarn typecheck` | EXIT 0, 0 errores |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H3.S1.M2 | TODO | Merge a `test`, reconstrucción en el VPS y ejercicio en el navegador sobre la solicitud pendiente de TEST |

## Evidencia
```text
<testsuite name="bo.aportaya.grupos.CU68AceptarIngresoTest" tests="10" skipped="0" failures="0" errors="0"
<testsuite name="CU-68 · decisión del organizador sobre una solicitud de ingreso" tests="5" skipped="0" failures="0" errors="0"
admisiones + shell:  Test Files 6 passed (6) · Tests 42 passed (42)
cabecera (ui):       Test Files 1 passed (1) · Tests 3 passed (3)
regresión backoffice (sin a11y): Tests 12 failed | 1252 passed (1264)  → clasificados abajo
menu-y-guardias aislado, árbol limpio:   Tests 87 passed (87) · 10.34s
menu-y-guardias aislado, con el cambio:  Tests 87 passed (87) · 10.60s
entrada-estricta tras generar packages/simulado/generado: pasa (96/97 en la corrida de los dos archivos; el 1 era menu-y-guardias)
```
Base TEST (solo lectura, antes del cambio): 2 solicitudes `PENDIENTE`, 0 filas en `grupos.decision_ingreso`.

## No cubierto
- Ejercicio real en TEST (backend desplegado + navegador). El login por API desde el agente lo bloqueó el clasificador de permisos.
- La llamada a `transparencia` (`afuera.reputacion`) con token de backoffice: en el test web está simulada.
- `apps/web` no usa `icono` en la cabecera (0 usos): no se verificó visualmente.
- Prueba visual en tres viewports y dos temas del shell sin pestañas: pendiente con H3.S1.M2.

## Desvíos del plan
- Los specs de la pantalla esperaban `whenStable()` con el pedido nuevo a la bandeja sin responder y se colgaban: se ajustaron para responderlo (no se debilitó ninguna aserción).
- El caso «vacío» del spec cambió por requisito: antes afirmaba que sin propuesta NO se ofrecía decidir (el defecto reportado); ahora afirma que se ofrece.
- Regla 70 (1 subagente): se lanzaron 2 agentes de auditoría de solo lectura en paralelo por pedido explícito de Pablo.

## Riesgos residuales
- La bandeja trae como mucho 200 solicitudes: una solicitud más vieja aparece con estado desconocido y se ofrece decidir; el servidor la rechaza con AP-CU68-09 si ya estaba resuelta.
- `layout/pestanas-de-seccion.ts` queda sin uso (deuda; no se borró).
- 12 rojos de la regresión completa: 10 `ENVIRONMENT` (faltaba `packages/simulado/generado/`, no versionado), 2 `ENVIRONMENT` por plazo de 5 s bajo carga (aislado pasa igual con y sin el cambio).

## Decisiones y ambigüedades
- «Decisión directa» elegida por Pablo (2026-10-28): sin propuesta, backoffice decide por la puerta directa; con propuesta, sigue el flujo de cuatro ojos. No se tocó `ck_decision_ingreso_propuesta`.
- Ícono: la captura tacha el de una pantalla; se quitó en todas las cabeceras de página del backoffice. Confirmar con Pablo.
- Seguridad (90.6): amenaza = operador que se admite a sí mismo u organizador ajeno decidiendo; control = `exigirQuienDecide` (`OrganizadorDecideIngreso.java`), test `backofficeNoDecideLaSuya` y `ajenoNoDecide` en verde.
