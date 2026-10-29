import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/billetera/tarjeta_de_extracto.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

Widget _app(Widget hijo, Brightness b) => MaterialApp(
  theme: temaDesde(b == Brightness.dark ? Tokens.oscuro : Tokens.claro, b),
  home: Scaffold(
    body: Padding(padding: const EdgeInsets.all(Espacio.s4), child: hijo),
  ),
);

void main() {
  final desde = DateTime.utc(2026, 9, 1, 12);
  final hasta = DateTime.utc(2026, 9, 30, 12);

  for (final b in Brightness.values) {
    testWidgets(
      'el extracto muestra período, saldo y movimientos (${b.name})',
      (tester) async {
        await tester.pumpWidget(
          _app(
            TarjetaDeExtracto(
              desde: desde,
              hasta: hasta,
              saldoFinal: '1250.50',
              moneda: 'BOB',
              cantidadMovimientos: 7,
            ),
            b,
          ),
        );
        expect(find.textContaining('1 sep 2026 – 30 sep 2026'), findsOneWidget);
        expect(find.text('Saldo al cierre del período'), findsWidgets);
        expect(find.textContaining('1.250,50'), findsOneWidget);
        expect(find.text('Movimientos en el período'), findsOneWidget);
        expect(find.text('7'), findsOneWidget);
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets('sin cantidad de movimientos no inventa el renglón', (
    tester,
  ) async {
    await tester.pumpWidget(
      _app(
        TarjetaDeExtracto(
          desde: desde,
          hasta: hasta,
          saldoFinal: '0.00',
          moneda: 'BOB',
        ),
        Brightness.light,
      ),
    );
    expect(find.text('Movimientos en el período'), findsNothing);
  });

  testWidgets('a 200 % y 320 px la cifra baja de tamaño, no se corta', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(320, 640);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    tester.platformDispatcher.textScaleFactorTestValue = 2;
    addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
    await tester.pumpWidget(
      _app(
        SingleChildScrollView(
          child: TarjetaDeExtracto(
            desde: desde,
            hasta: hasta,
            saldoFinal: '123456789.00',
            moneda: 'BOB',
            cantidadMovimientos: 120,
          ),
        ),
        Brightness.light,
      ),
    );
    // `FittedBox` escala por transformación: se mide donde queda dibujada, no su tamaño de layout.
    final cifra = find.textContaining('123.456.789,00');
    expect(tester.getTopRight(cifra).dx, lessThanOrEqualTo(320 - Espacio.s4));
    expect(tester.getTopLeft(cifra).dx, greaterThanOrEqualTo(Espacio.s4));
    expect(tester.takeException(), isNull);
  });
}
