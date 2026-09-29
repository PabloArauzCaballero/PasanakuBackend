# Plan — Primera subida de la app móvil a TestFlight

- Fecha: 2026-09-28 · Repos afectados: PasanakuBackend (solo `apps/movil`) · Predecesor: job `release-ios` de `.github/workflows/ci.yml` (firma escrita, nunca ejecutada)
- Resultado observable: Pablo abre TestFlight en su iPhone, instala "Aportaya Movil" y la app habla con el backend TEST de Contabo.
- Kill-test: en App Store Connect → TestFlight no aparece ningún build de `bo.aportaya.aportayaMovil`.

## Alcance
- IN: firma y subida **desde esta Mac** con la cuenta de Apple Developer (paga) iniciada en Xcode; build con `--dart-define=API=https://api.aportaya.161.97.85.216.sslip.io/api/v1`.
- OUT: los secretos de CI y el job `release-ios` (se anotan, no se tocan); `ITSAppUsesNonExemptEncryption` (declaración legal, la responde Pablo en ASC); nombre visible "Aportaya Movil" (se anota); backend.
- Ambigüedades registradas:
  - A1: Xcode tiene registrado `7A42TVN3Y6` como *Personal Team* (`isFreeProvisioningTeam = 1`). Pablo afirma tener membresía paga. Supuesto: la sesión de Xcode está desactualizada; se confirma con H1.S1.M1. Confirmar: Pablo.
  - A2: la URL por defecto del CI (`api.aportaya.bo`) no resuelve en DNS; se usa TEST por decisión de Pablo (2026-09-28).

## H1 — Build en TestFlight
**CA:** Dado el código de esta rama, cuando se sube el IPA firmado, entonces App Store Connect lo lista en TestFlight para `bo.aportaya.aportayaMovil`.
**DoD:** salida de subida `EXPORT SUCCEEDED`/upload OK pegada + captura o confirmación del build en ASC.
**Estado:** TODO

### H1.S1 — Cuenta y registro de la app
**CA:** existe un equipo pago con acceso a la app en App Store Connect.
**DoD:** comandos de M1 y M2.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Equipo pago visible en Xcode | Existe un equipo con `isFreeProvisioningTeam = 0` | `defaults read com.apple.dt.Xcode IDEProvisioningTeamByIdentifier` → un equipo no gratuito | HECHO — la caché de preferencias de Xcode siguió diciendo `Personal Team`, pero Apple aceptó la firma de distribución del equipo 7A42TVN3Y6 (`evidencia/h1s2m2-upload.txt`, `evidencia/h1s1m1-identidades.txt`). Desvío: se verificó por el resultado de la firma, no por `defaults read`. |
| H1.S1.M2 | `DEVELOPMENT_TEAM` del Runner = equipo pago | El proyecto apunta al Team ID pago | `grep DEVELOPMENT_TEAM ios/Runner.xcodeproj/project.pbxproj` → el ID pago en las 3 configuraciones | HECHO — Team pago confirmado `7A42TVN3Y6` (Individual, vía login de EAS en sesión GymSheet 2026-09-28) = el del proyecto; `evidencia/h1s1m2-team.txt` → 3. Sin cambios de código. |
| H1.S1.M3 | Registro de la app en App Store Connect (lo crea Pablo en la web: Apps → + → bundle `bo.aportaya.aportayaMovil`) | La app existe en ASC | La subida de H1.S2.M2 no falla con "No suitable application records" | HECHO — la subida no dio ese error (`evidencia/h1s2m2-upload.txt`) |

### H1.S2 — Archivo, firma y subida
**CA:** el IPA firmado con distribución llega a ASC.
**DoD:** salidas de M1 y M2.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Archivo release contra TEST | `Runner.xcarchive` generado | `flutter build ipa --release --no-codesign --build-number=<n> --dart-define=API=…` → exit 0 | HECHO — `evidencia/h1s2m1-build.txt` (exit=0, build 2026092808) |
| H1.S2.M2 | Exportar con destino `upload` y firma automática | Subida aceptada por ASC | `xcodebuild -exportArchive … -allowProvisioningUpdates` con `destination=upload` → `EXPORT SUCCEEDED` | HECHO — intento 2 `Upload succeeded` · `EXPORT SUCCEEDED` · exit=0 (`evidencia/h1s2m2-upload.txt`) |

### H1.S3 — Prueba en iPhone real
| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S3.M1 | Instalar desde TestFlight y abrir la pantalla inicial contra TEST | La app abre y la primera llamada al backend no da error de red | Observación de Pablo en el iPhone 12 (captura) | A MEDIAS — Pablo instaló, pero la app da error: builds 2026092808 y 202609281231 sin `HOSTS_PERMITIDOS` (bloqueo `host-ajeno`) + backend TEST sin servicios (solo gateway; 500 en todas las rutas). |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Xcode sin sesión de la cuenta paga | No se puede firmar | Pablo inicia sesión en Xcode → Settings → Accounts |
| Bundle ID tomado por otro equipo | No se registra la app | Decidir bundle nuevo con Pablo (no se inventa) |
| Build number repetido | ASC rechaza la subida | Usar número de build único |
| Procesamiento de Apple (10–30 min) y cumplimiento de exportación | El build no aparece de inmediato | Pablo responde la pregunta de cifrado en ASC |

