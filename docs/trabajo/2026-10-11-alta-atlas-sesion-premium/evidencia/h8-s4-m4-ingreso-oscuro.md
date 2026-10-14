# H8.S4.M4 — ingreso oscuro, captura dirigida

Resultado buscado: una persona que abre ingreso con tema oscuro en 360 × 740 dp y texto del sistema al 130 % ve el formulario legible y sin desborde. Kill-test: renderizar la pantalla real en widget test, comprobar tema/escala, ausencia de excepción y revisar el PNG generado.

IN: `apps/movil/test/goldens/h8_ingreso_oscuro_test.dart`, captura de ingreso oscuro y esta evidencia. OUT: rediseño de la pantalla, backend, capturas del resto del alcance y cambios de baselines existentes.

Skills locales leídas antes del cambio: `skills-router`, `flutter-testing`, `flutter-theming`, `visual-regression-testing`, `visual-proof`, `evidence-and-verification`, `agent-resource-control`, `scope-discipline`, `factual-discovery`, `outcome-first`, `context-thrift`, `native-code-patterns`, `anti-hallucination-guard`, `qa-evidence-reporting`; regla `95-frontend.md`.

Entorno: Windows, Flutter 3.44.8; árbol local sobre `08042090`. Datos sintéticos, campos vacíos y cliente HTTP simulado; no es una sesión real en dispositivo. La imagen de referencia se generó localmente y no se declara baseline canónica Linux.

| Caso | Veredicto | Evidencia |
|---|---|---|
| Ingreso, oscuro, 360 × 740, texto 130 %, formulario vacío | PASS en widget test y revisión visual de esa celda | [captura](./h8-ingreso-oscuro-360-130.png) |
| Ingreso, oscuro, 360 × 740, texto 130 %, credenciales sintéticas rechazadas (`AP-CU04-01`) | PASS: mensaje específico y campos/acciones visibles, sin recorte ni desborde; movimiento reducido y haptics falsos | [captura](./h8-ingreso-error-oscuro-360-130.png) |

Inspección del PNG: título, etiquetas, campos, ayuda, aviso de custodia y acciones se leen; no hay texto recortado, controles superpuestos ni bordes invisibles. «Ingresar» está deshabilitado en el estado vacío, coherente con el formulario. No se detectó desborde. Esta observación solo vale para la celda indicada.

Comando de captura:

```text
flutter test --no-pub --concurrency=1 --reporter expanded --update-goldens test/goldens/h8_ingreso_oscuro_test.dart
00:01 +1: All tests passed!
```

Confirmación posterior, sin actualizar baseline:

```text
flutter test --no-pub --concurrency=1 --reporter expanded test/goldens/h8_ingreso_oscuro_test.dart
00:01 +1: All tests passed!
```

Análisis del archivo nuevo:

```text
dart analyze --fatal-infos test/goldens/h8_ingreso_oscuro_test.dart
Analyzing h8_ingreso_oscuro_test.dart...
No issues found!
```

La ampliación del 13-10 usa un adaptador HTTP simulado con `422 AP-CU04-01`, un teléfono y contraseña sintéticos y una implementación haptica sin efecto. La contraseña permanece enmascarada. No se accede a backend ni plataforma física.

Comando verificado para las dos fixtures:

```text
flutter test --no-pub --concurrency=1 --reporter expanded test/goldens/h8_ingreso_oscuro_test.dart
00:00 +2: All tests passed!

dart analyze --fatal-infos test/goldens/h8_ingreso_oscuro_test.dart
No issues found!
```

No cubierto: ingreso con teclado abierto, otros errores/estados y tamaños; ejecución en Android/iOS; consola/red reales; baselines en Linux; las demás pantallas de H8.S4.M4. El inventario del resto del alcance está en `h8-s4-m4-inventario.md`. H8.S4.M4 permanece parcial.
