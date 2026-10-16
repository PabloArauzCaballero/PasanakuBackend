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

La integración inicial aisló diez cuadros con `FrameTiming`. El PNG de esa corrida no se conserva como archivo separado: el mismo [`h6-rendimiento.png`](./h6-rendimiento.png) fue sobrescrito por la corrida del 2026-10-15 descrita abajo; la captura actualmente conservada muestra el overlay durante la cuenta `2`, no la toma original:

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

## Revalidación adicional con AVD (2026-10-15)

Se inició `Samsung_A06-L` con 1536 MiB para respetar el límite de RAM de la estación. El APK de perfil existente se instaló sin recompilar con Flutter 3.44.8. La primera pantalla quedó en el diálogo Android de permiso de cámara; tras conceder `android.permission.CAMERA` al paquete `bo.aportaya.aportaya_movil`, el flujo llegó a `MainActivity`, activó CameraX/ML Kit y produjo una nueva ventana medida. Uso de RAM del sistema observado durante la ejecución: máximo 13,51/15,32 GB; el AVD se apagó después de obtener la muestra y el uso bajó a 9,67 GB.

Comando ejecutado desde `apps/movil`:

```text
C:\src\flutter-3.44.8\flutter\bin\flutter.bat run --profile --no-build --use-application-binary=build\app\outputs\flutter-apk\app-profile.apk --no-pub -d emulator-5554 --target=lib/main_h6_s3_m8_profile.dart
```

Logcat:

```text
H6_PROFILE_WINDOW=112503518:115603518
H6_PROFILE_FRAME_TIMINGS count=40 max_ui_ms=67.76 max_raster_ms=148.52 frames_over_16ms=11
```

La muestra vuelve a fallar el criterio de 16 ms. También aparecen `Skipped 145 frames`/`Skipped 66 frames` al inicio y telemetría del compositor del AVD con intervalos largos; la primera muestra de la ventana tiene un pico de 67,76/148,52 ms y el resto oscila. No hay trazas que atribuyan esos picos a `CuentaAtrasGrande` o al painter de silueta, por lo que no se hizo un cambio de código especulativo. El runner `flutter run` no tenía stdin disponible; la app se cerró con `am force-stop` y el AVD con `emu kill`. Verificación posterior: sin procesos emulator/qemu y ADB no muestra dispositivos.

**Conclusión:** H6.S3.M8 sigue `A MEDIAS`. Esta repetición confirma que el perfil virtual rebasa 16 ms, pero no permite distinguir el coste de composición/cámara de la variabilidad de virtualización. Para certificar el requisito de gama media hace falta una corrida de perfil en un dispositivo Android de referencia; no se afirmará que el requisito está satisfecho con esta evidencia del emulador.

### Intento de integración adicional (2026-10-15, no válido como PASS)

Se intentó `flutter drive --profile --no-build` usando el APK que ya había generado `lib/main_h6_s3_m8_profile.dart`. Aunque el APK ejecutó la pantalla y volvió a emitir métricas (`count=69`, máximo UI 39,94 ms, máximo raster 81,20 ms, 26/69 sobre 16 ms), ese binario no se había compilado con `integration_test/h6_s3_m8_profile_test.dart`; el driver quedó esperando el canal de integración y no generó un resultado `All tests passed` ni una captura nueva. El proceso exacto del runner se detuvo y el AVD se apagó. Este intento se conserva como diagnóstico del error de procedimiento; no se cuenta como prueba automatizada aprobada ni como nueva evidencia visual.

### Corrida de integración con el target correcto (2026-10-15)

La inconsistencia de SDK se corrigió limpiando solo artefactos ignorados de `apps/movil` (`flutter clean`) y regenerándolos bajo Flutter 3.47.5. Se ejecutó `flutter pub get --enforce-lockfile --offline`; los SHA-256 de `apps/movil/pubspec.lock` y `packages/diseno_flutter/pubspec.lock` permanecieron sin cambios. Build:

```text
C:\src\flutter\bin\flutter.bat build apk --profile --target=integration_test/h6_s3_m8_profile_test.dart --no-pub
√ Built build\app\outputs\flutter-apk\app-profile.apk (115.0MB)
```

Prueba en `Samsung_A06-L` (Android 13/API 33, AVD de 1536 MiB):

```text
C:\src\flutter\bin\flutter.bat drive --profile --no-build --use-application-binary=build\app\outputs\flutter-apk\app-profile.apk --no-pub -d emulator-5554 --driver=test_driver\h6_s3_m8_profile_test.dart --target=integration_test\h6_s3_m8_profile_test.dart
H6_S3_M8_COUNTDOWN_FRAME_TIMINGS count=1 max_ui_ms=4.38 max_raster_ms=63.94 frames_over_16ms=1
H6_S3_M8_SCREENSHOT=58298 bytes
exit_code=0
```

