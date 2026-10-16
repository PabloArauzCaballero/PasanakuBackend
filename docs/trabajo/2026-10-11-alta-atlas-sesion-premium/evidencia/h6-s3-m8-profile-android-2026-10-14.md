# H6.S3.M8 — perfil de la cuenta atrás en Android

Fecha: 2026-10-14. Entorno: Windows 11, AVD `Samsung_A06-L` (Android 13/API 33, 4 vCPU virtuales, 2048 MB configurados), Flutter 3.44.8 / Dart 3.12.2. El AVD es una aproximación, no hardware físico de gama media.

## Preparación y comandos

Se concedió `android.permission.CAMERA` al paquete de prueba `bo.aportaya.aportaya_movil`. La integración mantiene activa la cámara frontal nativa y su stream ML Kit. Como el AVD no tiene una persona frente al sensor, un wrapper exclusivo del test descarta los veredictos del detector y devuelve `false` para que la pantalla use su fallback documentado sin detección; así se puede medir la cuenta atrás sin fingir una cara válida. El preview y el procesamiento ML permanecen activos.

```text
dart format apps/movil/integration_test/h6_s3_m8_profile_test.dart apps/movil/test_driver/h6_s3_m8_profile_test.dart
Formatted 2 files (0 changed) in 0.01 seconds.

flutter analyze --no-pub integration_test/h6_s3_m8_profile_test.dart test_driver/h6_s3_m8_profile_test.dart
No issues found! (ran in 2.0s)

flutter build apk --profile --target=integration_test/h6_s3_m8_profile_test.dart --no-pub
Built build\app\outputs\flutter-apk\app-profile.apk (128.9MB)

flutter drive --profile --no-build --use-application-binary=<apps/movil>/build/app/outputs/flutter-apk/app-profile.apk --no-pub -d emulator-5554 --driver=test_driver/h6_s3_m8_profile_test.dart --target=integration_test/h6_s3_m8_profile_test.dart
00:05 +2: All tests passed!
exit_code=0
```

La integración guardó e inspeccionó [`h6-rendimiento.png`](./h6-rendimiento.png), con el overlay de Flutter visible durante el número 3. La instrumentación de `FrameTiming` aisló diez cuadros de ese tramo:

```text
H6_S3_M8_COUNTDOWN_FRAME_TIMINGS count=10 max_ui_ms=3.99 max_raster_ms=31.53 frames_over_16ms=1
H6_S3_M8_SCREENSHOT=60320 bytes
```

## Revalidación con `flutter run --profile` (2026-10-14)

La primera compilación con el `flutter` del PATH (3.47.5) falló porque el hook nativo `objective_c` usó un kernel incompatible con el `.dart_tool` resuelto para Flutter 3.44.8. No se cambiaron dependencias. Fijando la ruta del SDK canónico, la compilación del entrypoint temporal terminó correctamente:

```text
C:\src\flutter-3.44.8\flutter\bin\flutter.bat build apk --profile --target=lib/main_h6_s3_m8_profile.dart --no-pub
Built build\app\outputs\flutter-apk\app-profile.apk (115.9MB)

dart analyze --fatal-infos lib/main_h6_s3_m8_profile.dart
No issues found!

C:\src\flutter-3.44.8\flutter\bin\flutter.bat run --profile --no-build --use-application-binary=<apps/movil>/build/app/outputs/flutter-apk/app-profile.apk --no-pub -d emulator-5554 --target=lib/main_h6_s3_m8_profile.dart
```

El entrypoint temporal limita el caso a una pose y registra cuadros entre 1,5 y 4,6 segundos después de iniciar el stream. Logcat produjo `count=106`, `max_ui_ms=25.68`, `max_raster_ms=101.17`, `frames_over_16ms=17`. Por tanto, esta muestra más amplia tampoco cumple el umbral. La captura [`h6-rendimiento-cuenta-profile-2026-10-14.png`](./h6-rendimiento-cuenta-profile-2026-10-14.png) quedó en el estado final (FOTO 1 DE 1) con `PerformanceOverlay`, no durante la cuenta atrás; se conserva como evidencia del perfil/overlay, no como prueba visual del número de cuenta. El AVD reportó `Skipped 90 frames` y `Skipped 42 frames` durante el arranque; son jank de inicialización y no forman parte de la ventana contabilizada.

