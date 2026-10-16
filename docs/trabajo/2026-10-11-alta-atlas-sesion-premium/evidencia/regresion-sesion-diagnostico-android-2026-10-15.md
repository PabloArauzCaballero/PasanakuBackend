# Regresión focalizada de sesión y diagnóstico (2026-10-15)

Se validaron serialmente los cambios locales de sesión observable, guardia de rutas, diagnóstico seguro de respuestas API y navegación de una cuenta pendiente de verificación. No se conectó a TEST, no se mutaron cuentas ni se inició Docker o Android Emulator.

Comando desde `apps/movil`:

```text
flutter test --no-pub --concurrency=1 --reporter expanded test/unidad/diagnostico_api_test.dart test/unidad/acceso_a_rutas_test.dart test/unidad/sesion_observable_test.dart test/identidad/pantalla_estado_verificacion_sesion_test.dart
```

Resultado: `All tests passed!` — 23 pruebas en total: diagnóstico API 3/3, acceso a rutas 5/5, sesión observable 13/13 y estado de verificación 2/2. Los casos cubren canonización/omisión de rutas en logs, ausencia de secretos de sesión, acceso anónimo/activo y cuenta pendiente, hidratación/renovación/cierre de sesión y navegación a la pantalla informativa de verificación.

Esta regresión local no sustituye los recorridos de backend real exigidos por H2.S1.M1 ni el ensayo de 20 minutos/candado en AVD de H10.S2.M3–M4; esas microtareas conservan sus estados actuales.
