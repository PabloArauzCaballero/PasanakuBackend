# Reporte — Resolver conflictos de la promoción dev → test

> **AVANCE: 5 / 6 — 83,3 %.**

- Fecha: 2026-10-16 · Plan: [PLAN.md](./PLAN.md) · Rama: `pablo/merge/dev-a-test-2026-10-16` (base `origin/dev` + merge de `origin/test`)
- Peldaño de evidencia alcanzado: `TESTED` (tests locales dirigidos en verde; la CI de la rama todavía no corrió)

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `PROGRESO.md`: se tomó la versión de `dev`, que contiene todas las viñetas de `test` y más | `grep -c '^<<<<<<<' PROGRESO.md` | 0 |
| H1.S1.M2 | Backoffice cumplimiento: versión de `test` (agrega el correo, #87; búsqueda vía `coincideConBusqueda`) | `yarn typecheck` · `yarn test:front` | exit 0 · 73 archivos, 482 tests PASS |
| H1.S1.M3 | Identidad: versión de `test` en los 4 archivos con conflicto, más `ExpedienteDeIdentidad.java` y `SeguridadWebTest.java`, que git fusionó solo pero con constructor y campo `cerrarSesion` duplicados | `./gradlew :servicios:identidad:test --rerun-tasks` | BUILD SUCCESSFUL · 35 tests, 0 fallos |
| H1.S1.M4 | Goldens de capturas: versión de `test` (con la de `dev`, 3 fallan por diferencias de 39–97 %) | `flutter test test/widget/paso_capturas_test.dart` | `+4: All tests passed!` |
| H1.S1.M6 | `VerificacionesControllerWebTest` (roto ya en `test`) pasa `null` como teléfono al constructor de 12 campos | `./gradlew :servicios:identidad:test` | incluido en los 35 PASS |

## A medias
### H1.S1.M5 — Commit, push y PR hacia test
- Qué anda: merge resuelto, sin marcadores de conflicto, tests locales en verde.
- Qué no anda: no se verificó todavía la CI de GitHub sobre la rama.
- Qué falta exactamente: que la CI del PR nuevo corra y `gh pr view --json mergeable` dé MERGEABLE.
- Dónde quedó: rama `pablo/merge/dev-a-test-2026-10-16`, compila.

## Pendiente
ninguna

## Evidencia
```text
$ flutter test test/widget/paso_capturas_test.dart   (goldens de dev)
Golden "goldens/capturas_peek_360.png": Pixel test failed, 39.69%, 103025px diff detected.
Golden "goldens/capturas_marco_claro_360.png": Pixel test failed, 97.46%, 280683px diff detected.
Golden "goldens/capturas_marco_oscuro_360.png": Pixel test failed, 90.42%, 260407px diff detected.
$ flutter test test/widget/paso_capturas_test.dart   (goldens de test)
00:01 +4: All tests passed!
$ ./gradlew :servicios:identidad:test --rerun-tasks
tests=35 failures=0 errors=0 skipped=0
$ yarn --cwd apps/backoffice test:front
Test Files  73 passed (73)
     Tests  482 passed (482)
```

## No cubierto
- Tests de integración de identidad (`integrationTest`, necesitan base) y del resto de los servicios: no se corrieron.
- Goldens de `apps/movil/test/goldens` y de `diseno_flutter` (job de la CI): no se corrieron.
- E2E y verificación visual: no se hicieron.

## Desvíos del plan
- Se agregó H1.S1.M6: `test` ya traía un test de identidad que no compila.
- Dos archivos sin conflicto marcado (`ExpedienteDeIdentidad.java`, `SeguridadWebTest.java`) quedaron rotos por la fusión automática y se resolvieron tomando la versión de `test`.

## Riesgos residuales
- La CI de `test` ya estaba en rojo antes de este trabajo (run 37695173633: contratos, bóveda/esquema, dependencias, formato/compilación). Es posible que algunos de esos fallos sigan en el PR.

## Decisiones y ambigüedades
- Goldens: se tomó `test` porque son las que pasan con el código fusionado. A confirmar con Pablo.
