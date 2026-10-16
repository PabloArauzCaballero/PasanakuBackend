import 'package:aportaya_movil/pantallas/identidad/pantalla_bienvenida.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'cargar_fuentes_de_aplicacion.dart';

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  testWidgets('bienvenida oscura · 360 px · texto 130 %', (tester) async {
    tester.view.physicalSize = const Size(360, 740);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 1.3;
    addTearDown(tester.view.reset);
    addTearDown(tester.platformDispatcher.clearAllTestValues);

    final (:dio, adaptador: _) = dioSimulado();
    await tester.pumpWidget(
      conApp(const PantallaDeBienvenida(), dio: dio, brillo: Brightness.dark),
    );
    await asentar(tester);

    final contexto = tester.element(find.byType(Scaffold).first);
    expect(Theme.of(contexto).brightness, Brightness.dark);
    expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
    expect(find.text(TextosIdentidad.tituloBienvenida), findsOneWidget);
    expect(find.text('Ir a tu billetera'), findsOneWidget);
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(PantallaDeBienvenida),
      matchesGoldenFile(
        '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
        'evidencia/h8-bienvenida-oscuro-360-130.png',
      ),
    );
  });
}
