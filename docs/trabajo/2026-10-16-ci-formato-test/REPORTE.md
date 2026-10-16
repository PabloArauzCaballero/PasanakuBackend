# Reporte — `test` vuelve a pasar formato, barrido y `dart format`

> **AVANCE: 2 / 6 — 33,3 %.**

- Fecha: 2026-10-16 · Plan: [PLAN.md](./PLAN.md) · Rama: `pablo/fix/ci-formato-test-2026-10-16` (local, sin push)
- Peldaño de evidencia alcanzado: `RUNS` en formato (spotless y dart format con rc 0); barrido sigue en rojo.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M2 | `ExpedienteDeIdentidad.java` formateado | `./gradlew spotlessCheck` | PASS — `evidencia/m2-spotless.txt` (rc=0) |
| H1.S1.M3 | 6 archivos de `diseno_flutter` y 7 de `movil` formateados | `dart format --output=none --set-exit-if-changed lib test` | PASS — `evidencia/m3-dart-format.txt` (rc=0 en ambos) |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S1.M1 | BLOQUEADO | Que pasanaku-28 publique su PR a `dev` (`pablo/fix/arreglos-dev-ci-2026-10-16`) con la división de `RevisionRepositorio`/`UsuariosControllerWebTest`. Reparto acordado por mensaje. Doble declarado en el plan: si no llega, se replica esa división acá. |
| H1.S1.M4 | TODO | Depende de M1 (barrido: `RevisionRepositorio` 317, `UsuariosControllerWebTest` 300). |
| H1.S1.M5 | TODO | Después de M4. |
| H1.S2.M1 | TODO | Después de M5. |

## Evidencia
```text
$ ./gradlew spotlessCheck   (sobre origin/test 9ce01feb, antes)
  src/main/java/bo/aportaya/identidad/dominio/ExpedienteDeIdentidad.java
  Run './gradlew :servicios:identidad:spotlessApply' to fix these violations.   rc=1
$ ./gradlew testBarrido     (antes)
  RevisionRepositorio.java  317 lineas (limite 300)
  UsuariosControllerWebTest.java  300 lineas (limite 300)                        rc=1
$ ./gradlew spotlessCheck   (después)                                             rc=0
$ dart format --output=none --set-exit-if-changed lib test  (después)
  packages/diseno_flutter rc=0 · apps/movil rc=0
```

## No cubierto
- `flutter analyze` y los tests de widget/goldens no se corrieron: el cambio es solo de formato.
- El CI completo no corre en PRs hacia `test` (`ci.yml` solo dispara hacia `main`/`dev`).

## Desvíos del plan
ninguno

## Riesgos residuales
- Mientras M1 no se cierre, `test` sigue en rojo en el barrido.
- `ci.yml` no protege a `test`: una promoción puede volver a meter rojos sin que se vean.

## Decisiones y ambigüedades
- La división de los archivos de 300+ líneas se toma de `dev` (pasanaku-28) para que no haya dos versiones. Confirmado con esa sesión.
- Los `pubspec.lock` que cambió `flutter pub get` se revirtieron: fuera de alcance.
