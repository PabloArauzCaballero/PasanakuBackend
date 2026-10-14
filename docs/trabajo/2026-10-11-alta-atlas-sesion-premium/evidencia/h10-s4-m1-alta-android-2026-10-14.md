# H10.S4.M1 — alta sintética y logs Android

Fecha: 2026-10-14. Entorno: Windows, Flutter 3.44.8 / Dart 3.12.2, AVD `Samsung_A06-L` Android API 33. Sin backend real ni datos personales.

## Cobertura

La integración corre sobre Android los casos reales de cliente CU-04, CU-01 y CU-02 con un `HttpClientAdapter` sintético. Recorre login con respuesta sin token de refresco (activa el único logger productivo), recepción y almacenamiento del refresh en el segundo caso, verificación de correo, alta con nombre, teléfono, correo, contraseña, CI y contratos inventados, y las cinco subidas multipart usando archivos sintéticos en `Directory.systemTemp`. El adaptador conserva contadores y un booleano de autorización; no imprime ni guarda cuerpos, contraseñas, tokens, CI o rutas.

## Evidencia literal

Formato y análisis del archivo de integración:

```text
dart format integration_test/h10_s4_m1_logs_test.dart
Formatted 1 file (0 changed) in 0.05 seconds.

flutter analyze --no-pub --fatal-infos integration_test/h10_s4_m1_logs_test.dart
No issues found! (ran in 41.4s)
```

Integración Android:

```text
flutter drive --no-pub -d emulator-5554 --driver=test_driver/h10_s4_m1_logs_test.dart --target=integration_test/h10_s4_m1_logs_test.dart
I/flutter ( 4034): 00:00 +0: H10.S4.M1 login sintetico no vuelca secretos en logs
I/flutter ( 4034): El servidor no entregó tokenRefresco para la sesión móvil.
I/flutter ( 4034): 00:00 +1: H10.S4.M1 token de refresco queda en sesión sin imprimirse
I/flutter ( 4034): 00:00 +2: H10.S4.M1 alta y cinco fotos sintéticas no vuelcan datos en logs
I/flutter ( 4034): 00:00 +3: (tearDownAll)
I/flutter ( 4034): 00:00 +4: All tests passed!
All tests passed.
```

Inspección de logcat completado: se compararon en memoria diez marcadores sintéticos, sin imprimir sus valores ni líneas crudas:

```text
synthetic_markers_checked=10 leaked_marker_values=0 safe_logger_lines=1 total_log_lines=13359
exit_code=0
```

Inventario estático final de llamadas a logging de producción:

```text
rg -n -i -g '*.dart' 'developer\.log|print\s*\(|debugPrint\s*\(|logger\.|log\.(info|debug|warning|severe|shout|fine|config)' apps/movil/lib packages/diseno_flutter/lib
apps/movil/lib\pantallas\identidad\dominio\cu04_autenticar.dart:83:        debugPrint(
exit_code=0
```

El único mensaje productivo observado es constante y no interpola datos. Los archivos temporales se borran por el flujo de subida; `tearDown` elimina el directorio de prueba si aún existiera. No se pegó el logcat completo para evitar persistir información innecesaria.

## Veredicto y límites

**VEREDICTO: PASS** para la ausencia de valores sintéticos de sesión, credenciales, contacto, CI y ruta local en los logs del proceso Android durante los casos CU-04/CU-01/CU-02. **Peldaño: TESTED** (comportamiento Android del cliente con transporte simulado).

**No cubierto:** backend real, recorrido táctil de todas las pantallas del alta, teléfono físico, ni observación de servicios externos. Esas superficies quedan separadas en H1.S6/H10.S2 y no se afirman cerradas por esta prueba de logging.

Recursos apagados después de la corrida: AVD Samsung_A06-L, daemon ADB y un daemon Gradle (`1 Daemon stopped`). No quedó Docker activo. Skills leídas por instrucción de `AGENTS.md`: `flutter-testing`, `mobile-release-security`, `auth-session-pentest`, `kyc-identity-verification`, `data-privacy-financial`, `security-guardrails`, `file-uploads-media`, `synthetic-test-data-generation`, `evidence-and-verification`, `qa-evidence-reporting`, `unit-testing`, `work-report-md`, `progress-reporting`.