El test de integración terminó con código 0 y escribió [h6-rendimiento.png](./h6-rendimiento.png); se inspeccionó la captura y muestra el overlay durante la cuenta `2`. Sin embargo, la instrumentación del test solo recogió **un** cuadro: `runAsync(1.5s)` no bombea frames y después se llama a un único `pump()`. Por eso este PASS valida la ejecución/captura, pero su muestra no valida el umbral de rendimiento. La medición continua autoritativa sigue siendo la de `flutter run --profile` de esta evidencia (40 cuadros, 11 sobre 16 ms en la revalidación 2026-10-15); H6.S3.M8 continúa `A MEDIAS`.

### Ensayo diagnóstico adicional (2026-10-15, inválido como medición)

Se añadió al entrypoint temporal `main_h6_s3_m8_profile.dart` el `--dart-define=H6_PROFILE_OMITIR_STREAM_ML=true` para una futura comparación con el preview y resolución de cámara constantes, alternando solamente el stream de ML Kit. El valor predeterminado mantiene el stream real; no se cambió el flujo de producción. `dart analyze --fatal-infos lib/main_h6_s3_m8_profile.dart` quedó limpio y se compiló el APK de perfil con Flutter 3.47.5.

Se lanzó `Samsung_A06-L` con límite de 1536 MiB. Con el stream predeterminado activo, CameraX inició la cámara frontal a 1920×1080 y se observaron llamadas del detector. Logcat mostró `Skipped 108 frames` y después `Skipped 56 frames`; por ejemplo, `EGL_emulation app_time_stats avg=24.67ms max=247.86ms count=33` y `avg=80.95ms max=249.25ms count=13`. Es telemetría parcial del compositor, no `FrameTiming`. Antes de obtener una ventana `H6_PROFILE_FRAME_TIMINGS`, Flutter perdió conexión con el AVD. El último muestreo de memoria durante la corrida marcó 14,24 GB usados de 15,32 GB; al terminar el AVD ya no estaba activo y el sistema marcó 12,25 GB usados/3,07 GB libres. No hay evidencia de que el pico rebase el umbral acordado, pero por prudencia no se repitió en esta sesión.

Con el AVD apagado se compiló también la variante diagnóstica sin stream:

```text
C:\src\flutter\bin\flutter.bat build apk --profile --target=lib/main_h6_s3_m8_profile.dart --dart-define=H6_PROFILE_OMITIR_STREAM_ML=true --no-pub
√ Built build\app\outputs\flutter-apk\app-profile.apk (115.0MB)
EXIT_CODE=0
```

Esto verifica que la segunda configuración se construye, no que su rendimiento sea distinto: no se instaló ni ejecutó en Android.

**Resultado:** corrida no válida para el umbral de 16 ms ni para atribuir el jank al stream ML o a la cámara virtual. El A/B no se completó y no se modificó resolución ni detector: `veryHigh` es necesario porque la foto de 720p (preset `high`) quedaba por debajo del mínimo de 1400 px descrito en `camara_en_vivo_plugin.dart`; la API local de `camera 0.12.1` además advierte que el streaming a `high` puede provocar caídas de cuadros en dispositivos bajos y recomienda `low`, que no es un cambio aceptable sin volver a medir la calidad de captura. El siguiente ensayo requiere un perfil de memoria que el AVD sostenga y completar ambas variantes; aun así no sustituirá el dispositivo Android de gama media exigido por el plan. Docker quedó apagado.

### Reintento adicional en AVD (2026-10-15, no produjo medición)

Para corregir la muestra de un solo cuadro se probó temporalmente un loop de pumps en la
prueba de integración. Se recompiló con Flutter 3.47.5:

```text
flutter build apk --profile --target=integration_test/h6_s3_m8_profile_test.dart --no-pub
Built build\app\outputs\flutter-apk\app-profile.apk (128.1MB)
EXIT_CODE=0
```

En `Samsung_A06-L` (1536 MiB) `flutter drive --profile --no-build` devolvió `EXIT_CODE=1`:
la prueba agotó su espera sin mostrar la cuenta atrás (`cuentaVisible` fue `false`), por lo
que no entró en la ventana de captura de `FrameTiming` ni reemplazó el PNG anterior. El
AVD arrancó la cámara pero reportó `Skipped 129 frames` y `Skipped 77 frames`; no se atribuye
el fallo a un widget ni se interpreta como resultado de rendimiento. Se revirtió el loop
temporal no validado y no se cambió producto. Uso máximo observado: 13.91/15.32 GiB; AVD
apagado, daemon Gradle detenido, RAM final 10.77/15.32 GiB. La medición válida más reciente
sigue siendo la ventana de `flutter run --profile` (40 cuadros, 11 >16 ms); para la cuenta
atrás sostenida sigue faltando pose/entrada de cámara confiable y el dispositivo de
referencia requerido.
