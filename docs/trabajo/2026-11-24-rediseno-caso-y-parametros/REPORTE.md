# Reporte — Rediseño visual del Caso de cumplimiento (CU-44) y de Parámetros financieros (CU-132)

> **AVANCE: 5 / 5 — 100 %.**

- Fecha: 2026-11-24 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/visuales-backoffice-20261124` (sin push)
- Peldaño de evidencia alcanzado: `VERIFIED_FUNCTIONAL_ONLY` + prueba visual inspeccionada (3 viewports × 2 temas) contra
  el simulado Prism y red interceptada por Playwright. No hay backend real en este trabajo (sin cambios de contrato).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Capturas ANTES guardadas | `cp` tras los dos E2E | PASS · `../2026-11-23-continuar-traspaso/evidencia/backoffice-antes/` |
| H1.S1.M2 | `GrupoRadio` con `variante="tarjetas"` (radiogroup nombrado, default intacto) | `ng test src/grupo-radio` (packages/ui) | PASS 2/2; suite ui 233/233 |
| H1.S1.M3 | Caso: resumen (código, paso en curso, causal, notificación, plazo, huecos del contrato), riel lateral en ancho y escalera plegable en angosto, causal como tarjetas | `yarn playwright test e2e/caso-de-cumplimiento.e2e.ts` | PASS 18/18 |
| H1.S2.M1 | Parámetros: «Rige hoy» / «Espera aprobación» (cuatro ojos) / historial en tabla que colapsa a tarjetas; estados cargando, vacío, error con reintento | `yarn playwright test e2e/visual-parametros-financieros.e2e.ts` | PASS 10/10 |
| H1.S3.M1 | Regresión y gates | ver Evidencia | PASS |

## A medias
ninguna

## Pendiente
ninguna

## Evidencia
```text
yarn typecheck (apps/backoffice)              → exit 0
yarn lint (apps/backoffice)                   → All files pass linting. · verificar_frontend backoffice: TODO OK
yarn typecheck / lint (packages/ui)           → exit 0 · TODO OK
yarn test:front (apps/backoffice)             → Test Files 189 passed · Tests 1358 passed
yarn test:a11y  (apps/backoffice)             → Test Files 46 passed · Tests 97 passed
yarn test:front (packages/ui)                 → Test Files 40 passed · Tests 233 passed
yarn build (apps/backoffice)                  → exit 0 (avisos: NG8113 en alertas/abrir-caso.ts y presupuesto inicial 503,30 kB > 500 kB)
e2e/caso-de-cumplimiento.e2e.ts               → 18 passed
e2e/visual-parametros-financieros.e2e.ts      → 10 passed
e2e/accesibilidad.e2e.ts                      → 1 passed
e2e/f2-posiciones-y-roles.e2e.ts              → 10 passed
e2e/tableros-por-rol.e2e.ts                   → 16 passed
e2e/tablero-y-permisos.e2e.ts                 → 8 passed
e2e/shell.e2e.ts                              → 3 passed
```
Capturas después: `../2026-11-23-continuar-traspaso/evidencia/backoffice-rediseno/` (caso) y `.../backoffice-rediseno/parametros/`.
Contraste calculado: `--text-2` oscuro (#9A9AA3) sobre `--brand-bg` oscuro (#164A30) daba 3,66:1; se cambió a `--text` en
la tarjeta elegida y en el panel «Rige hoy».

## No cubierto
- El diálogo de decisión del caso (`DecidirCaso`) no se rediseñó ni se capturó.
- No se capturó el estado «sin permiso» de parámetros: lo resuelve la guardia `canMatch` de la ruta (sin cambios).
- `yarn format`: el backoffice no declara script de formato y la línea base no está formateada con Prettier; no se corrió
  `--write` para no reformatear archivos enteros.
- No se verificó si el aviso de presupuesto inicial del build ya existía antes de este cambio.
- Lector de pantalla real: no ejercitado; solo axe (unitario) y roles por Playwright.

## Desvíos del plan
- Las capturas «inicial» del caso del E2E original salían del **tablero** (la URL cambiaba con `pushState` antes de que el
  Router pintara): `TEST_BUG`. Se agregó la espera del `h1` «Caso de cumplimiento» en `entrarConPermiso` (refuerza, no debilita).
- Se agregaron 2 pruebas de disposición al E2E del caso y 3 capturas de estado al de parámetros.

## Riesgos residuales
- `GrupoRadio` es compartido (39 usos): el cambio es aditivo; suites completas en verde.

## Decisiones y ambigüedades
- El contrato de CU-44 no publica alerta de origen ni titular, y `VersionDeParametro` no publica quién propuso/aprobó ni
  cuándo se propuso. No se inventaron: la pantalla dice que el contrato no los publica. Confirmar con Pablo si se agregan.
