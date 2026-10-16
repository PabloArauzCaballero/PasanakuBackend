# H10.S2.M6 — selectores del alta con teclado Android

**VEREDICTO: PASS**  
**Peldaño:** `REGRESSION_VERIFIED` para los dos selectores del criterio M6.  
**Superficie:** Mobile, AVD Android.  
**No cubierto:** recorrido completo del alta, backend/correo, cámara, rostros/liveness, contrato y subida. No son parte del DoD de M6 y no se simularon aquí.

## Alcance

Se probaron los widgets reales `PasoDocumento` y `PasoActividad`, que son los pasos usados por el alta. No se saltó una verificación de rostro ni se llamó a un backend. El test buscó «a» en cada selector con Gboard abierto; verificó tres filas completas sobre el teclado y eligió la primera opción con un toque.

## Entorno y salida

- Windows, Flutter 3.47.5 / Dart 3.13.4; `Samsung_A06-L`, Android 13/API 33, AVD limitado a 768 MiB.
- Se conservó la resolución original del AVD antes de cerrarlo. Durante el test se fijó temporalmente 540×996 px a 240 dpi, que dio `MediaQuery.size = 360×640 dp`; el IME ocupó 275,33 dp.
- APK de integración compilada sin AVD:

```text
C:\src\flutter\bin\flutter.bat build apk --debug --target=integration_test/h10_s2_m6_selectores_ime_test.dart --no-pub
Running Gradle task 'assembleDebug'... 78,8s
√ Built build\app\outputs\flutter-apk\app-debug.apk
```

- Integración ejecutada sobre `emulator-5554`:

```text
C:\src\flutter\bin\flutter.bat drive --no-build --use-application-binary=build\app\outputs\flutter-apk\app-debug.apk --no-pub -d emulator-5554 --driver=test_driver\h10_s2_m6_selectores_ime_test.dart --target=integration_test\h10_s2_m6_selectores_ime_test.dart
H10S2M6_CAPTURE_READY archivo=h10-s2-m6-expedido-en-360x640.png campo=Expedido en pantalla=360.0x640.0 inset=275.3333333333333 buscador=Rect.fromLTRB(16.0, 141.0, 344.0, 196.0) filas=[Rect.fromLTRB(0.0, 200.0, 360.0, 248.0), Rect.fromLTRB(0.0, 248.0, 360.0, 296.0), Rect.fromLTRB(0.0, 296.0, 360.0, 344.0), Rect.fromLTRB(0.0, 344.0, 360.0, 392.0)] visibles=3
H10S2M6_CAPTURE_READY archivo=h10-s2-m6-actividad-economica-360x640.png campo=Actividad económica pantalla=360.0x640.0 inset=275.3333333333333 buscador=Rect.fromLTRB(16.0, 141.0, 344.0, 196.0) filas=[Rect.fromLTRB(0.0, 200.0, 360.0, 248.0), Rect.fromLTRB(0.0, 248.0, 360.0, 296.0), Rect.fromLTRB(0.0, 296.0, 360.0, 344.0), Rect.fromLTRB(0.0, 344.0, 360.0, 392.0)] visibles=3
01:33 +1: (tearDownAll)
01:33 +2: All tests passed!
All tests passed.
```

The assertions also verified that tapping the first filtered result closes the sheet and displays the selected value in each field. The integration runner exited 0. Static checks:

```text
dart format --output=none --set-exit-if-changed integration_test/h10_s2_m6_selectores_ime_test.dart test_driver/h10_s2_m6_selectores_ime_test.dart
Formatted 2 files (0 changed) in 0.01 seconds.

dart analyze integration_test/h10_s2_m6_selectores_ime_test.dart test_driver/h10_s2_m6_selectores_ime_test.dart
No issues found!
```

## Inspección visual

Ambas capturas de 540×996 px se abrieron e inspeccionaron. El buscador y tres filas completas quedan por encima de Gboard; la cuarta fila queda parcialmente oculta por el teclado y no se contó. No hay recorte del buscador ni solapamiento con la lista. El rótulo diagonal `DEBUG` es del APK de integración. Capturas:

- [Expedido en, 360×640 dp](./h10-s2-m6-android-2026-10-15/h10-s2-m6-expedido-en-360x640.png)
- [Actividad económica, 360×640 dp](./h10-s2-m6-android-2026-10-15/h10-s2-m6-actividad-economica-360x640.png)

## Recursos

Pico de RAM muestreado durante la ejecución: **14,20/15,32 GiB**, bajo el límite de 15,1 GiB. Al terminar se restauró la resolución anterior, se cerró el AVD y el daemon de Gradle ya estaba detenido. Estado final: `adb devices` vacío, sin procesos emulator/QEMU/Java ni contenedor iniciado; RAM **9,8/15,32 GiB**.
