# Reporte — Rediseño del backoffice en el repo que despliega Coolify

> **AVANCE: 5 / 8 — 62,5 %.** Integración, typecheck, lint, pruebas y build en verde. Faltan los dos PR (en curso en esta misma sesión) y la verificación en el despliegue, que requiere redeploy y credenciales.

- Fecha: 2026-10-27 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/inspiring-ritchie-x93xz4` (PasanakuBackend)
- Peldaño de evidencia: **TESTED** (typecheck, lint, 1237 + 192 pruebas unitarias y build de producción). No `VERIFIED`: no se recorrió en navegador contra la API real.

## Causa del problema reportado
Coolify construye el backoffice desde **PasanakuBackend**. El rediseño del 26/10 (PasanakuFrontend#42) se hizo y se mergeó en **PasanakuFrontend**, que Coolify no despliega. Por eso el backoffice de test seguía igual.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Merge de `PasanakuFrontend@dev` (234d6389) sobre `PasanakuBackend@dev` (3ed2224f); 68 conflictos resueltos | `git diff --name-only --diff-filter=U` | 0 archivos |
| H1.S1.M2 | Typecheck backoffice y ui | `yarn typecheck` | exit 0 · `evidencia/tc-bo.txt`, `evidencia/tc-ui.txt` |
| H1.S1.M3 | Lint backoffice | `yarn lint` | All files pass linting · TODO OK · `evidencia/lint-bo.txt` |
| H1.S1.M4 | Pruebas unitarias | `yarn test:front` | backoffice 173 archivos / 1237 PASS · ui 38 / 192 PASS · `evidencia/test-*.txt` |
| H1.S1.M5 | Build de producción | `yarn build` | Initial total 498,17 kB · `evidencia/build-bo.txt` |

## A medias
### H1.S2.M1 / H1.S2.M2 — PR a `dev` y de `dev` a `test`
- Qué anda: la rama compila y pasa pruebas.
- Qué no anda: al escribir este reporte los PR no estaban abiertos.
- Qué falta exactamente: abrir y mergear el PR a `dev` y el PR `dev` → `test`.
- Dónde quedó: rama `claude/inspiring-ritchie-x93xz4`.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S2.M3 | TODO | Redeploy del backoffice en Coolify (Pablo) y un usuario operador de test para recorrerlo. |

## Evidencia
```text
backoffice · tsc exit 0
backoffice · All files pass linting. · TODO OK
backoffice · Test Files 173 passed (173) · Tests 1237 passed (1237)
ui         · Test Files 38 passed (38) · Tests 192 passed (192)
backoffice · build Initial total 498.17 kB
```
Primera corrida: 11 fallas. Ocho por falta de `packages/simulado/generado/` (entorno, se generó con `yarn workspace @aportaya/simulado contratos`) y tres de `menu-y-guardias.spec.ts` por el desajuste del menú de Sistemas (corregido en `menu.ts`).

## No cubierto
- Recorrido en navegador contra la API real, pruebas a11y (`test:a11y`) y e2e de Playwright: no se corrieron.
- Lo que el rediseño cambió en pantallas que el Backend también tocó: se tomó el lado del rediseño y no se revisó una por una en navegador.

## Desvíos del plan
Ver la sección «Desvíos registrados» del PLAN.

## Riesgos residuales
- Pantallas que en PasanakuFrontend se rediseñaron contra el doble pueden pedir campos que la API real no devuelve.
- Las ramas de PasanakuFrontend y PasanakuBackend siguen divergiendo: dos copias del mismo frontend.

## Decisiones y ambigüedades
- Fuente de verdad del frontend: se tomó PasanakuBackend porque es el que despliega. **Confirmar con Pablo** si PasanakuFrontend se abandona o se sincroniza.
- Dependabot de PasanakuFrontend no se trajo. **Confirmar con Pablo** si esas actualizaciones se quieren en Backend (en otro PR).
