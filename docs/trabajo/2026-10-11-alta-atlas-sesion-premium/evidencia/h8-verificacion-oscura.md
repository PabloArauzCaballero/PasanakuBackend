# H8.S4.M4 — estado de verificación en oscuro

Fecha: 2026-10-13. Evidencia de `PantallaDeEstadoDeVerificacion`; no cierra por sí sola H8.S4.M4.

## Capturas y alcance

- Viewport: 360 × 740 dp, DPR 1, texto 130 %, tema oscuro, animaciones deshabilitadas.
- Se cargaron las fuentes reales empaquetadas por la app.
- Se cubrieron `PENDIENTE`, `EN_REVISION`, `APROBADA` y `RECHAZADA`. Cada captura usa un proveedor falso y un ID sintético `a11ce001-…`; el rechazo usa el motivo de fixture `IDENTIDAD_NO_COINCIDE`.
- Test: `apps/movil/test/goldens/h8_verificacion_oscura_test.dart`. No se modifica código de producto, no se inicia backend ni se usa red.

| Estado | Captura | Inspección visual |
|---|---|---|
| PENDIENTE | `h8-verificacion-pendiente-oscura-360-130.png` | OK: título, indicación, caso y acciones completos; sin recorte ni excepción. |
| EN_REVISION | `h8-verificacion-en-revision-oscura-360-130.png` | OK: título, plazo no fijo, caso y acciones legibles; sin recorte ni excepción. |
| APROBADA | `h8-verificacion-aprobada-oscura-360-130.png` | OK: confirmación, caso y acción «Entrar» legibles; sin recorte ni excepción. |
| RECHAZADA | `h8-verificacion-rechazada-oscura-360-130.png` | OK: motivo localizado, caso, reintento y actualización legibles; sin recorte ni excepción. |

## Verificación

```text
flutter test --no-pub --concurrency=1 --reporter expanded --update-goldens test/goldens/h8_verificacion_oscura_test.dart
00:01 +1: All tests passed!

flutter test --no-pub --concurrency=1 --reporter expanded test/goldens/h8_verificacion_oscura_test.dart
00:01 +1: All tests passed!

dart analyze --fatal-infos test/goldens/h8_verificacion_oscura_test.dart
No issues found!
```

Las cuatro imágenes se abrieron y revisaron después de la última generación. El test comprueba tema/escala, título, identificador de caso, acción y ausencia de excepciones. La espera de sondeo de estados no finales no se avanza; los proveedores se liberan al cambiar de fixture.

## No cubierto

Carga y error de red, claro, tablet/escritorio, escala 200 %, dispositivo real y navegación al tocar acciones. La API/sondeo real ya tiene cobertura unitaria separada; estas capturas acreditan únicamente la presentación con datos sintéticos.

Skills consultadas: `flutter-testing`, `flutter-theming`, `pasanaku-diseno`, `visual-proof`, `visual-regression-testing`, `kyc-identity-verification`, `data-privacy-financial` y `synthetic-test-data-generation`.
