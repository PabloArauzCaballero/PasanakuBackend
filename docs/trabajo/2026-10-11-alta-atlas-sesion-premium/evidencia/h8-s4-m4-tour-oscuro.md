# H8.S4.M4 — tour oscuro

Resultado buscado: las cuatro láminas del tour son legibles, completas y sin desbordes en tema oscuro, 360 × 740 dp, texto del sistema al 130 %. Kill-test: prueba widget con el tema real, movimiento reducido y cuatro capturas inspeccionadas individualmente.

IN: prueba golden dirigida bajo `apps/movil/test/goldens/`, cuatro PNG del tour, este reporte e inventario H8.S4.M4. OUT: rediseño del tour sin defecto demostrado, backend, flujo de alta posterior, baselines previas y cierre del resto de pantallas.

Skills locales leídas antes del cambio: `flutter-testing`, `flutter-theming`, `visual-proof`, `visual-regression-testing`.

Estado: cuatro capturas inspeccionadas sin defecto visible; H8.S4.M4 continúa A MEDIAS. Fecha: 13 de octubre de 2026, Windows, árbol local sobre `08042090`. Los PNG son evidencia visual local, no baselines canónicas Linux. El montaje usa HTTP simulado y movimiento reducido; no ejercita backend ni navegación a registro, y no alcanza el peldaño de verificación de la app en dispositivo.

| Celda: 360 × 740 dp, oscuro, texto 130 % | Inspección visual del PNG | Archivo |
|---|---|---|
| Lámina 1, «sin el cuaderno» | OK: dibujo, titular, párrafo, puntos y «Siguiente» completos y legibles; sin superposición | [PNG](./h8-tour-oscuro-lamina-1-360-130.png) |
| Lámina 2, «en custodia» | OK: escudo/comprobante, título y párrafo visibles; botón y puntos libres | [PNG](./h8-tour-oscuro-lamina-2-360-130.png) |
| Lámina 3, «a la vista de todos» | OK: sorteo, título y explicación completos; sin recorte | [PNG](./h8-tour-oscuro-lamina-3-360-130.png) |
| Lámina 4, «el grupo no se frena» | OK: ilustración, título y explicación completos; «Empezar» visible y «Saltar» ausente | [PNG](./h8-tour-oscuro-lamina-4-360-130.png) |

Captura dirigida:

```text
flutter test --no-pub --concurrency=1 --reporter expanded --update-goldens test/goldens/h8_tour_oscuro_test.dart
00:01 +1: All tests passed!
```

Confirmación posterior sin actualizar goldens y regresión funcional vecina:

```text
flutter test --no-pub --concurrency=1 --reporter expanded test/goldens/h8_tour_oscuro_test.dart test/widget/tour_test.dart
00:03 +3: All tests passed!

dart analyze --fatal-infos test/goldens/h8_tour_oscuro_test.dart
Analyzing h8_tour_oscuro_test.dart...
No issues found!
```

No cubierto: tema claro en este test, otros tamaños/escalas, gestos con animación normal, capturas en Android/iOS, salida real a registro y consola/red de la app. La prueba widget verifica tema, escala, índice, texto y ausencia de excepción, pero no equivale a E2E.
