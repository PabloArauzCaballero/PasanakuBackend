import 'package:aportaya_movil/pantallas/pasanaku/pantalla_aporte_no_disponible.dart';
import 'package:aportaya_movil/pantallas/pasanaku/textos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  for (final brillo in [Brightness.light, Brightness.dark]) {
    testWidgets('aporte bloqueado al 200 % · ${brillo.name} cumple a11y', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(360, 760);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      final (:dio, adaptador: _) = dioSimulado();
      await tester.pumpWidget(
        conApp(
          const PantallaAporteNoDisponible(),
          dio: dio,
          brillo: brillo,
          escalaDeTexto: 2,
        ),
      );
      await tester.pumpAndSettle();

      expect(
        find.text(TextosPasanaku.aporteSinMontoVerificado),
        findsOneWidget,
      );
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
      await expectLater(tester, meetsGuideline(textContrastGuideline));
      expect(tester.takeException(), isNull);
    });
  }
}
