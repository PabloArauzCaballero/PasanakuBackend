# H8.S4.M4 — portada y MFA oscuros

Objetivo: capturar e inspeccionar portada y MFA iniciales con tema oscuro, 360 × 740 dp y escala de texto 130 %, sin desbordes ni textos ilegibles. Kill-test: pruebas widget dirigidas con el tema real, más inspección de cada PNG.

IN: prueba golden nueva bajo `apps/movil/test/goldens/`, PNG de portada claro/oscuro y MFA oscuro, corrección local de espaciado en portada, este reporte e inventario H8.S4.M4. OUT: backend, navegación real, otros cambios visuales, baselines existentes y cierre de las demás pantallas.

Skills locales leídas/revisadas para este avance: `flutter-testing`, `flutter-theming`, `visual-proof`, `visual-regression-testing` (además de las registradas en `h8-s4-m4-ingreso-oscuro.md`).

Estado: parcial para H8.S4.M4. Entorno: Windows, árbol local sobre `08042090`; los PNG son evidencia de inspección local, no baselines canónicas Linux. La prueba usa datos simulados y no navega ni llama al backend.

Defecto reproducido antes del ajuste: en `PantallaDePortada`, dos separaciones verticales `Espacio.s6` (32 dp cada una) en la zona principal, más tres pastillas envueltas en filas propias, hacen que el último texto termine a y=580,25 mientras la zona fija de salidas empieza en y=568 a 360 × 740/130 %. El `SingleChildScrollView` conserva acceso por arrastre, pero la captura inicial corta la tercera pastilla. Clase: producto/layout, no rasterizador. Regresión añadida antes del fix:

```text
flutter test --no-pub --concurrency=1 --reporter expanded test/goldens/h8_portada_mfa_oscuro_test.dart
Expected: a value less than or equal to <568.0>
  Actual: <580.2480241670859>
la tercera garantía debe leerse entera antes de las salidas
00:01 +1 -1: Some tests failed.
```

Corrección: las dos separaciones `Espacio.s6` alrededor del titular/descripción en `pantalla_portada.dart` pasaron a `Espacio.s4`. Se conservaron los textos, las tres garantías y las salidas. La prueba geométrica pasó después; el golden previo difirió 16,72 % por el desplazamiento esperado, se abrió la imagen nueva y solo entonces se actualizó esta baseline dirigida.

| Celda | Veredicto visual local | Captura |
|---|---|---|
| Portada, oscuro, 360 × 740 dp, texto 130 %, movimiento reducido | PASS: tres garantías completas y legibles sobre las salidas; sin superposición, recorte ni desborde observado | [PNG](./h8-portada-oscuro-360-130.png) |
| Portada, claro, 360 × 740 dp, texto 130 %, movimiento reducido | PASS: misma disposición sin recorte, contraste y controles legibles | [PNG](./h8-portada-claro-360-130.png) |
| MFA inicial, oscuro, 360 × 740 dp, texto 130 % | PASS: título, seis casillas y ayuda legibles; sin recorte ni desborde observado | [PNG](./h8-mfa-oscuro-360-130.png) |
| MFA, código sintético rechazado, oscuro, 360 × 740 dp, texto 130 % | PASS: mensaje de código inválido, seis casillas y texto de ayuda legibles; error visible por borde y mensaje, sin corte ni desborde | [PNG](./h8-mfa-error-oscuro-360-130.png) |

Verificación posterior a la corrección, sin modo de actualización:

```text
flutter test --no-pub --concurrency=1 --reporter compact test/goldens/h8_portada_mfa_oscuro_test.dart test/widget/portada_test.dart
00:08 +13: All tests passed!

dart analyze --fatal-infos lib/pantallas/identidad/pantalla_portada.dart test/goldens/h8_portada_mfa_oscuro_test.dart
Analyzing pantalla_portada.dart, h8_portada_mfa_oscuro_test.dart...
No issues found!
```

Ampliación del 13-10: se agregó respuesta sintética `422 AP-CU04-04` para capturar el rechazo de OTP. La pantalla muestra el mensaje de dominio «Ese código no sirve: revisalo o pedí uno nuevo.» y conserva las casillas listas para corregir/reintentar. El golden se revisó visualmente; fixture sin credenciales reales ni red.

No cubierto: OTP válido/navegación, teclado del sistema, otros tamaños, MFA claro, consola/red de la app real, hardware Android/iOS y el resto del alcance H8.S4.M4. El alcance completo no se declara verificado.
