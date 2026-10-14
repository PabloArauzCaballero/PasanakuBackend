# H3.S1.M3 — teclado real del sistema Android

- Resultado: PASS en emulador Android Samsung_A06-L con Gboard del sistema; no se afirma validación en un teléfono físico.
- Pantalla: emulador 540×960 px, densidad 240 dpi (360×640 dp); viewport Flutter mientras se muestra el IME: 360×616 dp.
- Ejecución: `flutter drive --no-pub -d emulator-5554 --use-application-binary=build/app/outputs/flutter-apk/app-debug.apk --driver=test_driver/h3_s1_m3_teclado_test.dart --target=integration_test/h3_s1_m3_teclado_test.dart` — `00:27 +2: All tests passed!` (una prueba de integración).
- El test abre `mostrarHojaDeSeleccion` con 30 opciones, solicita el IME Android y espera a que finalice `AnimatedPadding` antes de evaluar la geometría. `dart analyze --fatal-infos apps/movil/integration_test/h3_s1_m3_teclado_test.dart` — `No issues found!`.
- Métrica impresa por la prueba: `inset=275.333 dp`, borde superior del teclado `340.667 dp`, buscador `Rect(16, 152.9, 344, 207.9)`, cuatro filas construidas; dos filas completas quedan dentro del viewport sobre el teclado. La tercera es recortada por el límite de la lista, sin invadir la superficie del IME.
- Captura completa del sistema (incluye Gboard): [h3-s1-m3-android-keyboard.png](./h3-s1-m3-android-keyboard.png). Inspección visual: buscador despejado, lista desplazable y teclado visible; la hoja se reajusta sin solapamiento.
- El emulador y el servidor ADB se apagaron al terminar; no quedaron procesos de emulator, qemu, adb, Flutter/Dart, Java ni Docker activos.
