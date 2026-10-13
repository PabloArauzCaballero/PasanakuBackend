import 'package:aportaya_movil/pantallas/identidad/pantalla_portada.dart';
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

  for (final ancho in [412, 600]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      final tema = brillo == Brightness.light ? 'claro' : 'oscuro';
      testWidgets('portada principal · $tema · $ancho dp · texto 130 %', (
        tester,
      ) async {
        final alto = ancho == 412 ? 850 : 960;
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
            const PantallaDePortada(),
            dio: dio,
            brillo: brillo,
            movimientoReducido: true,
          ),
        );
        await asentar(tester);

        final contexto = tester.element(find.byType(PantallaDePortada));
        expect(Theme.of(contexto).brightness, brillo);
        expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
        expect(find.text(TextosIdentidad.bienvenidaTitulo), findsOneWidget);
        expect(find.text(TextosIdentidad.portadaCrearCuenta), findsOneWidget);
        expect(find.text(TextosIdentidad.portadaYaTengoCuenta), findsOneWidget);
        expect(tester.takeException(), isNull);

        final garantiaInferior = tester
            .getBottomRight(find.text(TextosIdentidad.portadaGarantiaTurno))
            .dy;
        final botonesSuperior = tester
            .getTopLeft(find.byKey(const ValueKey('portada-botones')))
            .dy;
        expect(
          garantiaInferior,
          lessThanOrEqualTo(botonesSuperior),
          reason: 'las garantías terminan antes de las acciones de portada',
        );

        await expectLater(
          find.byType(PantallaDePortada),
          matchesGoldenFile(
            '$_evidencia'
            'h10-portada-principal-$tema-$ancho-130.png',
          ),
        );
      });
    }
  }
}
