# Plan — Llevar el rediseño del backoffice al repo que despliega Coolify

- Fecha: 2026-10-27 · Repos afectados: PasanakuBackend (destino), PasanakuFrontend (origen, solo lectura) · Predecesor: `PasanakuFrontend/docs/trabajo/2026-10-26-backoffice-listados-y-navegacion` (PR PasanakuFrontend#42)
- Resultado observable: quien abre el backoffice de test (Coolify, construido desde `PasanakuBackend@test`) ve el rediseño de #42 (kit de listados, menú de 7 entradas, íconos) sin perder los arreglos de backoffice hechos sólo en PasanakuBackend.
- Kill-test: `git grep -l crearListado origin/test -- apps/backoffice` en PasanakuBackend devuelve 3 archivos (hoy) → no está hecho.

## Alcance
- IN: merge de `PasanakuFrontend@dev` sobre `PasanakuBackend@dev` en `claude/inspiring-ritchie-x93xz4`; resolución de conflictos; typecheck, lint, pruebas y build del backoffice y de `packages/ui`; PR a `dev` y PR `dev` → `test`.
- OUT: cambios de comportamiento nuevos; servicios Java salvo lo que el merge traiga sin conflicto; la configuración de Coolify.
- Ambigüedades registradas:
  - Cuál de los dos repos es la fuente de verdad del frontend. Supuesto: PasanakuBackend, porque es el que despliega Coolify. Confirmar con Pablo.
  - En conflictos de backoffice/ui gana el lado del rediseño (#42) y se portan los arreglos que sólo existen en Backend. En conflictos fuera del frontend (sql, servicios, scripts) gana el lado de Backend. Confirmar con Pablo.

## H1 — El backoffice de test muestra el rediseño
**CA:** Dado el backoffice de test redesplegado desde `PasanakuBackend@test`, cuando un operador entra, entonces ve el menú nuevo y los listados con el kit.
**DoD:** PRs mergeados a `dev` y `test`; build verde; redeploy (lo hace Pablo) y captura.
**Estado:** EN CURSO

### H1.S1 — Integración
| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Merge de `fe/dev` sin marcadores de conflicto | 0 archivos en conflicto | `git diff --name-only --diff-filter=U \| wc -l` → 0 y `git grep -n '^<<<<<<<'` vacío | HECHO |
| H1.S1.M2 | Typecheck del backoffice y de ui | tsc sale 0 | `npx tsc -p apps/backoffice/tsconfig.app.json --noEmit` → exit 0 | HECHO |
| H1.S1.M3 | Lint | sin errores | lint del backoffice → exit 0 | HECHO |
| H1.S1.M4 | Pruebas unitarias backoffice + ui | 0 fallos | `ng test` → todos PASS | HECHO |
| H1.S1.M5 | Build de producción del backoffice | build sin errores | `ng build backoffice` → exit 0 | HECHO |

### H1.S2 — Entrega
| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S2.M1 | PR a `dev` en PasanakuBackend | PR abierto y mergeado | URL del PR + `merged:true` | EN CURSO |
| H1.S2.M2 | PR `dev` → `test` | PR mergeado | URL del PR + `merged:true` | TODO |
| H1.S2.M3 | Verificación en el despliegue | menú nuevo visible | captura del backoffice de test tras redeploy | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Arreglos de Backend perdidos al tomar el lado del rediseño | Regresión en Admisiones, Sistemas, catálogo | Revisar cada commit de Backend en backoffice desde la base común y portarlo |
| Sin credenciales de operador | No se puede verificar adentro | Pedir usuario de test o capturas |

## Desvíos registrados durante la ejecución
- El merge trajo de PasanakuFrontend actualizaciones de Dependabot (Spring Boot 3.5 → 4.1, openapi-generator 7.14 → 7.26, TypeScript 7, ESLint 10, Vitest 5, Gradle wrapper, pubspec). Rompían la compilación de `buildSrc`. Se dejaron **todas** las dependencias, los workflows y la app móvil como están en PasanakuBackend: el cambio es sólo del frontend web.
- Catálogo de datos (`rutas/tablero/catalogo`): se conservó la versión de PasanakuBackend, que coincide con el contrato de `servicios/auditoria`.
- Admisiones: se conservó la bandeja de PasanakuBackend, que usa `GET /grupos/solicitudes` (no existe en el contrato de PasanakuFrontend). `textos.ts` lleva la unión de los dos lados.
- `menu.ts`: el requisito de Sistemas se alineó con `guardia-rol-sistemas.ts` de Backend (acepta los permisos de catálogo `ADMIN_PLATAFORMA` y `RESPONSABLE_SEGURIDAD`). Lo detectó `menu-y-guardias.spec.ts`.
