# Reporte — iOS sin ML Kit: detección de rostro con Vision de Apple

> **AVANCE: 5 / 5 — 100,0 %.** #372/#373 mergeados; «navegación en simulador iOS» en verde en el CI por primera vez desde 2026-10-04; TestFlight 202610310124 con Vision.

- Fecha: 2026-10-31 · Plan: [PLAN.md](./PLAN.md) · Ramas: `claude/ios-vision-sin-mlkit-20261030` (#372 → test), `claude/ios-vision-sin-mlkit-dev-20261030` (#373 → dev)
- Peldaño: **TESTED** (build de simulador, instalación y arranque, tests de la traducción). No `VERIFIED`: la detección real de la cara con Vision no se probó en un iPhone (el simulador no tiene cámara).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | ML Kit fuera de iOS (copias solo Android + overrides) | `grep -c MLKit ios/Podfile.lock` | 0 |
| H1.S1.M2 | Lector Vision en Swift + canal | `xcodebuild build-for-testing -sdk iphonesimulator` | TEST BUILD SUCCEEDED (x86_64 arm64) |
| H1.S1.M4 | ipa con Vision subido a TestFlight | `xcodebuild -exportArchive` | `Upload succeeded`, build 202610310124 |
| H1.S1.M5 | CI: navegación en simulador iOS en verde (#372 y #373); PRs mergeados | `gh pr checks 372/373` | pass · MERGED |
| H1.S1.M3 | Traducción Vision → LecturaDeRostro | `flutter test test/unidad/lector_de_rostro_vision_test.dart` | 3/3 |

## A medias
ninguna

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
- **`deep_link_test` 02 intermitente en el CI** (enlace con la app abierta): falló en #372 (13 s, no apareció «Invitación al grupo») y pasó en #373 con el mismo árbol (6 s). En local, iOS 26.5, pasan los dos (`patrol test`, 2/2). Clasificación: intermitente de entorno (simulador iOS 26.2 del runner), **no** bug de producto. No se agregaron reintentos ni se subió el timeout. Si vuelve a fallar, hace falta el trace del xcresult del CI para ver si `openUrl` muestra el diálogo «¿Abrir en AportaYa?».
- El E2E `arranque_y_registro_test` estaba desactualizado desde 2026-10-28 (esperaba «Saltar» y «Nombres»); se corrigió al flujo actual.
- Si el signo del giro sale al revés, «girá a la izquierda/derecha» no se valida solo; queda la foto manual. Arreglo: invertir `giro` en `lecturaDesdeVision`.
- En iOS no hay «ojos abiertos»; el juez la acepta ausente, así que esa comprobación no corre en iPhone.
- Las copias de `terceros/` hay que actualizarlas a mano (README).

## Decisiones y ambigüedades
- Vision en iOS: decisión de Pablo (2026-10-30).
- Sentido del giro: supuesto, **a confirmar por Pablo en un iPhone**.
