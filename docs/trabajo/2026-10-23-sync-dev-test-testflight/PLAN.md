# Plan — Unificar dev y test y relanzar TestFlight

- Fecha: 2026-10-23 · Repos afectados: PasanakuBackend (solo ramas, sin código) · Predecesor: `2026-10-21-menu-plegable-y-guardia-sistemas`
- Resultado observable: `dev` y `test` con el mismo contenido y un build nuevo de `origin/test` en TestFlight.
- Kill-test: `git diff origin/dev origin/test` no vacío, o `xcodebuild -exportArchive` sin `Upload succeeded`.

## Alcance
- IN: PRs de sincronización dev↔test; build iOS desde `origin/test` en worktree aparte; subida.
- OUT: cualquier cambio de código; tu árbol de trabajo local.
- Ambigüedades: ninguna.

## H1 — dev y test unificados
**CA:** Dado dev y test, cuando se comparan, entonces no hay diferencia de archivos.
**DoD:** `git diff --stat origin/dev origin/test` vacío.
**Estado:** HECHO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | PR test→dev (#185) | mergeado | `gh pr merge 185` | HECHO |
| H1.S1.M2 | PR dev→test (#186) | mergeado | `gh pr merge 186` | HECHO |

## H2 — Build en TestFlight
**CA:** Dado `origin/test` b4ba8263, cuando se compila y sube, entonces el build 202610231045 queda en App Store Connect.
**DoD:** `Upload succeeded` en la salida de xcodebuild.
**Estado:** HECHO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Compilar Runner.xcarchive | existe el archivo | `scripts/testflight_ios.sh` pasos 1-2 exit 0 | HECHO |
| H2.S1.M2 | Firmar y subir con clave ASC | subida aceptada | `Upload succeeded` | HECHO |
