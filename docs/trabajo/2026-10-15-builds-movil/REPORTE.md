# Reporte — APK y TestFlight desde `test` (con el visual bancario)

> **AVANCE: 4 / 4 — 100,0 %.** APK listo y build 202610151748 subido a TestFlight (`Upload succeeded`). Ninguno de los dos se abrió todavía en un teléfono.

- Fecha: 2026-10-15 · Plan: [PLAN.md](./PLAN.md) · Rama: `pablo/docs/builds-movil-2026-10-15` (= `origin/test` dbfbf35b + estos docs), worktree `.claude/worktrees/visual-bancario`
- Peldaño de evidencia: `RUNS` (los builds compilan). No se instaló nada en un teléfono.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Clientes Dart (Java 21), tokens, diseño y movil construidos | `gradlew generateOpenApiClients` + `yarn workspace … build` | exit=0 · `evidencia/h1s1m1-preparacion.txt` |
| H1.S1.M2 | APK release contra TEST, 110,6 MB, versionCode 26101517 | `flutter build apk --release --build-number=26101517 --dart-define=API=… --dart-define=HOSTS_PERMITIDOS=…` | exit=0 · `~/Downloads/APK-Pasanaku/aportaya-test-7-visual-bancario-TEST.apk`, sha256 a4669a2b…0286a30 · `evidencia/h1s1m2-apk.txt` |
| H1.S1.M4 | Build 202610151748 firmado y subido a App Store Connect | `xcodebuild -exportArchive … -authenticationKeyPath/ID/IssuerID` (lo corrió Pablo con `!`; variables desde `~/.appstoreconnect/env`) | `Upload succeeded` · `EXPORT SUCCEEDED` · exit 0 · `evidencia/h1s1m4-subida.txt` |
| H1.S1.M3 | `Runner.xcarchive` release, build 202610151748, contra TEST | `flutter build ipa --release --no-codesign --build-number=202610151748 …` | exit=0 · `evidencia/h1s1m3-ipa.txt` |

## A medias
### H1 — hito
- Qué anda: APK y TestFlight generados y subidos (tabla de arriba).
- Qué no anda: nada observado en rojo.
- Qué falta exactamente: instalar los dos builds en un teléfono y recorrer el alta con la cámara real.
- Dónde quedó: worktree `.claude/worktrees/visual-bancario`, rama `pablo/docs/builds-movil-2026-10-15`, sin cambios de código.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Prueba en dispositivo (APK y TestFlight) | TODO | Pablo instala y abre el alta |

## Evidencia
```text
$ flutter build apk --release --build-number=26101517 …
✓ Built build/app/outputs/flutter-apk/app-release.apk (110.6MB)
exit=0
$ flutter build ipa --release --no-codesign --build-number=202610151748 …
Codesigning disabled with --no-codesign, skipping IPA.
exit=0 build=202610151748
$ xcodebuild -exportArchive … -allowProvisioningUpdates
error: exportArchive Failed to Use Accounts
** EXPORT FAILED **
exit=70
# 2.º intento, primero falló por variables vacías (`!` no carga el perfil): el script ahora hace `source ~/.appstoreconnect/env`
Progress 100%: Upload succeeded.
** EXPORT SUCCEEDED **
```

## No cubierto
- No se abrió ninguno de los dos builds en un teléfono, así que tampoco se vieron la cámara real ni ML Kit.

## Desvíos del plan
- La subida no pudo hacerla el agente: Xcode sin sesión y la clave de ASC fuera de su entorno. La corrió Pablo con el script `subir-testflight.sh` del scratchpad.

## Riesgos residuales
- Los dos builds apuntan a TEST (`api.aportaya.161.97.85.216.sslip.io`). Si TEST no desplegó todavía dbfbf35b, la app habla con un backend más viejo.
- Xcode sigue sin la sesión de la cuenta: cada subida depende de la clave de API.

## Decisiones y ambigüedades
- API = TEST, la misma decisión que el 2026-09-28 (A2).
