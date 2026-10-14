# H8.S4.M4 — bienvenida oscura

Resultado buscado: bienvenida tras alta legible y sin desbordes en tema oscuro, 360 × 740 dp y texto al 130 %. Kill-test: captura del widget con el tema real, inspección del PNG y prueba sin actualización de golden.

IN: prueba golden nueva bajo `apps/movil/test/goldens/`, una captura y esta evidencia. OUT: importes de bono, backend, cierre del alta en hardware, baselines ajenas y resto de pantallas.

Skills locales leídas en este turno: `flutter-testing`, `flutter-theming`, `visual-proof`, `visual-regression-testing`.

Estado: captura inspeccionada, H8.S4.M4 todavía A MEDIAS. Fecha: 13 de octubre de 2026, Windows, árbol local sobre `08042090`. Datos sintéticos; la captura local no es baseline canónica Linux.

| Celda | Resultado de inspección | Archivo |
|---|---|---|
| Bienvenida, oscuro, 360 × 740 dp, texto 130 %, cuenta nueva sin bono | Título, saludo y dos acciones legibles; sin recorte ni superposición visible. El panel ocupa casi toda la altura y queda mucho espacio vacío porque el bono aún es `null` en el producto; eso no prueba un estado con bono. | [PNG](./h8-bienvenida-oscuro-360-130.png) |

Captura dirigida:

```text
flutter test --no-pub --concurrency=1 --reporter expanded --update-goldens test/goldens/h8_bienvenida_oscuro_test.dart
00:00 +1: All tests passed!
```

Confirmación posterior sin actualización:

```text
flutter test --no-pub --concurrency=1 --reporter expanded test/goldens/h8_bienvenida_oscuro_test.dart
00:00 +1: All tests passed!

dart analyze --fatal-infos test/goldens/h8_bienvenida_oscuro_test.dart
Analyzing h8_bienvenida_oscuro_test.dart...
No issues found!
```

No cubierto: tema claro y otros tamaños/escalas en este test, transición real desde el alta, navegación de las acciones, bono definido por backend, red/consola reales y dispositivo físico. Una captura con HTTP simulado no prueba el flujo completo.
