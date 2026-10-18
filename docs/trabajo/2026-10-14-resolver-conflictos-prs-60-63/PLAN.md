# Plan — Resolver conflictos de los PRs #60 y #63 contra `test`

- Fecha: 2026-10-14 · Repos afectados: PasanakuBackend · Predecesor: ninguno
- Resultado observable: #60 (`dev` → `test`) y #63 pasan a `MERGEABLE`, sin conflictos, y el CI deja de saltear sus jobs.
- Kill-test: `gh pr view 60 --json mergeable` devuelve `CONFLICTING`.

## Alcance
- IN: merge de `origin/test` dentro de `dev` resolviendo los 113 archivos en conflicto; regenerar goldens; actualizar #63 sobre `dev`.
- OUT: refactor, features, tocar el working tree local de `pablo/fix/lint-movil-2026-09-30`.
- Ambigüedades registradas:
  - `paso_celular.dart` borrado en `dev` y modificado en `test`: supuesto, gana el borrado salvo dependencia viva. Confirmar con Pablo.
  - Pacto `nucleo-financiero-entregas.json` add/add: supuesto, se une sin perder contratos de ninguno. Confirmar con Pablo.
  - #63 es rama de Leonardolv21: se actualiza solo después de #60. Confirmar con Leonardolv21.

## H1 — #60 sin conflictos
**CA:** Dado el PR #60, cuando se empuja el merge resuelto a `dev`, entonces `mergeable` ≠ `CONFLICTING`.
**DoD:** `gh pr view 60 --json mergeable` · `git diff --name-only --diff-filter=U` vacío.
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Merge de `origin/test` en worktree sobre `dev`, resolver conflictos | Sin marcadores `<<<<<<<` | `git grep -n '^<<<<<<<'` vacío | TODO |
| H1.S1.M2 | Regenerar goldens | PNG sin conflicto | `flutter test --update-goldens` en `apps/movil` → exit 0 | TODO |
| H1.S1.M3 | Verificar compilación/tests dirigidos | Pasa | comandos del CLAUDE.md del repo | TODO |
| H1.S1.M4 | Push a `dev` | PR 60 mergeable | `gh pr view 60 --json mergeable` | TODO |

## H2 — #63 sin conflictos
**CA:** Dado #63, cuando se actualiza sobre `dev`, entonces deja de estar `CONFLICTING`.
**DoD:** `gh pr view 63 --json mergeable`.
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Merge de `dev` resuelto en la rama de #63 | Sin conflictos | `gh pr view 63 --json mergeable` | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Elegir mal un lado en dinero/despliegue | Alto | Revisar cada add/add a mano, registrar decisión |

## Actualización 2026-10-14 (pedido de Pablo): igualar dev/test, verificar TEST, lanzar APK/IPA/TestFlight
Hallazgo: #60 y #63 quedaron MERGED/MERGEABLE; mientras tanto Leonardolv21 y Justin siguieron mergeando en `test` (#66). H1/H2 se dan por superados por el estado real de las ramas.

## H3 — dev == test y TEST (Contabo) desplegado con ese commit
**CA:** Dado `origin/dev` y `origin/test`, entonces apuntan al mismo SHA y el host de Contabo corre ese SHA con los 15 servicios + 3 fronts sanos.
**DoD:** `git rev-parse origin/dev origin/test` iguales · `ssh … tail autodespliegue.log` muestra `listo en <sha>` sin `FALLA` en la tanda · `docker ps` healthy · curl 200.
**Estado:** EN CURSO
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Fast-forward de dev a test | mismo SHA | `git rev-parse origin/dev origin/test` | HECHO (f783db9b) |
| H3.S1.M2 | Esperar tanda del host para f783db9 y verificar | `listo en f783db9` | log + `docker ps` + curl | EN CURSO |
| H3.S1.M3 | Imágenes de fronts/móvil-web al día respecto del commit | fecha de imagen ≥ cambio de `apps/` | `docker images` | TODO |

## H4 — Pendientes delegados a la Mac (RELEVO-CODEX §6, PROGRESO H10.S1.M6/M7)
**CA:** Dado `origin/test`, cuando se compila en esta Mac (Flutter 3.44.8, Xcode 26.6), entonces existen un APK y un IPA nuevos y el IPA está en TestFlight; los goldens se validan en macOS.
**DoD:** salidas literales en `evidencia/`.
**Estado:** TODO
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H4.S1.M1 | Goldens de `apps/movil` y `packages/diseno_flutter` en macOS/3.44.8, sin `--update-goldens` | todos PASS o fallos clasificados | `flutter test test/goldens` | TODO |
| H4.S1.M2 | Compilar APK contra TEST | APK existe | `flutter build apk` exit 0 | TODO |
| H4.S1.M3 | `scripts/testflight_ios.sh` | `Upload succeeded` | salida del script | TODO |