## Revalidación de perfil con AVD limitado (2026-10-14)

Se repitió el entrypoint sin cambios de código en `Samsung_A06-L`, Android 13/API 33, AVD iniciado con límite de memoria de 1536 MiB. El stream de cámara real del emulador quedó activo y el test continuó descartando los veredictos ML para medir el fallback. Comando:

```text
C:\src\flutter-3.44.8\flutter\bin\flutter.bat run --profile --no-build --use-application-binary=build\app\outputs\flutter-apk\app-profile.apk --no-pub -d emulator-5554 --target=lib/main_h6_s3_m8_profile.dart
```

Logcat registró:

```text
H6_PROFILE_WINDOW=96411375:99511375
H6_PROFILE_FRAME_TIMINGS count=135 max_ui_ms=19.68 max_raster_ms=34.95 frames_over_16ms=20
```

Esta repetición también supera el umbral (20/135 cuadros, principalmente por raster). El máximo fue menor que en la corrida anterior (34.95 ms frente a 101.17 ms), lo que demuestra variación importante del AVD; no identifica por sí solo una causa de producción. No se editó el painter ni se cambió el diseño. Tras recoger la ventana, se cerró la app y se apagaron AVD y ADB; los puertos 5554 y 5037 quedaron cerrados.

La ventana de medición sí emitió su resumen; esta corrida no se califica como suite PASS: la sesión de `flutter run` quedó en modo interactivo sin stdin utilizable en la herramienta y se detuvo su proceso propio tras registrar la ventana. El criterio de rendimiento continúa incumplido por las métricas, independientemente del estado del runner.

La app usa el preview y el stream real de CameraX/ML Kit; el wrapper de perfil descarta veredictos y fuerza el retorno `false` para ejercitar el fallback sin rostro. El resultado del AVD es diagnóstico, no representa un Android físico ni basta por sí solo para atribuir el pico al código de producción.

## Resultado y límites

La primera integración aisló diez cuadros con UI máxima de 3,99 ms, raster máxima de 31,53 ms y 1/10 sobre 16 ms. La revalidación del entrypoint perfilado amplió la muestra a 106 cuadros: UI máxima 25,68 ms, raster máxima 101,17 ms y 17/106 sobre 16 ms. El criterio de rendimiento **no está satisfecho** y H6.S3.M8 permanece `A MEDIAS`. Los picos grandes de raster/UI en AVD requieren confirmación en Android físico antes de atribuir una causa de producción.

La escena virtual y el fallback determinista no prueban que ML Kit valide una pose real, ni representan con fidelidad el tiempo de raster de un Samsung físico. Falta repetir `flutter run --profile` en un Android de gama media real con una pose delante de la cámara y confirmar que ningún cuadro de la cuenta atrás supere 16 ms. Si el pico se reproduce, localizar y corregir su causa antes de cerrar la tarea; no se ocultó ni se excluyó del resultado.

Skills leídas según `AGENTS.md`: `fixing-motion-performance`, `frontend-performance`, `flutter-development`, `flutter-state-architecture`, `pasanaku-movimiento`, `flutter-testing`, `visual-proof`, `evidence-and-verification`, `root-cause-debugging`, `scope-discipline`, `agent-resource-control` y `windows-dev-environment`. No se modificaron baselines ni código de producción. Tras la corrida se apagaron el AVD, ADB y Gradle; la comprobación final no encontró procesos activos del test ni listeners en 5554/5037.

Nota de cierre observada en Logcat: al cancelar/liberar la cámara aparecen mensajes `BufferQueue has been abandoned` y `CameraError(ERROR_CAMERA_DISABLED)` durante el teardown. La integración termina 0, pero esta corrida no clasifica esos mensajes como comportamiento inocuo en dispositivo físico; revisarlos si se reproducen fuera del cierre del test.
