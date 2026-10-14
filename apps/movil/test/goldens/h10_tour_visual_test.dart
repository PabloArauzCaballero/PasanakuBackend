import 'package:aportaya_diseno/atomos/barra_de_puntos.dart';
import 'package:aportaya_movil/pantallas/identidad/laminas_del_tour.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_tour.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'cargar_fuentes_de_aplicacion.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h10-visual/';
const _tourH8 =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h8-tour-oscuro-lamina-';

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  for (final ancho in [360, 412, 600]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      final tema = brillo == Brightness.light ? 'claro' : 'oscuro';
      for (var lamina = 0; lamina < laminasDelTour.length; lamina += 1) {
        final indice = lamina;
        testWidgets(
          'tour · lámina ${indice + 1} · $tema · $ancho dp · texto 130 %',
          (tester) async {
            final alto = switch (ancho) {
              360 => 740,
              412 => 850,
              _ => 960,
            };
            tester.view.physicalSize = Size(ancho.toDouble(), alto.toDouble());
            tester.view.devicePixelRatio = 1;
            tester.platformDispatcher.textScaleFactorTestValue = 1.3;
            tester.platformDispatcher.accessibilityFeaturesTestValue =
                const FakeAccessibilityFeatures(disableAnimations: true);
            addTearDown(tester.view.reset);
            addTearDown(tester.platformDispatcher.clearAllTestValues);
            addTearDown(
              tester.platformDispatcher.clearAccessibilityFeaturesTestValue,
            );

            final (:dio, adaptador: _) = dioSimulado();
            await tester.pumpWidget(
              conApp(
                const PantallaDeTour(),
                dio: dio,
                brillo: brillo,
                movimientoReducido: true,
              ),
            );
            await asentar(tester);

            for (var paso = 0; paso < indice; paso += 1) {
              await tester.tap(find.text(TextosIdentidad.tourSiguiente));
              await asentar(tester);
            }

            final pantalla = find.byType(PantallaDeTour);
            final contexto = tester.element(find.byType(Scaffold).first);
            expect(Theme.of(contexto).brightness, brillo);
            expect(
              MediaQuery.textScalerOf(contexto).scale(10),
              closeTo(13, 0.001),
            );
            expect(
              tester.widget<BarraDePuntos>(find.byType(BarraDePuntos)).actual,
              indice,
            );
            expect(find.text(laminasDelTour[indice].texto), findsOneWidget);
            if (indice == laminasDelTour.length - 1) {
              expect(find.text(TextosIdentidad.tourEmpezar), findsOneWidget);
              expect(find.text(TextosIdentidad.tourSiguiente), findsNothing);
              final semantica = tester.ensureSemantics();
              try {
                expect(
                  find.bySemanticsLabel(TextosIdentidad.portadaSaltar),
                  findsNothing,
                );
              } finally {
                semantica.dispose();
              }
              expect(
                tester
                    .widget<AnimatedOpacity>(find.byType(AnimatedOpacity).last)
                    .opacity,
                0,
              );
            } else {
              expect(find.text(TextosIdentidad.tourSiguiente), findsOneWidget);
              expect(find.text(TextosIdentidad.portadaSaltar), findsOneWidget);
            }
            expect(tester.takeException(), isNull);

            final ruta =
                ancho == 360 &&
                    brillo == Brightness.dark &&
                    indice < laminasDelTour.length - 1
                ? '$_tourH8${indice + 1}-360-130.png'
                : '$_evidencia'
                      'h10-tour-lamina-${indice + 1}-$tema-$ancho-130.png';
            await expectLater(pantalla, matchesGoldenFile(ruta));
          },
        );
      }
    }
  }
}
