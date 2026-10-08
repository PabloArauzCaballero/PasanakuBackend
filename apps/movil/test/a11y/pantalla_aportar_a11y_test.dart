import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:aportaya_movil/pantallas/pasanaku/pantalla_aportar.dart';

import '../comun.dart';

const _obligacionId = '68a98585-2854-4b71-80fd-5ef490fe60f6';

void main() {
  for (final brillo in [Brightness.light, Brightness.dark]) {
    testWidgets('aporte · ${brillo.name} cumple las guías de accesibilidad', (
      tester,
    ) async {
      final (:dio, adaptador: _) = dioSimulado();
      _telefono(tester);
      await tester.pumpWidget(
        conApp(
          const PantallaAportar(
            obligacionId: _obligacionId,
            montoDelPeriodo: '125.00',
          ),
          dio: dio,
          brillo: brillo,
        ),
      );
      await tester.pumpAndSettle();

      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
      await expectLater(tester, meetsGuideline(textContrastGuideline));

      await tester.enterText(find.byType(TextField), 'REF-001');
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Revisar aporte'));
      await tester.pumpAndSettle();
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
      await expectLater(tester, meetsGuideline(textContrastGuideline));
    });
  }

  testWidgets('aporte sigue siendo operable con texto al 200 %', (
    tester,
  ) async {
    final (:dio, adaptador: _) = dioSimulado();
    _telefono(tester);
    await tester.pumpWidget(
      conApp(
        const PantallaAportar(
          obligacionId: _obligacionId,
          montoDelPeriodo: '125.00',
        ),
        dio: dio,
        escalaDeTexto: 2,
      ),
    );
    await tester.pumpAndSettle();

    await tester.scrollUntilVisible(
      find.text('Revisar aporte'),
      250,
      scrollable: find.byType(Scrollable).first,
    );
    expect(find.text('Revisar aporte'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}

void _telefono(WidgetTester tester) {
  tester.view.physicalSize = const Size(360, 760);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
}
