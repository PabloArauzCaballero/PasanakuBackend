import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/billetera/comprobante_de_retiro.dart';
import 'package:aportaya_movil/pantallas/billetera/textos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

Widget _app(Widget hijo, Brightness b) => MaterialApp(
  theme: temaDesde(b == Brightness.dark ? Tokens.oscuro : Tokens.claro, b),
  home: Scaffold(body: Center(child: hijo)),
);

SalidaRetiro _retiro({String neto = '495.00', String costo = '5.00'}) =>
    SalidaRetiro(
      ordenRetiroId: 'orden-sintetica',
      estado: SalidaRetiroEstadoEnum.PENDIENTE,
      costoRetiro: Dinero(monto: costo, moneda: DineroMonedaEnum.BOB),
      montoNeto: Dinero(monto: neto, moneda: DineroMonedaEnum.BOB),
      retencionId: 'retencion-sintetica',
    );

/// El retiro instruido se lee como «en camino», no como «listo» (regla 91.3), y muestra el
/// neto y el costo tal como los devolvió el servidor. Datos sintéticos.
void main() {
  for (final b in Brightness.values) {
    testWidgets('correcto: dice dónde está, el neto y el costo (${b.name})', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(360, 720);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(
        _app(ComprobanteDeRetiro(resultado: _retiro()), b),
      );
      await tester.pumpAndSettle();
      expect(find.text(TextosBilletera.retiroInstruido), findsOneWidget);
      expect(find.text(TextosBilletera.retiroInstruidoDetalle), findsOneWidget);
      expect(find.textContaining('495,00'), findsOneWidget);
      expect(find.textContaining('5,00'), findsWidgets);
      expect(find.text(TextosBilletera.pasoEnCamino), findsOneWidget);
      expect(tester.takeException(), isNull);
    });
  }

  testWidgets('límite: costo cero se muestra como cero, no se oculta', (
    tester,
  ) async {
    await tester.pumpWidget(
      _app(
        ComprobanteDeRetiro(resultado: _retiro(costo: '0.00')),
        Brightness.light,
      ),
    );
    await tester.pumpAndSettle();
    expect(find.textContaining('0,00'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  test('inválido: el motivo de transferencia nombra lo primero que falta', () {
    expect(
      TextosBilletera.motivoTransferencia(
        hayMonto: false,
        hayDestino: false,
        hayConcepto: false,
      ),
      'Escribí cuánto querés transferir.',
    );
    expect(
      TextosBilletera.motivoTransferencia(
        hayMonto: true,
        hayDestino: false,
        hayConcepto: true,
      ),
      'Indicá a quién le transferís.',
    );
    expect(
      TextosBilletera.motivoTransferencia(
        hayMonto: true,
        hayDestino: true,
        hayConcepto: true,
      ),
      isNull,
    );
  });
}
