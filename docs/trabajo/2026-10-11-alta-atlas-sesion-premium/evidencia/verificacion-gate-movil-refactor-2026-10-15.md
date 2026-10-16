# Verificación del gate móvil tras refactorizaciones locales

Fecha: 2026-10-15. Rama: `codex/alta-atlas-sesion-premium-2026-10-11`.

## Alcance

Se corrigió el uso de estado/proveedores desde la extensión `AccionesMotorDeTutoriales`: la extensión ahora llama accesores privados definidos en `MotorDeTutoriales`, manteniendo los controles de Riverpod. Se añadió `.toInt()` al índice acotado por `clamp`, cuyo tipo estático es `num`. No se alteró la lógica del tutorial. Los archivos `motor.dart` (133 líneas), `motor_acciones.dart` (107) y `proveedores/sesion.dart` (169) quedan bajo el límite de 200 líneas.

## Resultados

Desde `PasanakuBackend/apps/movil`:

```text
dart analyze lib/dominio/tutoriales/motor.dart lib/dominio/tutoriales/motor_acciones.dart lib/proveedores/tutoriales.dart lib/proveedores/sesion.dart lib/dominio/reclamos_del_token.dart lib/dominio/diagnostico_api.dart lib/pantallas/identidad/dominio/cu04_autenticar.dart lib/main_h6_s3_m8_profile.dart
No issues found!
EXIT_CODE=0
```

```text
flutter test --no-pub test/widget/tutoriales/motor_test.dart test/a11y/tutoriales_a11y_test.dart test/unidad/sesion_observable_test.dart test/unidad/sesion_persiste_test.dart test/unidad/guardian_de_sesion_test.dart test/unidad/diagnostico_api_test.dart
All tests passed!
EXIT_CODE=0
```

La batería ampliada de navegación, estado de verificación, diagnóstico y goldens (360/412/600 dp, claro/oscuro) pasó **53/53**. Tras corregir una sugerencia `const` detectada durante el análisis completo, el test de pantalla de sesión se repitió: **2/2 PASS**. El análisis de todo `apps/movil` terminó con `No issues found!`.

```text
flutter test --no-pub --concurrency=1 --reporter expanded test/identidad/pantalla_estado_verificacion_sesion_test.dart
All tests passed! (2/2)
EXIT_CODE=0

dart analyze
No issues found!
EXIT_CODE=0
```

Desde la raíz `PasanakuBackend`:

```text
python scripts/verificar_frontend.py movil
TODO OK (11/11 comprobaciones)
EXIT_CODE=0

python scripts/verificar_frontend.py diseno
TODO OK (6/6 comprobaciones)
EXIT_CODE=0

git diff --check
EXIT_CODE=0
```

El recuento autoritativo del plan permanece en **197/213 (92,5 %)**: no se cambió el estado de ninguna microtarea, porque estos resultados validan una limpieza del gate y no satisfacen por sí solos los DoD parciales/bloqueados. No se publicaron cambios.