### H1.S4 — Segunda subida (pedido de Pablo 2026-09-28)
| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S4.M1 | Nuevo build a TestFlight con build number único | ASC acepta el paquete | `flutter build ipa --no-codesign` + `xcodebuild -exportArchive … destination=upload` → `Upload succeeded` | HECHO — build 202609281231, `Upload succeeded` · `EXPORT SUCCEEDED` · exit=0 (`evidencia/h1s4m1-subida.txt`) |
| H1.S4.M2 | Build con `HOSTS_PERMITIDOS` (sin él, release bloquea con `host-ajeno`: `lib/dominio/configuracion.dart:85`) | ASC acepta el paquete | build + upload → `Upload succeeded` | HECHO — build 202609281353, `Upload succeeded` · exit=0; validador con estos valores → `valida` (sin lista → `host-ajeno`) (`evidencia/h1s4m2-subida-hosts.txt`) |
| H1.S5.M1 | Reconstruir backend TEST (esquema + 15 servicios, de a uno) y desplegar (autorizado por Pablo 2026-09-28) | Toda ruta de la API deja de dar 500 | `curl` a una ruta de identidad → 4xx de contrato, no 500 | HECHO — 15 healthy en `d603ffe`; API responde 401/200 de contrato, no 500 (`evidencia/h1s5-api-viva.txt`) |
| H1.S5.M2 | Generador DDL: evolución idempotente del outbox (ADD COLUMN IF NOT EXISTS ×4 + CHECK con TOMADO) para bases creadas antes de #20 | Una base TEST vieja queda sin deriva tras aplicar el esquema | clon del esquema de TEST + imagen nueva → exit 0 y `comm` fresco/clon = 0 diferencias | HECHO en rama `pablo/fix/esquema-outbox-evolucion-2026-09-28`, PR #29 contra `test` (fusionado por Pablo: `1d97c92`; en TEST el contenedor esquema terminó `Exited (0)`). Clon de TEST: exit 0, 2ª corrida exit 0, deriva 0. |
| H1.S5.M3 | Imagen: nativos de JNA/argon2 extraídos en la imagen (no en /tmp noexec) — `identidad` en bucle `UnsatisfiedLinkError … failed to map segment` desde H2.S4 (f1e5dee) | `identidad` arranca con `read_only` + tmpfs `/tmp` noexec | `docker run --read-only --tmpfs /tmp` de la imagen + hash Argon2 real → OK; contenedor healthy en TEST | HECHO — PR #30 fusionado (`d603ffe`); identidad healthy en el compose real (`evidencia/h1s5-api-viva.txt`) |

### H1.S6 — Protección de pantalla en iOS (decisión de Pablo 2026-09-28: "implementar la protección real")
Causa: la app muestra «AportaYa no puede abrir en este dispositivo» en TODO iPhone release, porque `proteccionPantalla` de iOS es `noSoportado` (`lib/infraestructura/capacidades.dart`) y `seguridadCriticaSoportada` bloquea el arranque. No depende del modelo de iPhone.
Técnica: el canal `bo.aportaya/proteccion_pantalla` (el mismo de Android) atendido en `AppDelegate.swift`; la capa de la ventana se cuelga del lienzo seguro de un `UITextField` y `isSecureTextEntry` prende/apaga el tapado en capturas y grabaciones. Grado `degradado`, no `soportado`: depende de un comportamiento de UIKit que Apple no documenta como API de privacidad.

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S6.M1 | Adaptador Dart iOS por canal + grado `degradado` + tests actualizados al nuevo requisito | iOS invoca `activar`/`desactivar` por el canal y `seguridadCriticaSoportada` es true | `flutter test test/unidad/capacidades_test.dart test/widget/bloqueo_plataforma_test.dart` → verde | HECHO — 18/18 (`evidencia/h1s6m1-tests.txt`) |
| H1.S6.M2 | Receptor nativo en `AppDelegate.swift` | El build de iOS compila | `flutter build ios --release --no-codesign` → exit 0 | HECHO — exit 0 (`evidencia/h1s6m2-build.txt`) |
| H1.S6.M3 | En el iPhone 12 la app abre sin cartel de bloqueo | Pantalla inicial visible | lanzar por `devicectl` + captura/confirmación de Pablo | A MEDIAS — instalada en el iPhone 12 (exit 0); no se pudo lanzar (teléfono bloqueado); Pablo reporta "el error persiste" sin precisar el teléfono; el iPhone se desconectó. |
| H1.S6.M4 | Build nuevo a TestFlight | ASC acepta | `Upload succeeded` | HECHO — build 202609282146, canal en el binario, `Upload succeeded` (`evidencia/h1s6m4-subida.txt`) |
