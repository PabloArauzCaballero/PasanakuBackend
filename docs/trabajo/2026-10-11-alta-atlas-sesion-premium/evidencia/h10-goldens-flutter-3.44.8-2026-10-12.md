# H10.S1.M6/M7 - Repetición de goldens en Flutter 3.44.8

Fecha: 2026-10-12. SDK Flutter 3.44.8 / Dart 3.12.2; todas las pruebas en serie. No se usó `--update-goldens`.

App, desde `apps/movil`:

```text
flutter test --concurrency=1 --reporter expanded test/goldens --timeout 60s
00:17 +5 -15: Some tests failed.
EXIT_CODE=1
```

Los 15 casos de `test/goldens` que ya existían fallan en comparación pixelaria. Los 5 casos nuevos de estrés H8 pasan y guardan capturas específicas; se conservaron las baselines previas y los diffs quedaron en `apps/movil/test/goldens/failures/`. Repetida después de corregir la inicialización del ticker de `HundidoAlTocar`; las discrepancias visuales anteriores continúan.

Suite completa del paquete, desde `packages/diseno_flutter`:

```text
flutter test --concurrency=1 --reporter compact --timeout 60s
+99 -15: Some tests failed.
EXIT_CODE=1
```

Goldens del paquete aislados:

```text
flutter test --concurrency=1 --reporter compact test/goldens --timeout 60s
+3 -15: Some tests failed.
EXIT_CODE=1
```

Los fallos de golden son de comparación pixelaria. No satisfacen el DoD verde de M6/M7; falta ejecutar en el rasterizador/plataforma canónicos y revisar visualmente antes de aceptar cualquier baseline.

## Revalidación del paquete de diseño — 2026-10-13

SDK disponible: Flutter 3.47.5 / Dart 3.13.4 en Windows. La suite completa de `packages/diseno_flutter` llegó a 116 PASS / 2 FAIL. Ambas fallas se aislaron en `test/goldens/marca_golden_test.dart`: los PNG `marca_claro`/`marca_oscuro` y los tres estados de apertura conservaban colores maestros anteriores a los roles actuales. La geometría y los hitos temporales permanecían iguales; la diferencia visible coincide con los cambios deliberados en `packages/tokens/tokens.json` (acento claro `o500`→`o700`; marca oscura `g400`→`g300`).

Se actualizaron únicamente esas cinco baselines, tras inspeccionar los diffs actuales y verificar que la forma/posición de marca y barra no cambian. Las cinco imágenes resultantes se abrieron e inspeccionaron individualmente: marca claro/oscuro y apertura temprano/medio/tarde; colores corresponden al tema, trazos completos, sin artefactos ni cambio de composición.

```text
flutter test --no-pub --concurrency=1 --reporter expanded --update-goldens test/goldens/marca_golden_test.dart
00:00 +2: All tests passed!
EXIT_CODE=0

flutter test --no-pub --concurrency=1 --reporter expanded test/goldens/marca_golden_test.dart
00:00 +2: All tests passed!
EXIT_CODE=0

flutter test --no-pub --concurrency=1 --reporter compact --timeout 60s
00:27 +118: All tests passed!
EXIT_CODE=0
```

Resultado local: la suite completa del paquete está verde (118/118) en Windows/Flutter 3.47.5. Sin embargo, H10.S1.M7 sigue `A MEDIAS`: las cinco baselines de marca se regeneraron en Windows para iterar sobre los cambios intencionales de tokens, pero la guía `visual-regression-testing` exige el entorno canónico y declara que las baselines Windows no sirven para CI Linux. La única distro WSL disponible es `docker-desktop`; Docker daemon no responde, así que no hay runner Linux local para aprobarlas. M6 —suite completa de `apps/movil`— también sigue parcial. No cubierto: compatibilidad de estas cinco referencias en Linux/macOS y suite Android/app global.

## Repetición posterior tras el feed de movimientos (12-10-2026)

App, desde `apps/movil`, ya con H8.S1.M3 corregida y goldens de `h8_break_ui_test.dart` revisados:

```text
flutter test --no-pub --concurrency=1 --reporter compact test/goldens --timeout 90s
+10 -14: Some tests failed.
EXIT_CODE=1
```

Los seis casos de `h8_break_ui_test.dart` pasan con y sin `--update-goldens`. Quedan 14 fallos en H10 texto al 200 % (5), candado H1 (6), las dos capturas de saldo que no representan el feed integrado, y la prueba de transición portada→ingreso (diff en hitos asentado/final). La comparación visual confirma que los seis casos de candado y los cinco de texto conservan geometría y contenido, con diferencias de rasterizado/bordes; las imágenes de saldo sí cambiaron estructuralmente por la sección nueva de movimientos. La transición asentada muestra diferencias frente al master anterior del formulario (el teléfono y contraseña están en ambos; la captura actual incorpora además el enlace de recuperación y la nota de custodia). No se reemplazaron esas baselines.

Paquete `packages/diseno_flutter`, suite completa actual:

```text
flutter test --no-pub --concurrency=1 --reporter expanded --timeout 90s
+100 -15: Some tests failed.
EXIT_CODE=1
```

Los fallos son comparaciones golden: 14 casos de `test/goldens/catalogo_golden_test.dart` y el caso «la apertura, cuadro por cuadro» (sus capturas de apertura son tres hitos dentro de un solo test); las 100 pruebas restantes pasan, incluyendo accesibilidad, lógica, widgets y movimiento. Los archivos de catálogo/marca documentan que sus PNG base son de macOS y exigen generarlos/revisarlos en Linux/amd64 para consistencia. En esta máquina `wsl --list --verbose` muestra solamente `docker-desktop` detenido, no hay distro Linux utilizable; no se actualizaron capturas del paquete ni del resto de la app fuera de las seis H8 revisadas.
