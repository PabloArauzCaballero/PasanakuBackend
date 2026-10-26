# Reporte — Backoffice: navegación que responde, menú recortado y listados con una sola disciplina

> **AVANCE: 109 / 110 — 99,1 %.** Una microtarea A MEDIAS (precarga, peor caso). Todo lo que muta se verificó **contra dobles** (simulado OpenAPI sin estado + `page.route`): ninguna persistencia real está demostrada.

- Fecha: 2026-10-26 · Plan: [PLAN.md](./PLAN.md) · Guía de franjas: [GUIA-DE-FRANJA.md](./GUIA-DE-FRANJA.md) · Rama: `claude/jolly-mccarthy-uwzef6` (repo PasanakuFrontend)
- Peldaño de evidencia: **VERIFIED contra el doble** en el backoffice (camino real en navegador, red y consola revisadas, persistencia NO real). No llega a `REGRESSION_VERIFIED` contra servicios reales: no hay backend desplegado en este entorno.
- Informes por franja (con microtareas, CA, DoD y evidencia): [a](./franjas/a.md) shell y navegación · [b](./franjas/b.md) Operación · [c](./franjas/c.md) Cumplimiento · [d](./franjas/d.md) Sistemas · [e](./franjas/e.md) Contabilidad y Configuración · [f](./franjas/f.md) Publicidad, Tablero y Admisiones.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H0 (6) | Kit en `@aportaya/ui`: `crearListado` (búsqueda multicampo sin acentos, N filtros, orden, página acotada), `ap-listado` (filtros + CSV/JSON de lo filtrado + tabla + paginación con filas por página, «Ir a» N, Anterior/Siguiente + estados), `ap-acciones-de-fila` (Ver → dominio → Editar → Eliminar, ícono + nombre, Eliminar confirma), `ap-exportar-listado` (RFC 4180, BOM, anti-fórmulas), `ap-boton` con ícono, ~55 íconos | `ng test` en `packages/ui` | 40 archivos / 201 PASS · a11y 9 PASS · `evidencia/H0-ui-test.txt` |
| H1 (12 de 13) | «Sistemas» ya no rebota; menú 10 → 7 entradas, pestañas ≤ 8; visibilidad = guardias; precarga según sesión; botones del shell, campana, ingreso y Salir revisados | ver `franjas/a.md` | sonda 0 rebotes / 0 errores (oficial 30 vistas, PLATAFORMA 43) |
| H2–H6 (86) | Todas las vistas de Operación, Cumplimiento, Sistemas, Contabilidad, Configuración, Publicidad, Tablero y Admisiones con la anatomía del kit, íconos y los tres niveles (aceptado / límite / error) | ver cada `franjas/*.md` | B 135 + 26 a11y · C 142 + 14 · D 149 + 18 · E 214 + 13 · F 174 + 13; recorridos en navegador 0 FAIL |
| H7.M1 | Integración de las 6 ramas | `git merge --no-ff` ×6 | sin conflictos |
| H7.M2 | Regresión unitaria, a11y, typecheck, lint, build | `evidencia/H7-regresion-backoffice.txt` | backoffice 170 / **1218 PASS**, a11y 45 / 92 PASS, tsc 0, lint OK, build 446,66 kB sin advertencias · ui 201 + 9 PASS |
| H7.M3 | e2e completo del backoffice, sin reintentos | `npx playwright test --retries=0` | **166 passed (5.7m)**, 0 fallos, tras los arreglos de la revisión · `evidencia/H7-e2e.txt` |
| H7.M4 | Sonda de navegación después | `ROL=… node e2e/apoyo/sonda-de-navegacion.mjs` | `evidencia/sonda-despues-*.json` |
| H7.M5 | Revisión independiente (frontend-reviewer) y corrección de sus hallazgos B1, I1, I2, I3, I6, M1, M2 | commit `dc44f29d` | specs nuevos de ida y vuelta del cursor, CSV con monedas y exportación sin permiso |

## A medias

### H1.S1.M5 — Precarga de rutas (peor caso)
- Qué anda: con 1,5 s de reposo tras entrar, clic → vista pintada baja de 984 ms a **438 ms** de mediana (build de producción, red 150 ms, CPU ×4) y no se descarga código al hacer clic.
- Qué no anda: con un clic **inmediato** tras entrar, el máximo sube de 1418 a 2413 ms (los chunks precargados compiten con el del clic).
- Qué falta exactamente: elegir entre (a) aceptarlo, (b) precargar solo la pantalla de aterrizaje de cada entrada, (c) precargar por intención (hover/foco); implementar y volver a medir con `e2e/apoyo/medir-latencia-del-menu.mjs` contra el build servido.
- Dónde quedó: `apps/backoffice/src/app/nucleo/` (`PrecargaSegunSesion`), rama integrada, compila y sus 4 specs pasan.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| Cola de solicitudes escaladas (D-15) | BLOQUEADO (DECISION_REQUIRED) | Operación nueva en `grupos.yaml`: decisión de backend/negocio. La vista lo dice y deriva a Admisiones. |
| Baja definitiva de usuario interno | BLOQUEADO (contrato) | `DELETE` en identidad; hoy «Suspender» con motivo. |
| Editar elemento de catálogo operativo | BLOQUEADO (contrato) | Solo existe activar/desactivar con motivo. |
| Duplicación de motores de listado por cursor (5) | TODO | Unificar en `packages/ui` (deuda I5 de la revisión). |
| Filtros en URL y selección múltiple (Reclamos, Reversos, Desembolsos, Cumplimiento) | TODO / decisión | Sincronizar con query params o aprobar el selector simple (I4). |

