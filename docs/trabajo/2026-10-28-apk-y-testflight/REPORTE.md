# Reporte — APK y TestFlight con los cambios nuevos de test

> **AVANCE: 3 / 3 — 100,0 %.** APK 8 publicado y build 202610282041 subido a App Store Connect.

- Fecha: 2026-10-28 · Plan: [PLAN.md](./PLAN.md) · Rama(s): `origin/test` 5dafb0f5 (sin cambios de código)
- Peldaño de evidencia alcanzado: APK `VERIFIED` hasta el release publicado (no se instaló en un teléfono); iOS subida aceptada por App Store Connect (no se abrió en un teléfono).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | APK de prueba 8 (`aportaya-test-8.apk`, 113 MB) de 5dafb0f5, generado por el workflow al mergear #219 en test | `gh release view apk-prueba-8` | PASS · [evidencia/apk.json](./evidencia/apk.json) |
| H2.S1.M2 | Build 202610282041 subido tras aceptar el PLA (API ASC respondió 200) | `xcodebuild -exportArchive … -authenticationKey*` | `EXPORT SUCCEEDED` · [evidencia/subida.txt](./evidencia/subida.txt) |
| H2.S1.M1 | Runner.xcarchive de 5dafb0f5, build 202610282041, en el worktree `../Pasanaku-testflight` | `bash scripts/testflight_ios.sh --solo-compilar` | exit 0 · [evidencia/compilar.txt](./evidencia/compilar.txt) |

## A medias
ninguna

## Pendiente
ninguna (el procesado en TestFlight lo hace Apple en unos minutos)

## Evidencia
Ver [evidencia/subida.txt](./evidencia/subida.txt): `PLA Update available` + `No signing certificate "iOS Distribution" found` → `EXPORT FAILED`.

## No cubierto
- Ningún build se instaló ni se abrió en un dispositivo.
- No se corrieron tests de móvil (el CI del merge #219 es el que corresponde).

## Desvíos del plan
- La subida falló dos veces por `PLA_NOT_ACCEPTED`; el titular aceptó el acuerdo y la tercera pasó.
- La subida no la lanzó Pablo con `!`: la clave ASC sí es legible desde `~/.appstoreconnect/env`. Falló por el PLA, no por la clave.

## Riesgos residuales
- El worktree `../Pasanaku-testflight` queda en disco con el archivo; borrarlo con `git worktree remove ../Pasanaku-testflight` tras la subida.

## Decisiones y ambigüedades
- Aceptar el acuerdo legal de Apple es una decisión del titular de la cuenta; el agente no lo hace.
