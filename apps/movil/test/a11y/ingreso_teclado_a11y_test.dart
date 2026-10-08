import 'package:aportaya_movil/pantallas/identidad/pantalla_sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  for (final brillo in [Brightness.light, Brightness.dark]) {
    testWidgets('ingreso con teclado al 200 % · ${brillo.name} cumple a11y', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(320, 760);
      tester.view.devicePixelRatio = 1;
      tester.view.viewInsets = const FakeViewPadding(bottom: 300);
      addTearDown(tester.view.reset);

      await tester.pumpWidget(
        conApp(
          const PantallaDeSesion(),
          dio: dioSimulado().dio,
          brillo: brillo,
          escalaDeTexto: 2,
        ),
      );
      await tester.ensureVisible(find.byType(TextField).at(0));
      await tester.pumpAndSettle();
      await tester.tap(find.byType(TextField).at(0));
      await tester.testTextInput.receiveAction(TextInputAction.next);
      await tester.pumpAndSettle();

      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
      await expectLater(tester, meetsGuideline(textContrastGuideline));
      expect(tester.takeException(), isNull);
    });
  }
}
