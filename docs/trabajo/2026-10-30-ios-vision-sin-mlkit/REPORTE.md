# Reporte — iOS sin ML Kit: detección de rostro con Vision de Apple

> **AVANCE: 3 / 5 — 60,0 %.** Build de simulador arm64 en verde y app arrancando en iOS 26; falta el ipa/TestFlight y mergear #372/#373.

- Fecha: 2026-10-31 · Plan: [PLAN.md](./PLAN.md) · Ramas: `claude/ios-vision-sin-mlkit-20261030` (#372 → test), `claude/ios-vision-sin-mlkit-dev-20261030` (#373 → dev)
- Peldaño: **TESTED** (build de simulador, instalación y arranque, tests de la traducción). No `VERIFIED`: la detección real de la cara con Vision no se probó en un iPhone (el simulador no tiene cámara).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | ML Kit fuera de iOS (copias solo Android + overrides) | `grep -c MLKit ios/Podfile.lock` | 0 |
| H1.S1.M2 | Lector Vision en Swift + canal | `xcodebuild build-for-testing -sdk iphonesimulator` | TEST BUILD SUCCEEDED (x86_64 arm64) |
| H1.S1.M3 | Traducción Vision → LecturaDeRostro | `flutter test test/unidad/lector_de_rostro_vision_test.dart` | 3/3 |

## A medias
### H1.S1.M4 — ipa
- Qué anda: build de simulador; instala y arranca en iPhone iOS 26 arm64.
- Qué no anda: —
- Qué falta exactamente: `flutter build ipa` + subida, que se hace con el TestFlight tras mergear.
- Dónde quedó: rama `claude/ios-vision-sin-mlkit-20261030`, compila.
### H1.S1.M5 — Regresión y PRs
- Qué anda: regresión sin fallos nuevos (los 8 de la línea base en Mac son goldens que pasan en Linux).
- Qué no anda: —
- Qué falta exactamente: CI de #372/#373 y merge.
- Dónde quedó: #372, #373.

## Pendiente
ninguna

## Evidencia
Ver [evidencia/verificacion.txt](./evidencia/verificacion.txt).

## No cubierto
- Detección real con la cámara de un iPhone; sentido del `yaw` de Vision con la delantera espejada (supuesto: igual que ML Kit en iOS).
- `patrol build` local no corrió: patrol_cli 3.11.0 (el del CI) no acepta patrol 3.15.0 del proyecto; se reprodujo con `xcodebuild build-for-testing`.

## Desvíos del plan
- Ninguno de fondo. El arreglo de 3 líneas del agente (`EXCLUDED_ARCHS` en Runner) no se usó: sin ML Kit en iOS no hace falta.

## Riesgos residuales
- Si el signo del giro sale al revés, «girá a la izquierda/derecha» no se valida solo; queda la foto manual. Arreglo: invertir `giro` en `lecturaDesdeVision`.
- En iOS no hay «ojos abiertos»; el juez la acepta ausente, así que esa comprobación no corre en iPhone.
- Las copias de `terceros/` hay que actualizarlas a mano (README).

## Decisiones y ambigüedades
- Vision en iOS: decisión de Pablo (2026-10-30).
- Sentido del giro: supuesto, **a confirmar por Pablo en un iPhone**.
