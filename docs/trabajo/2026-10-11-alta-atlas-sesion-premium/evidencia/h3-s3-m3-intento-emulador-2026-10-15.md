# H3.S3.M3 — integración y evidencia Android (2026-10-15)

## Resultado

La integración Android terminó **PASS (1/1)** en `Samsung_A06-L` con el APK debug precompilado. Se abrieron tres selectores reales del alta con Gboard: departamento, actividad económica y origen de fondos. Los tres PNG se conservaron e inspeccionaron. El test midió `viewInsets.bottom=308 dp`, pantalla de `412×708 dp` y respectivamente 4, 2 y 2 filas completas por encima del teclado.

**H3.S3.M3 sigue BLOQUEADO únicamente por alcance:** el DoD pide que el tercer selector sea uno de soporte; el inventario y la búsqueda en `apps/movil/lib` solo encuentran los tres del alta. Origen de fondos es evidencia adicional de un selector real, no se declara como selector de soporte. No se añadió un selector ficticio.

## Corrida final y archivos de evidencia

- Se regeneró el APK sin emulador activo: `C:\src\flutter\bin\flutter.bat build apk --debug --target=integration_test/h3_s3_m3_selectores_reales_test.dart --no-pub` → `√ Built build\app\outputs\flutter-apk\app-debug.apk`.
- El test se ejecutó sin Gradle/compilación concurrente con `flutter drive --no-build --use-application-binary=build\app\outputs\flutter-apk\app-debug.apk --no-pub -d emulator-5554 --driver=test_driver\h3_s3_m3_selectores_reales_test.dart --target=integration_test\h3_s3_m3_selectores_reales_test.dart` → `All tests passed` (1/1).
- Registro del test: departamento `visibles=4`, actividad económica `visibles=2`, origen de fondos `visibles=2`; en los tres casos `inset=308.0` y el buscador quedó sobre el límite superior del IME.
- Capturas finales de Android de 618×1098 px, con Gboard visible, inspeccionadas: [departamento](./h3-s3-m3-android-2026-10-15/departamento.png), [actividad económica](./h3-s3-m3-android-2026-10-15/actividad-economica.png) y [origen de fondos](./h3-s3-m3-android-2026-10-15/origen-de-fondos.png). Se conserva además una captura auxiliar del primer recorrido ([PNG](./h3-s3-m3-android-2026-10-15/origen-de-fondos-antes-arreglo.png)); en esa captura el teclado ya estaba cerrado, por lo que no se cuenta como evidencia visual del IME ni del fallo. La falla previa queda probada por la salida del test (`inset=308`, `visibles=1`, expectativa mínima 2).
- Se añadió una prueba de widget para listas cortas. Junto a la suite existente de la hoja: **8/8 PASS**.
- Memoria: el APK se compiló sin AVD; durante el recorrido el pico medido fue 13,29/15,32 GiB usados (2,03 GiB libres), bajo el límite de 15,1 GiB. Tras `adb emu kill`, `adb devices` quedó vacío; 9,75 GiB usados/5,57 GiB libres y sin procesos Java/QEMU.

## Fallos descubiertos y corregidos

1. El primer build de la sesión no compiló porque el `.g.dart` ignorado del cliente Dart de identidad estaba desfasado y omitía el campo obligatorio `telefono`. Se regeneró solo ese artefacto con `build_runner`; `dart analyze lib/src/model/expediente_en_revision.dart` quedó limpio. La especificación OpenAPI ya declaraba `telefono`; no se alteró el contrato.
2. El primer recorrido de la integración instalada falló antes de abrir el IME porque `find.bySemanticsLabel` encontraba varios nodos y `ensureVisible` esperaba uno. El test ahora localiza `CampoDeSeleccion` por su etiqueta y toca su `InkWell` único.
3. Ese recorrido llegó luego al selector de origen y probó una falla real: la hoja corta con teclado abierto se encogía y dejaba solo una fila. `hoja_de_seleccion.dart` ahora ocupa el espacio disponible mientras el IME está visible; una prueba nueva de lista corta y el recorrido Android confirman dos o más filas.

## Historial de intentos Android anteriores

