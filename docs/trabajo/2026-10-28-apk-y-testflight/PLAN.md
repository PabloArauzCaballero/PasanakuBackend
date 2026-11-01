# Plan — APK y TestFlight con los cambios nuevos de test

- Fecha: 2026-10-28 · Repos afectados: ninguno (solo builds) · Predecesor: `2026-10-23-sync-dev-test-testflight`
- Resultado observable: APK descargable y build nuevo en TestFlight, ambos de `origin/test` 5dafb0f5.
- Kill-test: no existe release del APK para 5dafb0f5, o `xcodebuild -exportArchive` sin `Upload succeeded`.

## Alcance
- IN: verificar el APK que generó el workflow; build iOS desde `origin/test` en worktree aparte; subida.
- OUT: cambios de código; el árbol de trabajo local (tiene cambios sin commitear del backoffice).
- Ambigüedades: ninguna. `git diff origin/dev origin/test` vacío: test ya tiene todo dev.

## H1 — APK de 5dafb0f5
**CA:** Dado el merge #219 en test, cuando corre `apk-de-prueba.yml`, entonces hay un prerelease con el APK.
**DoD:** `gh run view 40790291254` success con headSha 5dafb0f5 + `gh release view apk-prueba-8`.
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Confirmar run y release del APK | release con .apk | `gh release view apk-prueba-8` | HECHO |

## H2 — Build en TestFlight
**CA:** Dado `origin/test` 5dafb0f5, cuando se compila y sube, entonces el build queda en App Store Connect.
**DoD:** `Upload succeeded` en la salida de xcodebuild.
**Estado:** HECHO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Compilar Runner.xcarchive en worktree | existe el archivo | `scripts/testflight_ios.sh --solo-compilar` exit 0 | HECHO |
| H2.S1.M2 | Firmar y subir con clave ASC | subida aceptada | `Upload succeeded` | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Clave ASC fuera del entorno del agente | no se puede subir | Pablo lanza la subida con `!` (como el 2026-10-23) |
