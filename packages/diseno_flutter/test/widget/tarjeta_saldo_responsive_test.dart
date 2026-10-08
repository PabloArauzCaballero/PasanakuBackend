import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/moleculas/tarjeta_saldo.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  for (final brillo in [Brightness.light, Brightness.dark]) {
    for (final ancho in [320, 360]) {
      testWidgets(
        'tarjeta de saldo muestra cifras y acciones a $ancho dp y 200 % en $brillo',
        (tester) async {
          tester.view.physicalSize = Size(ancho.toDouble(), 760);
          tester.view.devicePixelRatio = 1;
          addTearDown(tester.view.reset);
          await tester.pumpWidget(
            conTema(
              TarjetaSaldo(
                disponible: '1240.00',
                retenido: '150.00',
                enPasanakus: '2400.00',
                moneda: 'BOB',
                accionPrincipal: Boton(
                  texto: 'Recargar',
                  variante: BotonVariante.primario,
                  onPressed: () {},
                ),
                accionSecundaria: Boton(
                  texto: 'Retirar',
                  variante: BotonVariante.sobreVerde,
                  onPressed: () {},
                ),
              ),
              brillo: brillo,
              escala: 2,
            ),
          );
          await tester.pumpAndSettle();

          expect(
            find.bySemanticsLabel('Saldo disponible: Bs 1.240,00'),
            findsOneWidget,
          );
          expect(find.text('Recargar'), findsOneWidget);
          expect(find.text('Retirar'), findsOneWidget);
          expect(tester.takeException(), isNull);
          await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
          await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
          await expectLater(tester, meetsGuideline(textContrastGuideline));
        },
      );
    }
  }

  testWidgets('un importe largo conserva su cifra completa a 320 dp y 200 %', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(320, 760);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      conTema(
        const TarjetaSaldo(
          disponible: '999999999999.99',
          retenido: '999999999999.99',
          moneda: 'BOB',
        ),
        escala: 2,
      ),
    );
    await tester.pumpAndSettle();
    expect(
      find.bySemanticsLabel('Saldo disponible: Bs 999.999.999.999,99'),
      findsOneWidget,
    );
    expect(tester.takeException(), isNull);
  });
}
