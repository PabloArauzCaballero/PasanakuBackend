# Recorridos de la app

## Android: runner real y smoke local

`android/app/src/androidTest/java/bo/aportaya/aportaya_movil/MainActivityTest.java`
expone los tests Dart al runner nativo de Patrol. `build.gradle.kts` fija
`PatrolJUnitRunner` y Android Test Orchestrator para aislar cada caso; no limpia
los datos de la app automáticamente. Antes de ese cableado, `patrol test`
terminaba con exit 0 y **Total: 0**, que no demuestra nada.

Con Flutter 3.44.8, JDK 21, Patrol CLI 3.11.0 y un emulador Android con
espacio suficiente, desde `apps/movil`:

```bash
patrol test --target integration_test/arranque_y_registro_test.dart --device <id> --dart-define=API=http://10.0.2.2:4010/api/v1
patrol test --target integration_test/tour_completo_test.dart --device <id> --dart-define=API=http://10.0.2.2:4010/api/v1
```

En el AVD AtlasDemo se ejecutó **1/1 PASS** en cada comando: el primero abrió
portada → «Saltar» → formulario; el segundo avanzó por las cuatro láminas hasta
el alta. En un intento de ejecutar ambos juntos el AVD se desconectó durante
el último toque; el reporte marcó la ejecución incompleta. No contarlo como
PASS de suite combinada. Un `Total: 0` tampoco es PASS aunque el proceso devuelva
exit 0. Estos dos recorridos no envían un alta ni prueban OTP, backend TEST,
reanudación tras cierre, iOS o dispositivo físico.

Los scripts `test:e2e` y `test:e2e:smoke` pasan por
`scripts/ejecutar_patrol.dart`, que falla si Patrol reporta `Total: 0` o no
imprime resumen. El smoke apunta al primer recorrido; `test:e2e` intenta toda
la carpeta y requiere un entorno TEST completo. Ambos aceptan los argumentos
de Patrol, por ejemplo `--device` y `--dart-define`.

`reanudacion_borrador_test.dart` agrega un recorrido Android del primer paso de
alta: escribe un dato sintético, desmonta el árbol Flutter, vuelve a abrir la
app y comprueba que Keystore devuelve el borrador al formulario. Usa un prefijo
exclusivo `e2e.reanudacion.` y borra solo esa clave al terminar; no lee ni
reemplaza el borrador de una persona. Se ejecutó aislado con **1/1 PASS**:

```bash
dart scripts/ejecutar_patrol.dart --target integration_test/reanudacion_borrador_test.dart --device <id> --dart-define=API=http://10.0.2.2:4010/api/v1
```

Esta prueba verifica almacenamiento nativo y reconstrucción de UI, **no** un
`force-stop` ni relanzamiento del proceso por Android. Ese recorrido y su
equivalente iOS siguen pendientes para H7.S1.M2.

Para el cierre real del proceso en un **emulador de ensayo** hay un gate aparte:

```bash
corepack yarn workspace @aportaya/movil test:e2e:reinicio emulator-5554 --dart-define=API=http://10.0.2.2:4010/api/v1
```

`verificar_reinicio_android.dart` rechaza un dispositivo que no confirme ser
emulador. Ejecuta `reinicio_sembrar_test.dart` con `--no-uninstall`, abre el APK,
comprueba un PID vivo, hace `adb shell am force-stop` solo del paquete de la app,
comprueba que ya no hay PID y ejecuta `reinicio_verificar_test.dart` sin limpiar
los datos. Cada fase debe reportar al menos un caso y salir en verde. La segunda
borra la clave sintética; el borrador normal no se toca. Los dos archivos de
fase están excluidos del `test:e2e` general porque no son tests independientes:
su orden y el cierre nativo son parte del gate especializado. El APK de ensayo
queda instalado en el emulador; no ejecutar este gate en un teléfono personal.

En AtlasDemo, ambas fases pasaron **1/1**, y `adb` confirmó proceso vivo antes y
ausente después de `force-stop`. Esto cubre reanudación tras cierre en Android
emulado, pero no iOS, dispositivo físico, OTP ni backend TEST.

La configuración sigue la [guía de instalación de Patrol](https://patrol.leancode.co/documentation)
y el [aislamiento de Android Test Orchestrator](https://developer.android.com/training/testing/instrumented-tests/androidx-test-libraries/runner#using-android-test-orchestrator).

En el simulador iOS de GitHub CI, `arranque_y_registro_test.dart` comprueba el
arranque sin sesión, el tour y la entrada al formulario de alta.
`deep_link_test.dart` comprueba `aportaya://unirse/<tokenId>.<secreto>` como URI
de inicio y como URL nativa con la app abierta. Patrol monta el árbol Flutter
de prueba antes de abrir enlaces nativos; no prueba un arranque del proceso desde
una app terminada. Las
pruebas widget cubren la ruta sin pestañas y el doble envío de recarga con una
sola petición.

El enlace permite al organizador copiarlo, al destinatario autenticarse con el
teléfono invitado, ver el monto y el reglamento, y aceptarlo una vez. Identidad
valida el secreto, teléfono, estado y nivel KYC; grupos verifica restricciones,
reputación y cupo, y crea participante, cupo y firma en una sola transacción.

Los otros guiones Patrol de F12.1 todavía necesitan un entorno de ensayo con API
real y un Android físico: Scalar no ejecuta los efectos financieros completos ni
provee una cámara real. No se consideran aprobados por las pruebas de iOS.

En esta Mac, la ejecución Android local con `patrol_cli` 3.11.0 se bloqueó por
JDK 26 (`Unsupported class file major version 70`). Para Android se necesita un
JDK 17 o 21 específico de la herramienta, sin cambiar el JDK global.
