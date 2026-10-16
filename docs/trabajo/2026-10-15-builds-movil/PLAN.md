# Plan — APK y TestFlight desde `test` (con el visual bancario)

- Fecha: 2026-10-15 · Repos afectados: Pasanaku (sin cambios de código) · Predecesor: `2026-10-15-visual-bancario`, receta de `2026-10-14-resolver-conflictos-prs-60-63` (H4) y `scripts/testflight_ios.sh`
- Commit compilado: `origin/test` dbfbf35b (incluye PR #71 visual bancario y PR #72 alta con correo repetido)
- Resultado observable: Pablo tiene un APK nuevo en `~/Downloads/APK-Pasanaku/` y un build nuevo en TestFlight, ambos contra TEST (Contabo).
- Kill-test: `aapt`/`apkanalyzer` del APK no muestra versionCode nuevo, o `xcodebuild -exportArchive` no imprime `Upload succeeded`.

## Alcance
- IN: generar clientes Dart (Java 21), build de tokens/diseño/movil, `flutter build apk`, `flutter build ipa --no-codesign`, export + upload.
- OUT: cambios de código; instalar en un dispositivo (lo hace Pablo).
- Ambigüedades: ninguna nueva. API = TEST (decisión A2 del 2026-09-28).

## H1 — Builds distribuibles
**CA:** Dado `origin/test` dbfbf35b, cuando se compila, entonces existe un APK release y un build de TestFlight subido, ambos apuntando a TEST.
**DoD:** salidas de los comandos en `evidencia/`.
**Estado:** A MEDIAS — builds hechos y subidos; falta abrirlos en un teléfono

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Clientes Dart + tokens + diseño + movil build | build_runner exit 0 | `gradlew generateOpenApiClients` + `yarn workspace … build` exit 0 | HECHO |
| H1.S1.M2 | APK release contra TEST | APK copiado a Downloads | `flutter build apk --release …` exit 0 + sha256 | HECHO |
| H1.S1.M3 | Archivo iOS sin firmar | `Runner.xcarchive` existe | `flutter build ipa --no-codesign …` exit 0 | HECHO |
| H1.S1.M4 | Firmar y subir a TestFlight | `Upload succeeded` | `xcodebuild -exportArchive … -allowProvisioningUpdates` exit 0 | HECHO (2.º intento, con clave de ASC) |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Xcode sin sesión (`Failed to Use Accounts`, ya pasó el 14-oct) | No sube | Usar la clave de API de ASC si está permitido; si no, Pablo corre el export con `!` |
