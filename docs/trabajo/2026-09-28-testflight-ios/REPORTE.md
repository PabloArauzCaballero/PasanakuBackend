# Reporte — Primera subida de la app móvil a TestFlight

> **AVANCE: 13 / 15 — 86,7 %. Todo build de iOS mostraba «AportaYa no puede abrir en este dispositivo» (protección de pantalla sin implementar en iOS). Arreglo subido en 202609282146; SIN confirmar todavía en un iPhone.**

- Fecha: 2026-09-28 · Plan: [PLAN.md](./PLAN.md) · Rama: `pablo/fix/docker-test-coolify-2026-09-25`
- Peldaño de evidencia alcanzado: `RUNS` en la cadena de distribución (App Store Connect aceptó el paquete). La app no se abrió todavía en un iPhone.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S6.M1 | iOS: protección de pantalla por canal, grado `degradado`; el arranque release con capacidades reales de iOS ya no bloquea | `flutter test test/unidad/capacidades_test.dart test/widget/bloqueo_plataforma_test.dart` | PASS 18/18 |
| H1.S6.M2 | Receptor nativo en `AppDelegate.swift` | `flutter build ios --release --no-codesign` | PASS exit 0 |
| H1.S6.M4 | Build 202609282146 a TestFlight | build + export upload | PASS `Upload succeeded` |
| H1.S5.M1–M3 | TEST: esquema evolucionado (PR #29), nativos JNA en imagen (PR #30), 15 servicios healthy | `curl` a la API de TEST | PASS — 401/200, no 500 (`evidencia/h1s5-api-viva.txt`) |
| H1.S2.M1 | `Runner.xcarchive` release, bundle `bo.aportaya.aportayaMovil`, versión 0.1.0, build 2026092808, API = TEST Contabo | `flutter build ipa --release --no-codesign --build-number=2026092808 --dart-define=API=https://api.aportaya.161.97.85.216.sslip.io/api/v1` | PASS — `evidencia/h1s2m1-build.txt` (exit=0) |
| H1.S1.M2 | Team ID pago = `7A42TVN3Y6`, el mismo que declara el proyecto; no hubo que cambiar código | `grep -c "DEVELOPMENT_TEAM = 7A42TVN3Y6;" …project.pbxproj` | PASS — 3 (`evidencia/h1s1m2-team.txt`) |
| H1.S1.M1 | Apple aceptó la firma de distribución del equipo pago | `xcodebuild -exportArchive … -allowProvisioningUpdates` | PASS — `evidencia/h1s2m2-upload.txt` |
| H1.S1.M3 | La app existe en App Store Connect: la subida no falló por falta de registro | ídem | PASS — ídem |
| H1.S2.M2 | IPA firmado y subido a App Store Connect | `xcodebuild -exportArchive -archivePath apps/movil/build/ios/archive/Runner.xcarchive -exportOptionsPlist <method=app-store-connect, destination=upload, signingStyle=automatic, teamID=7A42TVN3Y6> -exportPath build/ios/ipa-upload -allowProvisioningUpdates` | PASS — `Upload succeeded` · `** EXPORT SUCCEEDED **` · exit=0 |
| H1.S4.M2 | Build 202609281353 con `HOSTS_PERMITIDOS` | build + upload + test temporal del validador | PASS — `Upload succeeded`; validador `valida` |
| H1.S4.M1 | Segunda subida, build 202609281231 | `flutter build ipa --no-codesign` + `xcodebuild -exportArchive … destination=upload` | PASS — `Upload succeeded` · `EXPORT SUCCEEDED` · exit=0 (`evidencia/h1s4m1-subida.txt`) |

## A medias
### H1.S3.M1 — App en el iPhone contra TEST
- Qué anda: backend TEST con 15 servicios healthy (`d603ffe`), API responde 401/200 de contrato; el build 202609281353 lleva `HOSTS_PERMITIDOS` y pasa el validador.
- Qué no anda: sin observar todavía en el dispositivo con este build.
- Qué falta exactamente: Pablo instala 202609281353 desde TestFlight y confirma que la pantalla inicial carga sin error.
- Dónde quedó: build 202609281353 en App Store Connect.

## Pendiente
ninguna

## Evidencia
```text
# intento 1 (evidencia/h1s2m2-upload.txt)
error: exportArchive No Team Found in Archive
** EXPORT FAILED **   exit=70
# causa: el archivo se generó con --no-codesign y no lleva Team; se agregó teamID a las opciones de exportación.

# intento 2
Progress 100%: Uploaded package is processing.
Progress 100%: Upload succeeded.
warning: exportArchive App Store Connect Warning. Deployment target too low. Your app targets iOS 13.0. Starting in April 2027, iOS apps must target 15.0 or later …
Uploaded Runner
** EXPORT SUCCEEDED **
exit=0

$ security find-identity -v -p codesigning   (evidencia/h1s1m1-identidades.txt, correo enmascarado)
  1) … "Apple Development: <correo> (K7H7WVCXWP)"
# la firma de distribución la hizo Xcode con un certificado administrado en la nube (-allowProvisioningUpdates): no quedó un certificado local.
$ curl https://api.aportaya.bo/api/v1/salud        → curl exit 6 (no resuelve DNS)
$ curl https://app.aportaya.161.97.85.216.sslip.io/ → 200, TLS válido
```

## No cubierto
- El uso en el iPhone solo está respaldado por la confirmación verbal de Pablo, sin captura.
- No se ejercitó ningún flujo de la app contra TEST desde iOS.

## Desvíos del plan
- H1.S1.M1 se verificó por el resultado real de la firma, no por `defaults read com.apple.dt.Xcode`: esa caché siguió diciendo `Personal Team` aunque Apple aceptó la firma de distribución del equipo.
- El archivo de opciones de exportación de la subida vive en el scratchpad de la sesión, no en el repo. `ios/ExportOptions.plist` (CI) no se tocó.

## Riesgos residuales
- Quedaron sin limpiar (la limpieza fue bloqueada por el clasificador de permisos): el contenedor `deriva-tmp` (postgres:16 descartable, sin datos reales, solo el esquema) y la imagen `aportaya/esquema:verif` en el VPS; `/tmp/esq-verif`, `/tmp/clon.sql` y `/tmp/*.txt` en el VPS; y los worktrees locales `../Pasanaku-fixesq` y `../Pasanaku-fixjna`. La imagen `aportaya/identidad:verif` y `/tmp/Dockerfile.verif`, `/tmp/verif-identidad-build.log` en el VPS.
- Efectos del `flutter build` local, sin commitear y sin revertir: `apps/movil/pubspec.lock` bajó versiones (Flutter local más viejo) y `project.pbxproj` recibió cambios de CocoaPods. Los IPA subidos se compilaron con ese lock.
- `movil-web` en TEST no construye desde 2026-09-25: `flutter pub get --enforce-lockfile` falla con exit 65 (lock desalineado con pubspec). Anotado, fuera de alcance.
- El deployment target es iOS 13.0. Desde abril de 2027, Apple exige 15.0 o más para aceptar subidas (aviso de ASC).
- En CI, `release-ios` usa por defecto `api.aportaya.bo`, que no resuelve. Tampoco hay secretos de iOS en GitHub. Anotado, fuera de alcance.
- Faltan `ITSAppUsesNonExemptEncryption` en `Info.plist`, una pantalla de lanzamiento propia (hoy es el placeholder de Flutter) y el nombre visible correcto (hoy "Aportaya Movil").
- Cada subida nueva necesita otro build number; se usó `AAAAMMDDHH`.

## Decisiones y ambigüedades
- A1 (resuelta 2026-09-28): la cuenta es paga, Individual, equipo 7A42TVN3Y6 (confirmado por el login de EAS en la sesión de GymSheet y por la firma aceptada).
- A2 (decidido por Pablo, 2026-09-28): el build de TestFlight apunta a TEST en Contabo.
