# H10.S4.M1 — captura filtrada de logs Android (14 de octubre de 2026)

## Alcance

Se ejecutó en un Samsung_A06-L virtual, con el CU-04 real de la app y un adaptador HTTP falso. El adaptador devolvió una respuesta sintética sin `tokenRefresco`, de modo que el único logger de producción se recorrió. No hubo petición a ningún backend. La prueba no recorrió las pantallas del alta ni manejó carnet o archivos de foto; por eso complementa, pero no reemplaza, la verificación integral que pide la microtarea.

Los valores de teléfono, contraseña y token de acceso fueron sintéticos y únicos para la ejecución. No se guardan en este informe ni se muestran en la salida.

## Evidencia

Búsqueda de llamadas de logging en Dart de producción:

```text
rg -n -i -g '*.dart' 'developer\.log|print\s*\(|debugPrint\s*\(|logger\.|log\.(info|debug|warning|severe|shout|fine|config)' apps/movil/lib packages/diseno_flutter/lib
apps/movil/lib\pantallas\identidad\dominio\cu04_autenticar.dart:83:        debugPrint(
```

El mensaje es constante y no interpola campos, respuestas ni valores de sesión.

Verificaciones en `apps/movil` (Windows, Flutter 3.47.5):

```text
dart format integration_test/h10_s4_m1_logs_test.dart test_driver/h10_s4_m1_logs_test.dart lib/pantallas/identidad/dominio/cu04_autenticar.dart
Formatted 3 files (0 changed) in 0.01 seconds.

flutter analyze --no-pub --fatal-infos integration_test/h10_s4_m1_logs_test.dart test_driver/h10_s4_m1_logs_test.dart lib/pantallas/identidad/dominio/cu04_autenticar.dart
No issues found! (ran in 3.3s)

flutter test --no-pub --concurrency=1 --reporter expanded test/identidad/autenticar_test.dart
00:00 +6: All tests passed!

flutter drive --no-pub -d emulator-5554 --driver=test_driver/h10_s4_m1_logs_test.dart --target=integration_test/h10_s4_m1_logs_test.dart
All tests passed.
EXIT_CODE=0
```

Durante esa integración, `flutter logs --clear --device-id emulator-5554` mostró únicamente esta línea de producción pertinente:

```text
I/flutter (4588): El servidor no entregó tokenRefresco para la sesión móvil.
```

Después se contó, sin imprimir líneas crudas, el contenido de `adb logcat -d`:

```text
safe_logger_lines=1 injected_secret_matches=0 total_log_lines=1100
```

El patrón de búsqueda cubrió los marcadores sintéticos usados para contraseña, token de acceso y teléfono. No se probó un token de refresco real porque la respuesta simulada lo omite; el inventario estático confirma que ninguna llamada de logging restante accede a ese campo. CI y ruta de foto no se inyectaron en el flujo CU-04.

## Estado

PASS para el recorrido Android sintético del logger de CU-04. La AVD Samsung_A06-L, ADB y el daemon Gradle se apagaron al terminar; la memoria física libre regresó a aproximadamente 7,3 GiB. No queda emulador ni Docker activo.

**No cubierto:** recorrido completo de alta con CI/capturas, teléfono físico y backend real. H10.S4.M1 permanece `A MEDIAS` hasta completar esa superficie exacta; este resultado no se presenta como validación integral del alta.

Después se quitó del test un marcador de diagnóstico Dart no sensible. Análisis/formato y `autenticar_test.dart` 6/6 volvieron a pasar con la fuente final. Una repetición Android posterior no llegó a ejecutar el test: el AVD quedó `offline` durante el arranque. Se detuvieron sus procesos exactos y no se insistió; la integración y el conteo de logs documentados arriba corresponden a la misma ruta CU-04 y al mismo código productivo, pero a la versión del test que aún tenía ese marcador inocuo. Por esta limitación y por no cubrir el alta completa, el estado sigue parcial.
