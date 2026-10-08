import 'package:aportaya_movil/pantallas/billetera/pantalla_recargar.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_retirar.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_transferir.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  const cuenta = '22222222-2222-4222-8222-222222222222';
  final pantallas = <(String, Widget)>[
    ('Recargar', const PantallaRecargar(cuentaId: cuenta)),
    ('Retirar', const PantallaRetirar(cuentaId: cuenta)),
    ('Transferir', const PantallaTransferir(cuentaOrigenId: cuenta)),
  ];

  for (final (accion, pantalla) in pantallas) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      testWidgets('$accion: teclado y texto 200 % en $brillo', (tester) async {
        tester.view.physicalSize = const Size(1080, 2280);
        tester.view.devicePixelRatio = 3;
        tester.view.viewInsets = const FakeViewPadding(bottom: 900);
        addTearDown(tester.view.reset);
        await tester.pumpWidget(
          conApp(
            pantalla,
            dio: dioSimulado().dio,
            brillo: brillo,
            escalaDeTexto: 2,
          ),
        );
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);

        final campo = find.byType(TextField).last;
        await tester.ensureVisible(campo);
        await tester.pumpAndSettle();
        await tester.tap(campo);
        await tester.pumpAndSettle();
        expect(tester.getBottomRight(campo).dy, lessThanOrEqualTo(460));

        final boton = find.widgetWithText(FilledButton, accion);
        await tester.ensureVisible(boton);
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);
        expect(tester.getBottomRight(boton).dy, lessThanOrEqualTo(460));
      });

      testWidgets('$accion: controles accesibles en $brillo', (tester) async {
        tester.view.physicalSize = const Size(360, 760);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        await tester.pumpWidget(
          conApp(pantalla, dio: dioSimulado().dio, brillo: brillo),
        );
        await tester.pumpAndSettle();

        await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
        await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
        await expectLater(tester, meetsGuideline(textContrastGuideline));
      });
    }
  }
}
