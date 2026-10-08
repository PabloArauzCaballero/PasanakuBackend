import 'package:aportaya_movil/pantallas/identidad/pantalla_verificacion_profunda.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  for (final brillo in [Brightness.light, Brightness.dark]) {
    testWidgets(
      'verificación profunda bloqueada · ${brillo.name} cumple a11y',
      (tester) async {
        tester.view.physicalSize = const Size(320, 760);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        await tester.pumpWidget(
          conApp(
            const PantallaDeVerificacionProfunda(),
            dio: dioSimulado().dio,
            brillo: brillo,
            escalaDeTexto: 2,
          ),
        );
        await tester.pumpAndSettle();

        await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
        await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
        await expectLater(tester, meetsGuideline(textContrastGuideline));
        expect(tester.takeException(), isNull);
      },
    );
  }
}
