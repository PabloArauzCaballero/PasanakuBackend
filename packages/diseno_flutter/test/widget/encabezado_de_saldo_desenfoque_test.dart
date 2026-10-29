import 'package:aportaya_diseno/atomos/monto_que_sube.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/organismos/encabezado_de_saldo.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const _encabezado = EncabezadoDeSaldo(
  etiqueta: 'Disponible',
  etiquetaHablada: 'Saldo disponible',
  monto: '1240.00',
  moneda: 'BOB',
  cifras: [(etiqueta: 'Retenido', monto: '150.00')],
  distintivo: 'En custodia',
);

Finder _desenfocados() => find.descendant(
  of: find.byType(EncabezadoDeSaldo),
  matching: find.byWidgetPredicate((w) => w is ImageFiltered && w.enabled),
);

Widget _conMovimiento({required bool reducido}) => Builder(
  builder: (context) => MediaQuery(
    data: MediaQuery.of(context).copyWith(disableAnimations: reducido),
    child: _encabezado,
  ),
);

void main() {
  testWidgets('a mitad del cambio la cifra se desenfoca mientras se funde', (
    t,
  ) async {
    await t.pumpWidget(conTema(_conMovimiento(reducido: false)));
    await t.tap(find.byTooltip('Ocultar saldo'));
    await t.pump();
    await t.pump(Movimiento.ocultarSaldo ~/ 2);
    expect(_desenfocados(), findsWidgets);
  });

  testWidgets('al terminar no queda nada desenfocado', (t) async {
    await t.pumpWidget(conTema(_conMovimiento(reducido: false)));
    await t.tap(find.byTooltip('Ocultar saldo'));
    await t.pumpAndSettle();
    expect(_desenfocados(), findsNothing);
    expect(find.bySemanticsLabel('Saldo oculto'), findsOneWidget);
  });

  testWidgets(
    'con movimiento reducido el cambio es inmediato, sin desenfoque',
    (t) async {
      await t.pumpWidget(conTema(_conMovimiento(reducido: true)));
      await t.tap(find.byTooltip('Ocultar saldo'));
      await t.pump();
      expect(_desenfocados(), findsNothing);
      expect(find.bySemanticsLabel('Saldo oculto'), findsOneWidget);
    },
  );

  testWidgets('la cifra que se va conserva su estado: no repite el rodillo', (
    t,
  ) async {
    // Regresión: el filtro aparecía y desaparecía del árbol, la cifra se recreaba y su
    // rodillo arrancaba de cero — durante el cambio se leían importes que no eran.
    await t.pumpWidget(conTema(_conMovimiento(reducido: false)));
    await t.pumpAndSettle();
    final antes = t.state(find.byType(MontoQueSube));
    await t.tap(find.byTooltip('Ocultar saldo'));
    await t.pump();
    await t.pump(Movimiento.ocultarSaldo ~/ 4);
    expect(t.state(find.byType(MontoQueSube)), same(antes));
  });
}
