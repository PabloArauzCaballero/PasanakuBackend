# H8.S4.M4 — Grupos / mi estado en oscuro

Fecha: 2026-10-13. Evidencia local de una pantalla del inventario H8.S4.M4; no cierra la microtarea global.

## Cobertura

- Vista: `PantallaMiEstado`, con cuatro estados sintéticos: carga; error con reintento (el segundo intento llega a «Al día»); deuda vigente/una obligación/restricción vigente; y al día/cero obligaciones/sin restricción.
- Viewport: 360 × 740 dp, DPR 1, texto 130 %, animaciones deshabilitadas, tema oscuro.
- Proveedores de aportes y garantía reemplazados por datos falsos deterministas. No se inicia backend ni se emplean datos personales/financieros reales.
- Pruebas: `apps/movil/test/goldens/h8_grupos_mi_estado_oscuro_test.dart`.
- Capturas: `h8-grupos-mi-estado-cargando-oscuro-360-130.png`, `h8-grupos-mi-estado-error-oscuro-360-130.png`, `h8-grupos-mi-estado-deuda-oscuro-360-130-inicio.png`, `h8-grupos-mi-estado-deuda-oscuro-360-130-acciones.png` y `h8-grupos-mi-estado-al-dia-oscuro-360-130.png`. Las dos capturas de deuda son idénticas (mismo SHA-256); se conservan, pero no se cuentan como estados diferentes.

## Inspección visual

Las dos capturas de deuda muestran la misma composición porque todo el contenido y ambas acciones caben en el viewport y `ensureVisible` no necesita desplazar. El título, chip de deuda, importes `Bs 1.240,50`/`Bs 4,44`, obligaciones, aviso de restricción y botones se leen completos; no observé superposición ni overflow. El estado de deuda usa tono de aviso con texto e ícono, no solo color. La captura «al día» muestra chip, saldo `Bs 0,00`, cero obligaciones, ausencia de restricción y ambas acciones; todo el texto se lee completo, sin recorte ni superposición. La zona inferior libre es espacio del viewport, no contenido faltante en esta fixture.

La carga muestra esqueletos diferenciados para aporte y restricción, con acciones disponibles y sin excepción. El error muestra el mensaje genérico en español, ícono de error y botón «Volver a intentar», seguido del estado de éxito tras el retry; la captura no muestra desbordes ni recorte.

## Verificación

```text
dart analyze --fatal-infos test/goldens/h8_grupos_mi_estado_oscuro_test.dart
No issues found!

flutter test --no-pub --concurrency=1 --reporter expanded --update-goldens test/goldens/h8_grupos_mi_estado_oscuro_test.dart
00:00 +4: All tests passed!

flutter test --no-pub --concurrency=1 --reporter expanded test/goldens/h8_grupos_mi_estado_oscuro_test.dart
00:00 +4: All tests passed!
```

Skills aplicadas: `flutter-testing`, `flutter-theming`, `frontend-responsive-layout`, `visual-proof`, `visual-regression-testing` y `pasanaku-diseno`.

## No cubierto

No se cubrieron tema claro, contenido extremo, detalle del grupo, navegación integrada con la barra global, otros viewports, backend ni dispositivo. Las capturas de error y éxito proceden de providers falsos; no acreditan comportamiento del backend. Capturas generadas en Windows; no son baseline canónica Linux. El golden acredita solo estas fixtures y este viewport.