## Evidencia

```text
backoffice · tsc exit 0 · ng lint: All files pass linting. · verificar_frontend.py: TODO OK
backoffice · Test Files 170 passed (170) · Tests 1218 passed (1218)
backoffice · a11y Test Files 45 passed (45) · Tests 92 passed (92)
backoffice · build Initial total 446.66 kB (0 advertencias)
ui         · Test Files 40 passed (40) · Tests 201 passed (201) · a11y 9 passed · tsc 0 · lint OK
e2e        · 166 passed (5.7m) · --retries=0
sonda      · {"rol":"oficial","vistas":30,"rebotes":0,"lentas":0,"errores":0}
sonda      · {"rol":"PLATAFORMA","vistas":43,"rebotes":0,"lentas":0,"errores":0}
```

Capturas (1280 / 768 / 375 claro + 1280 oscuro) y recorridos de navegador por franja en `evidencia/franja-*/`.

## No cubierto
- Persistencia real (`UI → request → response → persistencia → recarga → UI`): todo contra el simulado sin estado y `page.route`. Sin asientos, idempotencia del servidor ni cuadre (regla 91.6).
- Lector de pantalla real (solo axe y teclado); solo Chromium.
- Capturas de diálogos y formularios a 375/768 en varias secciones.
- `menu-y-guardias.spec.ts` falló una vez en una corrida completa y no se reprodujo en 3 corridas: sin causa demostrada.

## Desvíos del plan
- 6 subagentes en paralelo con un navegador cada uno (regla 70 pide 1): pedido explícito de Pablo.
- En listados paginados por cursor sin búsqueda en el contrato, el buscador y la descarga actúan sobre **la página cargada** (lo dice la ayuda del buscador y el nombre del archivo), no sobre todo.
- Filtros de chips múltiples pasaron a selectores simples (Operación y Cumplimiento).
- Los e2e reescriben capturas versionadas de `docs/trabajo/2026-10-08-*` y `evidencia/`: se restauraron después de cada corrida, no se commitean.

## Riesgos residuales
- Exportación en el navegador sin huella ni tope del servidor en las secciones no sensibles (A3, abajo).
- Bundle inicial 446,66 kB (umbral de aviso 460 kB).
- `buildSrc` no compila con `openapi-generator 7.26.0` (bump de Dependabot `e10b3399`, línea 42 de `aportaya.openapi.gradle.kts`): los clientes se generaron localmente con 7.14.0 sin commitear ese cambio. El CI de clientes probablemente está en rojo en `dev`.
- Angular CLI exige Node ≥ 22.22.3; el entorno cloud trae 22.22.0 (se usó Node 24.15).

## Decisiones y ambigüedades (a confirmar con Pablo)
1. **A1 recorte del menú**: tabla antes/después con la razón de cada recorte en `franjas/a.md`. Sale «Mi perfil» (no hay pantalla). `ADMIN_PLATAFORMA` deja de ver Sistemas (su guardia ya lo rebotaba).
2. **A2 Editar/Eliminar**: solo donde el contrato tiene la operación; nunca Eliminar sobre registros financieros, de cumplimiento o auditoría. Resultado: Editar real en Usuarios internos y Catálogo de datos; el resto usa acciones de dominio con ícono + nombre.
3. **A3 exportación CSV/JSON en el navegador**: libre en listados operativos no sensibles; **detrás de `exportar:<recurso>`** en Cumplimiento, Verificación de identidad, Usuarios internos y Bitácora (hallazgo B1 de la revisión, regla 90.2). Confirmar si esas secciones exportan solo por CU-58 o si se registra un evento de auditoría antes de bajar.
4. `verificaciones` exige ahora `VERIFICACION_RESOLVER` (antes bastaba la sección): un auditor de solo lectura pierde la vista. El contrato no declara el permiso de `listarVerificaciones`.
5. Publicidad: carga de a 100, consumo sin moneda («moneda no registrada»), identificadores de segmento y espacio a mano (A-F1..F6 en `franjas/f.md`).
6. Precarga: opción (a)/(b)/(c) de H1.S1.M5.
