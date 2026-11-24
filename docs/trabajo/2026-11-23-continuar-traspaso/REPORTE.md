# Reporte — Continuar traspaso Pasanaku: PRs a dev, correcciones y rediseños, contrato y promoción a TEST

> **AVANCE: 31 / 37 — 83,8 %.** (`plan_status.py`). Sin PRs abiertas en PasanakuBackend ni PasanakuFrontend. Falta 1 TODO (salud de TEST, la ejecuta Pablo) y 5 BLOQUEADOS por decisiones o insumos de Pablo.

- Fecha: 2026-11-23 / 24 · Plan: [PLAN.md](./PLAN.md)
- Integrado en `dev` y promovido a `test` (#511, `d75ac069`): #503, #504, #505, #506, #507, #508, #510.
- Peldaño de evidencia: backend y backoffice `TESTED` en la CI de cada PR; app `TESTED` (957 pruebas locales + CI); pantallas rediseñadas con prueba visual en 3 anchos × 2 temas revisada por la sesión principal. TEST desplegado sin health check propio (pendiente de Pablo): el trabajo no pasa de `TESTED` hasta ese `estado`.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Backoffice compila con tipos estrictos (TS4111, plantilla) | CI #503 job 138136110143 | PASS — 31234da, 28cb588b |
| H1.S1.M2 | #503 en verde | check-runs de bcfc6274 | 12 success → merge d62225d9 |
| H1.S1.M3 | Cuatro ojos en CU-132 | `CU132RegistroDeParametros.java:168`, `TarifasController.java:245-265`, `CU132Test.mismaPersona` | control presente |
| H1.S2.M1–M2 | #504: causa (fixture sin titular ni firma del reglamento) y arreglo | check-runs de 29dc2b22 | 12 success → merge 98b03005 |
| H1.S3.M1 | #505 CU-44 analista/revisor | check-runs de ed9bb51 | 12 success → merge 64da8b97 |
| H1.S4.M1 | #503–#505 en `dev` | `git log origin/dev` | d62225d9, 98b03005, 64da8b97 |
| H2.S1.M1 | Diff `test..dev` revisado; M08/M6 ausentes | `git merge-base --is-ancestor` | M08 731459c5 «no», M6 fcbc557b «no» |
| H2.S1.M2 | Promoción `dev → test` | PR #511 | 28 success / 7 skipped / 1 failure preexistente → merge d75ac069 |
| H3.S1.M1–M10 | Correcciones de la app (enlaces, refresco único, FLAG_SECURE, guardia de identidad, vigilante, cierre de sesión, idioma, código muerto, CU-21) | `flutter test` 7 carpetas | 957 en verde |
| H3.S2.M1 | FLUJO-QR-001: «Simular pago» solo con `--dart-define`; guardia backend en producción | CI de #506 (2915db44) | 17 success / 2 skipped → merge e6751478 |
| H3.S2.M2 | SEG-005: secreto de invitación en el fragmento | tests + nginx real | 25/25; log `GET /g` |
| H3.S2.M3 | `cobrarAporte` deprecated (+ `@Deprecated` en el override) | validate + CI | sin problemas |
| H4.S1.M1 | Rediseño de la recarga QR (app) | analyze / format / 44 dirigidas + `test:front` 957 | verde; capturas revisadas |
| H4.S2.M1 | Rediseño del caso de cumplimiento | E2E caso 18/18 + typecheck | verde; capturas revisadas |
| H4.S2.M2 | Rediseño de parámetros financieros | E2E parámetros 10/10 | verde; capturas revisadas |
| H4.S3.M1 | Rediseños integrados | #507 → cc8ad163, #508 → 842f8e26 | CI verde |
| H5.S1.M1 | CU-132 publica `propuestoPor` / `aprobadoPor` | CI de #510 (`servicios:tarifas:test`) | BUILD SUCCESSFUL |
| H5.S1.M2 | CU-44 `GET /cumplimiento/casos/{casoId}` con alerta de origen y titular enmascarado | CI de #510 (`servicios:cumplimiento:test`, verificar_criterios) | BUILD SUCCESSFUL |
| H5.S2.M1 | Backoffice muestra esos datos | E2E caso 19/19, parámetros 12/12 | verde |
| H5.S3.M1 | #510 integrada | merge | cb651d9f |
| H6.S1.M1 | Cero PRs abiertas | `gh api repos/.../pulls?state=open` | 0 y 0 |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H2.S1.M3 | TODO | Pablo ejecuta `estado` en su host (SSH a TEST bloqueado desde este entorno) |
| H3.S1.M11 CONFIG-001 | BLOQUEADO | Clave de release de Android que solo Pablo puede crear y custodiar; verificar publica un prerelease |
| H3.S3.M1 RESPALDO-003 | BLOQUEADO — DECISION_REQUIRED | Pablo aprueba el texto del tour sobre el fondo de garantía |
| H3.S3.M2 APROB-001 | BLOQUEADO — DECISION_REQUIRED | Pablo decide si se retiran AP-CU20-02 / AP-CU69-10 |
| H3.S3.M3 grupos/detalle | BLOQUEADO — DECISION_REQUIRED | Decisión de negocio (PENDIENTES.md) |
| H3.S4.M1 dispositivo real | BLOQUEADO | Push FCM, rostros reales, goldens macOS: dispositivo y cuentas fuera de este entorno |
| M08 / M6 (checkpoints del traspaso) | fuera de este trabajo | Incompletos; no se integraron |

## Evidencia
```text
#503 bcfc6274: 12 success                                → merge d62225d9
#504 29dc2b22: 12 success, 6 skipped                     → merge 98b03005
#505 ed9bb51:  12 success, 6 skipped                     → merge 64da8b97
#506 2915db44: skipped=2 success=17                      → merge e6751478
#507 5630e11c: 0 fallos                                  → merge cc8ad163
#508 11ee8082: skipped=8 success=9                       → merge 842f8e26
#510 e6d1ab11: skipped=6 success=12 (cumplimiento:test, tarifas:test — BUILD SUCCESSFUL) → merge cb651d9f
#511 (dev→test): failure=1 skipped=7 success=28 → merge d75ac069
     Release iOS: deep_link_test 01 passed; deep_link_test 02 failed — mismo fallo que en dev antes (jobs 137891048575, 137889264412)
Fallos de entorno relanzados: 429 de Maven Central en «Contratos» (#506) y «Dependencias, secretos e imagen» (#511; gitleaks: no leaks found)
Fallos reales corregidos: -Werror por deprecación (2915db44, reproducido con javac: sin marca exit=1, con marca exit=0); criterio de CU-44 ausente en la bóveda (e6d1ab11, verificar_criterios: «Sin divergencias»)
movil local: flutter test 7 carpetas → +957 All tests passed · dart analyze → No issues found
Capturas: evidencia/movil-{antes,rediseno}/, evidencia/backoffice-{antes,rediseno}/, evidencia/contrato/
```

## No cubierto
- Gradle local: Maven Central responde 429 a esta máquina; todo lo Java se verificó solo en CI.
- Salud de TEST tras la promoción: no ejecutada (la hace Pablo).
- `deep_link_test 02` en iOS sigue en rojo (preexistente; requiere macOS para diagnosticar).
- Ficha del caso LFT y propuso/aprobó: E2E contra el simulado; no se ejercitaron contra un backend real desplegado.
- Lector de pantalla real y movimiento reducido en dispositivo: no probados (axe y semántica en código).
- `AndroidManifest.xml` (`path="/g"`) no probado en teléfono.

## Desvíos del plan
- #504 no era el lock de JaCoCo: era la fixture (titular y firma del reglamento).
- #503 «spotless» era un 429; detrás había TS4111, plantilla estricta y matriz de roles.
- Se agregaron H3 (app), H4 (rediseños) y H5 (contrato) a pedido de Pablo del 2026-11-24.
- CU-44 no tenía ruta para la ficha del caso: se agregó `obtenerCasoLft` (aditiva, mismo permiso `CUMPLIMIENTO_CASOS`).
- Las capturas versionadas del caso (`evidencia/visual-caso-*-inicial.png`) mostraban el Tablero; el E2E ahora espera el `h1` del caso.

## Riesgos residuales
- `RecargasProveedorSimulado` se activa por omisión (`matchIfMissing=true`); la guardia de #506 lo impide en producción con modo prueba, pero conviene revisar la configuración de producción.
- Dependabot seguirá abriendo PRs contra `main` hasta que la configuración de `dev` llegue a la rama por defecto.
- SEG-005: versiones viejas de la app abren la web en vez de la app hasta actualizarse.
- Presupuesto inicial del bundle del backoffice: 503,30 kB (> 500 kB, aviso de build).

## Decisiones y ambigüedades
- PRs a `main` #468, #495, #496, #497 y dependabot #509: cerradas sin mergear (decisión de Pablo). PasanakuFrontend #53, #56, #57, #59 (dependabot, CI en rojo): cerradas.
- COMERCIAL conserva `TARIFARIO_ADMINISTRAR` (confirmado por Pablo).
- Confirmar con Pablo: operador en parámetros mostrado como UUID (ningún contrato publica nombres; habría que consultar identidad).
- Confirmar con Pablo: titular del caso como `****` + 4 hex del seudónimo.
- Confirmar con Pablo: alerta de origen = la más antigua enlazada al caso, más el conteo.
- Confirmar con Pablo: en la recarga el número grande dice «Monto a acreditar» (la orden no publica un importe a pagar distinto).
- `aportaya://aporte` → `/pasanaku/mi-estado` (el recordatorio todavía no emite `obligacionId`).
- M08 TTL (24 meses estricto vs ~25): DECISION_REQUIRED, sin tocar.
