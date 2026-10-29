import 'package:aportaya_diseno/organismos/panel_bienvenida.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_bienvenida.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'cargar_fuentes_de_aplicacion.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h10-visual/';

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  for (final ancho in [360, 412, 600]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      final tema = brillo == Brightness.light ? 'claro' : 'oscuro';
      testWidgets('bienvenida principal · $tema · $ancho dp · texto 130 %', (
        tester,
      ) async {
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
            const PantallaDeBienvenida(),
            dio: dio,
            brillo: brillo,
            movimientoReducido: true,
          ),
        );
        await asentar(tester);

        final pantalla = find.byType(PantallaDeBienvenida);
        final contexto = tester.element(pantalla);
        expect(Theme.of(contexto).brightness, brillo);
        expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
        expect(find.text(TextosIdentidad.tituloBienvenida), findsOneWidget);
        expect(find.text('Bienvenida, tu cuenta está lista'), findsOneWidget);
        expect(find.text('Ver mi billetera'), findsOneWidget);
        expect(find.text('Completar mi perfil'), findsOneWidget);
        expect(find.text('Te acreditamos '), findsNothing);
        final panel = tester.getRect(find.byType(PanelBienvenida));
        final areaUtil = tester.getRect(find.byType(SafeArea).first);
        expect(panel.center.dx, closeTo(areaUtil.center.dx, 1));
        expect(panel.center.dy, closeTo(areaUtil.center.dy, 1));
        expect(tester.takeException(), isNull);

        await expectLater(
          pantalla,
          matchesGoldenFile(
            '$_evidencia'
            'h10-bienvenida-principal-$tema-$ancho-130.png',
          ),
        );
      });
    }
  }
}
