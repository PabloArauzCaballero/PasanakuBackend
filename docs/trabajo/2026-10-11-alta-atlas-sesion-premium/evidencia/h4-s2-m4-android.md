# H4.S2.M4 — distribución Android de campos de nombre

- Resultado: PASS en emulador Android Samsung_A06-L a 360 dp y 412 dp. La captura requerida por H4.S2.M4 se realizó en el emulador permitido por `NOTAS.md`; esto no equivale a validación en un teléfono físico.
- Integración: `flutter drive --no-pub -d emulator-5554 --use-application-binary=build/app/outputs/flutter-apk/app-debug.apk --driver=test_driver/h4_s2_m4_visual_test.dart --target=integration_test/h4_s2_m4_visual_test.dart` — PASS 1/1 a cada ancho (`All tests passed!`).
- 360 dp: Android 540×960 px @240 dpi. Salida de geometría: `width=360.0`, campos en `x=16..344 dp`, cuatro posiciones verticales distintas (56, 163, 270 y 377 dp); disposición en columna confirmada. [Captura inspeccionada](./h4-s2-m4-360-android.png): labels, hints y controles completos, sin recorte ni solapamiento.
- 412 dp: Android 618×1098 px @240 dpi. Salida de geometría: `width=412.0`; campos de cada pareja con mismo `top` (65 dp y 181 dp), en columnas `x=16..200` y `212..396 dp`; filas alineadas pese a que «Segundo nombre (opcional)» ocupa dos líneas. [Captura inspeccionada](./h4-s2-m4-412-android.png): cuatro campos legibles, alineación correcta y sin clipping.
- Regresión widget existente: `flutter test --no-pub --concurrency=1 --reporter expanded test/widget/campos_del_alta_test.dart test/widget/paso_datos_test.dart` — `00:03 +6: All tests passed!` (geometría 360/412, hints, etiquetas de ejemplo y avance del formulario).
- Análisis del test de integración: `flutter analyze --fatal-infos integration_test/h4_s2_m4_visual_test.dart` — `No issues found!`.
- La AVD y ADB se apagaron al terminar; no quedaron procesos emulator/qemu/adb, Dart/Flutter, Java o Docker activos.
