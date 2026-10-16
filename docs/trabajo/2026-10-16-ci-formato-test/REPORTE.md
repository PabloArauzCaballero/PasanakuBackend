# Reporte — `test` y `dev` vuelven a pasar formato, barrido y `dart format`

> **AVANCE: 6 / 7 — 85,7 %.**

- Fecha: 2026-10-16 · Plan: [PLAN.md](./PLAN.md)
- Ramas: `pablo/fix/ci-formato-test-2026-10-16` (PR #99 → `test`) y `pablo/merge/ci-test-a-dev-2026-10-16` (PR → `dev`), con la misma cabeza `1fad9ffe` o su descendiente con este reporte.
- Peldaño de evidencia: `TESTED` (checks locales y tests de identidad dirigidos en verde). El CI de GitHub corre después de publicar esto.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Merge de #98 (arreglo de CI de dev) en la rama de test. Conflicto en `RevisionRepositorio` resuelto: el mapeo vive en `FilaDeExpediente` y el campo `correo` (propio de test) pasa ahí | `git diff --check`; `grep '^<<<<<<<'` | PASS — `evidencia/m1-merge.txt` |
| H1.S1.M2 | spotless en `ExpedienteDeIdentidad` | `./gradlew spotlessCheck` | PASS — `evidencia/m2-spotless.txt`, `m2b-spotless-post-merge.txt` |
| H1.S1.M3 | `dart format` en `diseno_flutter` (6) y `movil` (7) | `dart format --output=none --set-exit-if-changed lib test` | PASS — `evidencia/m3-dart-format.txt` |
| H1.S1.M4 | Barrido de 300 líneas | `./gradlew testBarrido` | PASS — `evidencia/m4-barrido.txt` |
| H1.S1.M5 | Regresión de identidad sobre la rama de test | `./gradlew :servicios:identidad:test --rerun :servicios:identidad:webTest --rerun` | PASS — 35/35 y 120/120, `evidencia/m5-identidad.txt` |
| H1.S2.M2 | Rama para dev: #97 (ya en dev) y #98 partieron `UsuariosControllerWebTest` de dos formas. Queda la de #97 (`UsuariosConsultasSensiblesWebTest`); se quitan `UsuariosControllerConsultasWebTest` y `ArnesDeUsuariosController` | spotless, barrido, dart format e identidad en la rama de dev | PASS — `evidencia/dev-*.txt`, 35/35 y 123/123 |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S2.M1 | EN CURSO → merge de los PRs a `test` (#99) y a `dev` | CI de los PRs y `gh pr merge`. Si el modo auto bloquea el merge, lo hace Pablo. |

## Evidencia
```text
# Antes, sobre origin/test 9ce01feb
./gradlew spotlessCheck  → ExpedienteDeIdentidad.java · rc=1
./gradlew testBarrido    → RevisionRepositorio.java 317, UsuariosControllerWebTest.java 300 · rc=1
dart format …            → diseno_flutter rc=1 (6), movil rc=1 (7)

# Después, rama de test (7c5727d4)
spotless rc=0 · barrido rc=0 · diseno dart rc=0 · movil dart rc=0
identidad: test 35/35, webTest 120/120 (BUILD SUCCESSFUL, --rerun)

# Después, rama de dev (1fad9ffe)
spotless rc=0 · barrido rc=0 · diseno dart rc=0 · movil dart rc=0
identidad: test 35/35, webTest 123/123 (BUILD SUCCESSFUL, --rerun)
```

## No cubierto
- `integrationTest` de identidad (necesita base) y `flutter analyze` y los tests de widget y goldens: no se corrieron localmente.
- El barrido de reglas de frontend (`verificar_frontend`) que #98 corrigió no se corrió localmente.
- En los PRs hacia `test` no corre `ci.yml`; el CI completo solo corre en el PR hacia `dev`.

## Desvíos del plan
- Se agregó H1.S2.M2: `dev` avanzó con #97 mientras tanto, que partió los mismos archivos de otra forma que #98.
- El PR a `test` (#99) se actualizó a la misma cabeza que el PR a `dev`, para que las dos ramas queden idénticas.

## Riesgos residuales
- `ci.yml` no protege a `test`: una promoción puede volver a meter rojos sin que se vean. Habría que agregar `test` a `pull_request.branches`. Fuera de alcance; queda anotado.
- #98 (de pasanaku-28) queda incluido en esta rama; si se mergea aparte en `dev`, volvería a duplicar la división.

## Decisiones y ambigüedades
- Entre las dos divisiones de `UsuariosControllerWebTest` se eligió la de #97 porque ya estaba en `dev`. Los 6 tests siguen existiendo, ninguno duplicado.
- Los `pubspec.lock` que cambió `flutter pub get` se revirtieron: fuera de alcance.
