# Reporte — Backoffice: corregir lo roto y llevarlo al estándar del portal de Atlas (corte H0)

> **AVANCE: 15 / 31 — 48,4 %.** Solo cuenta lo ya desglosado en microtareas (H0, H1, H3.S1/S3, H6); H2, H4.B y H5 todavía no están desglosadas, así que el total real es mayor. **Nada se verificó contra TEST ni contra la API real:** el entorno no alcanza el host de TEST (H1.S1.M1, bloqueado por Pablo). Todo lo visual y de flujo está en `VERIFIED_FUNCTIONAL_ONLY` contra el simulado (Prism).

- Fecha: 2026-10-24 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/keen-cray-d0wpgf` (12 commits sobre `dev` = `test` 040e0bd7)
- PRs: [#187 → dev](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/187) · [#188 → test](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/188) (sin mergear; el merge lo decide Pablo)
- Peldaño de evidencia alcanzado, por área:
  - Generador del catálogo, DDL y scripts: `TESTED`.
  - Servicio `auditoria`: `TESTED` (check + test + webTest + barrido, contra PostgreSQL local; sin `integrationTest`).
  - Backoffice y `@aportaya/ui`: `VERIFIED_FUNCTIONAL_ONLY` (e2e contra Prism) + prueba visual con doble revisión independiente.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H0.S1.M1–M4 | Capa visual del portal por tokens (`data-producto="backoffice"`), solo sobre roles existentes | `yarn workspace @aportaya/tokens build && test:front` | PASS 15/15 · `evidencia/H0-S1/M1-M4-tokens.txt` |
| H0.S1.M5 / H0.S2.M5–M6 | Pestaña «Operaciones» con dos secciones, vacíos que orientan, paginada; doble revisión en 3 rondas | `playwright test catalogo-gobierno` | PASS 8/8 · `evidencia/doble-revision.md`, `H0-S2/` |
| H0.S2.M1–M4 | Operaciones por tabla desde casos de uso + matriz; contrato aditivo; simulado | `test_catalogo_de_datos.py` · `gradlew :servicios:auditoria:check test webTest testBarrido --rerun-tasks` · `simulado test:front` | PASS 13/13 · BUILD SUCCESSFUL (45+74+2) · 350/350 |
| H0.S3.M1–M3 | `borrador_onboarding` generado desde `generar_ddl.py`; aplicable e idempotente | `psql -v ON_ERROR_STOP=1 -f sql/aplicar.sql` ×3 bases | PASS exit 0, 0 `ERROR`, 442 tablas · `evidencia/H0-S3/` |
| H0.S6.M1 | Regresión del backoffice y paquetes | typecheck · lint · `test:front` · `test:a11y` · e2e completo | exit 0 · TODO OK · 948/948 · 78/78 · 168/168 · `evidencia/H0-S6/` |
| F1 | `resource.value()` sin `hasValue()` abortaba el render (8 sitios) | `ng test admisiones` + prueba de mutación | PASS; la regresión falla sin el arreglo · `evidencia/H4/` |
| F2 | `role=status` dentro de `role=table` (6 specs a11y rojas en `dev`) | `yarn test:a11y` | PASS 42/42 archivos |
| F3, F4 | `shell.e2e` al acordeón; lint del explicador de vista | `playwright test shell admisiones` · `yarn lint` | PASS 11/11 · TODO OK |

## A medias
### H0.S4.M1 — Paso de CI de la matriz
- Qué anda: el paso de CI de `test_catalogo_de_datos.py` está; la matriz regenerada es idempotente.
- Qué no anda: falta el paso `matriz_de_pruebas.py` + `git diff --exit-code -- docs/catalogo-de-flujos/matriz.md` en `ci.yml` (la matriz estaba vieja por 3 endpoints y nadie lo notó).
- Qué falta exactamente: agregar ese paso al job de verificación y comprobarlo en CI.
- Dónde quedó: `.github/workflows/ci.yml` sin tocar; compila.

### H0.S6.M2 — PRs mergeables
- Qué anda: los dos PRs existen, sin conflictos con `dev` ni `test`.
- Qué no anda: `mergeable_state = unstable` mientras corren los checks (6 en curso en el momento de escribir esto).
- Qué falta exactamente: esperar el CI y, si algo falla, corregirlo y empujar; pegar la salida literal de `pull_request_read` en `evidencia/`.
- Dónde quedó: `ddd6cf53` empujado.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H0.S4.M2 | DESCARTADO | `title` en celdas recortadas de la tabla virtualizada: Backend ya la reescribió |
| H0.S6.M3 | TODO | Resincronizar el espejo `PasanakuFrontend` desde Backend (28 conflictos en el merge de prueba, se resuelven a favor de lo ya portado) |
| H1.S1.M1 | BLOQUEADO | **Pablo** debe permitir `*.161.97.85.216.sslip.io` en la red del entorno |
| H1.S1.M2–M4 | TODO | Depende de lo anterior (sesión real del operador, recorrido en el backoffice desplegado, commit desplegado) |
| H2 (recorrido botón por botón) | TODO | Depende de H1 |
| H3.S1, H3.S3 | TODO | Los 24 dominios sin `ap-estado-de-pantalla`; textos de vacío; regla en `verificar_frontend.py` que impida otro `value()` sin guarda |
| H4.B.M1–M4 | TODO | Backend real para `GET /grupos/solicitudes`, `GET /grupos/solicitudes-escaladas`, `GET /publicidad/socios-comerciales/{id}` y el progreso de tutoriales |
| H4.B.M5 | BLOQUEADO (`DECISION_REQUIRED`) | Pablo: `POST /extraccion/*` ¿se mapea a `auditoria` (recomendado) o se crea el servicio `extraccion`? |
| H4.C, H4.D | TODO | Inventario de operaciones real; reservas visuales R1–R10, migas y banner |
| H5 (20 bloques de Atlas) | TODO | Se abren de a uno, con su backend real |
| H6.S1.M1–M4 | TODO | Regresión final, capturas de todas las rutas, recorrido en vivo |

## Evidencia
Índice de `evidencia/`: `H0-linea-base.txt` (typecheck rojo en `dev` antes de tocar nada) · `H0-S1/` (tokens) · `H0-S2/` (operaciones: generador, regeneración, auditoria, simulado, e2e, `capturas/` ronda 1 y `capturas-v3/` ronda final) · `H0-S3/` (DDL) · `H0-S6/` (typecheck, lint, test:front, a11y, ui, e2e completo, regresión final) · `H4/` (value sin hasValue, e2e shell y admisiones) · `doble-revision.md`.
```text
$ yarn workspace @aportaya/backoffice test:front  → Test Files 151 passed · Tests 948 passed
$ yarn workspace @aportaya/backoffice test:a11y   → Test Files 42 passed · Tests 78 passed
$ playwright test (backoffice completo)           → 168 passed (5.9m)
$ gradlew :servicios:auditoria:check test webTest testBarrido --rerun-tasks → BUILD SUCCESSFUL (45 + 74 + 2 pruebas)
$ psql -v ON_ERROR_STOP=1 -f sql/aplicar.sql (×3)  → exit 0 · 0 ERROR · 442 tablas
$ python3 -I scripts/verificar_rutas_frontend.py   → 87 rutas · 79 con contrato · 8 sin contrato
```

## No cubierto
- **Todo contra TEST o la API real** (sesión, segundo factor, persistencia, `UI → request → response → persistencia → recarga → UI`).
- `jacocoTestCoverageVerification` y `integrationTest` (necesitan Docker; ENVIRONMENT).
- Teclado, foco visible, lector de pantalla, zoom 200 %, estados cargando/error de la pestaña Operaciones, paginación más allá de la página 1.
- Las 10 reservas visuales menores R1–R10.
- El e2e completo reescribe 24 PNG de `evidencia/` en la raíz si no se fija `CAPTURAS_DIR` (se restauraron antes de commitear).

## Desvíos del plan
- Se descartó H0.S4.M2 (Backend ya reescribió la tabla virtualizada).
- Se quitó el contador de la pestaña Operaciones y la columna «Acciones» de la tabla de directas por la revisión independiente.
- Se agregó H4 (hallazgos F1–F9) porque la verificación destapó defectos que el plan no preveía.
- La acción de cabecera deja de ser un carril con scroll y pasa a envolver: revierte una decisión previa del carril.

## Riesgos residuales y deuda
- Las pantallas que dependen de las 8 rutas sin contrato siguen sin datos reales: el PR las deja con un error accionable, no resueltas.
- `operaciones.json` empaquetado es relleno: «Operaciones directas» casi siempre sale vacía.
- La contraseña del operador de TEST se pegó en el chat: conviene **rotarla**. No está en ningún archivo ni commit de esta rama.

## Decisiones y ambigüedades
| Decisión / ambigüedad | Supuesto tomado | A quién confirmar |
|---|---|---|
| Choque de diseño con el trabajo de Marcelo (pestañas de sección vs. acordeón) | Gana el acordeón (instrucción explícita de Pablo) | Pablo — ya decidido |
| «Estándar de centro de datos» y «de centro de decisiones» no existen como documento | Son los grupos Datos y Gobierno → Motor de decisiones de Atlas; se abordan en H5 | Pablo |
| `POST /extraccion/*` sin servicio | Mapear a `auditoria` (regla 98.7: un servicio nuevo exige justificación) | Pablo |
| Acciones de cabecera: envolver vs. scroll | Envolver (no ocultar estados) | Pablo / quien diseñó el carril |
| La regla de «una tarjeta con pestañas» es de `mantra-core-health`, no de Pasanaku | No aplica a Pasanaku | — |
