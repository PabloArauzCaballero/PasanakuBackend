# H8.S4.M4 — centro de ayuda en oscuro

Fecha: 2026-10-13. Evidencia de la pantalla `PantallaCentroDeAyuda`; no cierra por sí sola H8.S4.M4.

## Captura y fixture

- Viewport: 360 × 740 dp, DPR 1, escala de texto 130 %, tema oscuro, animaciones deshabilitadas.
- Se cargaron las fuentes reales empaquetadas por la app.
- Tres tutoriales sintéticos deterministas en español; `ProviderScope` reemplaza catálogo, registro y almacenamiento por fixtures locales. Sin backend, red ni datos reales.
- Test: `apps/movil/test/goldens/h8_centro_ayuda_oscuro_test.dart`.

| Estado | Captura | Inspección visual |
|---|---|---|
| Inicio, avance vacío y tutorial recomendado | `h8-ayuda-oscura-360-130-inicio.png` | OK: encabezado, propósito, avance, buscador, filtros y recomendación legibles; la tarjeta continúa fuera del viewport mediante scroll normal. Sin solapamientos ni excepciones. |
| Lista desplazada hasta el último tutorial | `h8-ayuda-oscura-360-130-lista.png` | OK: tarjetas, estados y botones completos y legibles, incluida la última tarjeta; sin recortes ni superposiciones dentro del viewport. |

## Verificación

```text
flutter test --no-pub --concurrency=1 --reporter expanded --update-goldens test/goldens/h8_centro_ayuda_oscuro_test.dart
00:01 +1: All tests passed!

flutter test --no-pub --concurrency=1 --reporter expanded test/goldens/h8_centro_ayuda_oscuro_test.dart
00:01 +1: All tests passed!

dart analyze --fatal-infos test/goldens/h8_centro_ayuda_oscuro_test.dart
No issues found!
```

El test además comprueba tema oscuro, escala 130 %, texto principal y ausencia de excepciones. La inspección abrió ambas capturas luego de generarlas. No se probó en dispositivo físico.

## No cubierto

Tema claro, tablet/escritorio, escala 200 %, catálogo vacío/error, búsqueda sin resultados, teclado real, persistencia real y navegación del tutorial sobre la app. No reemplaza pruebas de integración ni cubre el resto del alcance H8.S4.M4.

Skills consultadas: `flutter-testing`, `flutter-theming`, `pasanaku-diseno`, `visual-proof` y `visual-regression-testing`.
