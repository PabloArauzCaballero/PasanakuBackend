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
    'evidencia/';

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  testWidgets('cuatro láminas del tour oscuro · 360 px · texto 130 %', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 740);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 1.3;
    addTearDown(tester.view.reset);
    addTearDown(tester.platformDispatcher.clearAllTestValues);

    final (:dio, adaptador: _) = dioSimulado();
    await tester.pumpWidget(
      conApp(
        const PantallaDeTour(),
        dio: dio,
        brillo: Brightness.dark,
        movimientoReducido: true,
      ),
    );
    await asentar(tester);

    for (var i = 0; i < laminasDelTour.length; i++) {
      final contexto = tester.element(find.byType(Scaffold).first);
      expect(Theme.of(contexto).brightness, Brightness.dark);
      expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
      expect(
        tester.widget<BarraDePuntos>(find.byType(BarraDePuntos)).actual,
        i,
      );
      expect(find.text(laminasDelTour[i].texto), findsOneWidget);
      expect(tester.takeException(), isNull);
      await expectLater(
        find.byType(PantallaDeTour),
        matchesGoldenFile(
          '${_evidencia}h8-tour-oscuro-lamina-${i + 1}-360-130.png',
        ),
      );

      if (i < laminasDelTour.length - 1) {
        await tester.tap(find.text(TextosIdentidad.tourSiguiente));
        await tester.pumpAndSettle();
      }
    }
    expect(find.text(TextosIdentidad.tourEmpezar), findsOneWidget);
  });
}
