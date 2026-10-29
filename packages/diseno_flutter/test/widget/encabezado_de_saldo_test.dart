import 'package:aportaya_diseno/organismos/encabezado_de_saldo.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const _custodia = 'En custodia, separado del dinero de la empresa';

/// Montado como en la billetera: dentro de un `ListView`. Ahí, una anotación sin nodo
/// propio se pega a la del saldo (en un `SingleChildScrollView` no pasa).
Widget _enLista(Brightness brillo, {double escala = 1}) => conTema(
  Scaffold(body: ListView(children: [_encabezado()])),
  brillo: brillo,
  escala: escala,
  pantallaEntera: true,
);

Widget _encabezado() => const EncabezadoDeSaldo(
  etiqueta: 'Disponible',
  etiquetaHablada: 'Saldo disponible',
  monto: '1240.00',
  moneda: 'BOB',
  cifras: [(etiqueta: 'Retenido', monto: '150.00')],
  distintivo: _custodia,
);

/// La tarjeta héroe del saldo (ADR-053): lo que ve la persona y lo que oye el lector.
void main() {
  testWidgets('el saldo se anuncia solo; la custodia y lo retenido, aparte', (
    tester,
  ) async {
    final semantica = tester.ensureSemantics();
    await tester.pumpWidget(_enLista(Brightness.light));
    await tester.pumpAndSettle();

    expect(
      find.bySemanticsLabel(RegExp(r'^Saldo disponible: Bs 1\.240,00$')),
      findsOneWidget,
    );
    // La garantía se ve entera, no recortada a un chip.
    expect(find.text(_custodia), findsOneWidget);
    expect(find.bySemanticsLabel(_custodia), findsOneWidget);
    // «Retenido» una sola vez: la etiqueta visible no se repite antes de la cifra.
    expect(
      find.bySemanticsLabel(RegExp(r'^Retenido: Bs 150,00$')),
      findsOneWidget,
    );
    semantica.dispose();
  });

  testWidgets('el ojo tapa el saldo y lo retenido, y lo dice', (tester) async {
    final semantica = tester.ensureSemantics();
    await tester.pumpWidget(_enLista(Brightness.light));
    await tester.pumpAndSettle();

    await tester.tap(find.byTooltip('Ocultar saldo'));
    await tester.pumpAndSettle();

    expect(find.bySemanticsLabel('Saldo oculto'), findsOneWidget);
    expect(find.bySemanticsLabel('Retenido oculto'), findsOneWidget);
    expect(find.textContaining('1.240'), findsNothing);
    expect(find.byTooltip('Mostrar saldo'), findsOneWidget);
    semantica.dispose();
  });

  for (final brillo in Brightness.values) {
    testWidgets('a 320 px con texto al 130 % no desborda: $brillo', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(320, 900);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(_enLista(brillo, escala: 1.3));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
    });
  }
}
