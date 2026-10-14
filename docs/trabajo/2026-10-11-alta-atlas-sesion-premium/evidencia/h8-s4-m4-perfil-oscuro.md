# H8.S4.M4 — perfil oscuro

Resultado buscado: el perfil muestra correo y acciones legibles en oscuro a 360 × 740 dp y texto 130 %, sin desbordes. Kill-test: render del widget con estado inicial sintético, captura inspeccionada y prueba dirigida sin actualizar golden.

IN: test golden dirigido, PNG y evidencia. OUT: guardado de correo, cierre de sesión real, navegación y backend.

Skills locales leídas en este turno: `flutter-testing`, `flutter-theming`, `visual-proof`, `visual-regression-testing`.

Estado: captura inspeccionada, H8.S4.M4 sigue A MEDIAS. Fecha: 13 de octubre de 2026, Windows, árbol local sobre `08042090`. Fixture sin sesión real; baseline de Windows usada como evidencia visual local, no como baseline canónica Linux.

| Celda | Resultado de inspección | Archivo |
|---|---|---|
| Perfil vacío, oscuro, 360 × 740 dp, texto 130 % | PASS visual local: etiqueta y campo Correo, Guardar, Cambiar contraseña, Ver la guía otra vez, Cerrar sesión y Dar de baja tu cuenta se leen completos; controles separados y sin solapamientos ni desborde | [PNG](./h8-perfil-oscuro-360-130.png) |

Captura dirigida:

```text
flutter test --no-pub --concurrency=1 --reporter expanded --update-goldens test/goldens/h8_perfil_oscuro_test.dart
00:01 +1: All tests passed!
```

Confirmación posterior y regresión vecina:

```text
flutter test --no-pub --concurrency=1 --reporter expanded test/goldens/h8_perfil_oscuro_test.dart test/widget/pantalla_perfil_cerrar_sesion_test.dart
00:02 +2: All tests passed!

dart analyze --fatal-infos test/goldens/h8_perfil_oscuro_test.dart lib/pantallas/identidad/pantalla_perfil.dart
Analyzing h8_perfil_oscuro_test.dart, pantalla_perfil.dart...
No issues found!
```

No cubierto: tema claro, correo/contenido largo, teclado, guardar real, baja/cierre de sesión real, navegación, otros viewports, consola/red de la app y dispositivo.