- AVD disponible: `Samsung_A06-L`; arrancó como `emulator-5554` y `sys.boot_completed` devolvió `1`.
- Al iniciar el AVD: 12,48 GiB usados / 2,84 GiB libres.
- Antes de iniciar la compilación: 13,01 GiB usados / 2,31 GiB libres.
- Durante `flutter test integration_test/h3_s3_m3_selectores_reales_test.dart -d emulator-5554`, todavía en `Running Gradle task 'assembleDebug'...`: 14,61 GiB usados / 0,72 GiB libres. Se envió Ctrl+C y el comando terminó con código 1, antes del arranque de la app.
- Se apagó el emulador con `adb -s emulator-5554 emu kill`; después: 10,38 GiB usados / 4,94 GiB libres y `adb devices` sin dispositivos.
- Segundo intento: AVD `Samsung_A06-L` con `-memory 768`, Flutter test con `GRADLE_OPTS='-Dorg.gradle.jvmargs=-Xmx1G -XX:MaxMetaspaceSize=768m -XX:ReservedCodeCacheSize=256m'`; seguía en `assembleDebug` al llegar a 14,90 GiB usados / 0,42 GiB libres. Se canceló inmediatamente y se apagó el AVD. Después: 12,35 GiB usados / 2,97 GiB libres; `adb devices` sin dispositivos.
- Tercer intento, tras compilar e instalar el APK debug de la prueba de acceso: AVD `Samsung_A06-L` con `-memory 512` y el mismo override temporal de heap; `flutter test --no-pub integration_test/h3_s3_m3_selectores_reales_test.dart -d emulator-5554` permanecía en `assembleDebug` y alcanzó 14,99 GiB usados / 0,33 GiB libres. Se envió Ctrl+C y se apagó inmediatamente el AVD. Después: 11,13 GiB usados / 4,19 GiB libres; `adb devices` sin dispositivos.
- El análisis estático del test aislado sí se ejecutó: `dart analyze integration_test/h3_s3_m3_selectores_reales_test.dart` → `No issues found!`.
- La lectura del catálogo confirma tres selects reales del alta: departamento (9 opciones), actividad económica (18 más «Otra») y origen de fondos (7). No se encontró selector de soporte/banco en `apps/movil/lib`. La integración prepara los tres del alta, pero origen de fondos no sustituye el control de soporte especificado por el plan.
- Después de ampliar el test a origen de fondos: `dart format --output=none --set-exit-if-changed integration_test/h3_s3_m3_selectores_reales_test.dart` → 0 cambios; `dart analyze integration_test/h3_s3_m3_selectores_reales_test.dart` → `No issues found!`. Este análisis no reemplaza la integración Android.
- Cuarto intento (2026-10-15): el sistema estaba en 11,39 GiB usados / 3,93 GiB libres; `Samsung_A06-L` con `-memory 512` terminó de arrancar en `emulator-5554` y el test corrió serialmente con `flutter test --no-pub --concurrency=1 integration_test/h3_s3_m3_selectores_reales_test.dart -d emulator-5554` y `GRADLE_OPTS='-Dorg.gradle.jvmargs=-Xmx1G -XX:MaxMetaspaceSize=768m -XX:ReservedCodeCacheSize=256m'`. Gradle llegó a `Running Gradle task 'assembleDebug'...`; RAM medida 13,84 GiB y luego 14,32 GiB; al alcanzar 14,97 GiB usados / 0,35 GiB libres se envió Ctrl+C antes de que iniciara la app. Se apagó el AVD con `adb -s emulator-5554 emu kill`; después RAM 10,71 GiB usados / 4,61 GiB libres, `adb devices -l` sin dispositivos y sin procesos Java/QEMU activos. `flutter test` avisó que `--concurrency` se ignora para integración; no hubo suites ejecutadas ni capturas. Skills leídas para este intento: `.agents/skills/flutter-testing/SKILL.md`, `.agents/skills/mobile-native/SKILL.md` y `.agents/skills/visual-proof/SKILL.md`.
- Quinto intento (2026-10-15): tras aclaración del usuario, el techo operativo era 15,1 GiB (no 14,7). Inicio con RAM en 11,27 GiB; AVD 512 MiB arrancado y test serial en `assembleDebug` con heap Gradle de 1 GiB. Mediciones sucesivas: 13,87; 14,22; 14,20; 14,49; 14,75; 14,89; 15,02; la siguiente lectura fue 15,21 GiB usados, **0,11 GiB sobre el máximo indicado**. Se envió Ctrl+C y `adb -s emulator-5554 emu kill`; no inició la app ni produjo salida de tests/capturas. Tras cerrar: 10,30 GiB usados, `adb devices -l` vacío y ninguna JVM, Dart, Flutter o QEMU activa. Se registra como exceso de RAM, no como PASS ni como prueba ejecutada.

## Qué falta para satisfacer el DoD literal

1. Identificar e integrar un selector real de soporte dentro del alcance aprobado del producto, o cambiar explícitamente el criterio del plan mediante una decisión autorizada. No sustituirlo por origen de fondos.

El recorrido ya no está bloqueado por RAM ni por ejecución Android. No quedó proceso de emulador en ejecución. Sí se corrigió la presentación del componente compartido de selección al abrir el teclado.
