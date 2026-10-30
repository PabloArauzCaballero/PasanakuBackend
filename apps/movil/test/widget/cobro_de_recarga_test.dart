import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/dinero/formatear.dart';
import 'package:aportaya_movil/dominio/puertos/haptica.dart';
import 'package:aportaya_movil/pantallas/billetera/cobro_de_recarga.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const cuenta = '11111111-1111-4111-8111-111111111111';
const orden = '9b3e8dff-d7e2-4c41-8cdc-b1be7320ca40';
const rutaSaldo = '/billetera/$cuenta/saldo';
const rutaPago = '/billetera/recargas/$orden/pago-de-prueba';

class _Haptica implements Haptica {
  int exitos = 0;
  @override
  Future<void> seleccion() async {}
  @override
  Future<void> botonPrimario() async {}
  @override
  Future<void> avisoDeCredencialesIncorrectas() async {}
  @override
  Future<void> exito() async => exitos++;
  @override
  Future<void> errorQueBloqueaDinero() async {}
}

/// La orden con QR que devuelve el servidor; [origen] PRUEBA (TEST) o BANCO (el banco real).
SalidaRecarga _orden({String origen = 'PRUEBA'}) {
  final cuerpo = Map<String, dynamic>.from(
    ejemplo('nucleo-financiero', 'solicitarRecarga', 'qrDePrueba')['cuerpo'],
  );
  cuerpo['cobroQr'] = <String, dynamic>{
    ...(cuerpo['cobroQr'] as Map<String, dynamic>),
    'origen': origen,
  };
  return SalidaRecarga.fromJson(cuerpo);
}

Map<String, dynamic> _saldo(String monto) => {
  ...ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
  'disponible': {'monto': monto, 'moneda': 'BOB'},
};

Map<String, dynamic> _acreditacion(String saldoDespues) => {
  'ordenRecargaId': orden,
  'transaccionId': 'b77459fb-a680-4709-81f4-1ef25df4a104',
  'saldoDespues': {'monto': saldoDespues, 'moneda': 'BOB'},
};

/// El importe que la pantalla muestra en este cuadro, en centavos.
int _centavosEnPantalla(WidgetTester tester) {
  final texto = tester
      .widgetList<Text>(find.textContaining('Bs '))
      .map((t) => t.data!)
      .firstWhere((t) => t != 'Bs 150,00'); // el «a acreditar» no es el saldo
  return centavosDe(desformatearMonto(texto));
}

Future<({Dio dio, _Haptica haptica, List<String?> claves})> _montar(
  WidgetTester tester, {
  SalidaRecarga? conOrden,
  bool movimientoReducido = false,
  int estadoDelPago = 200,
}) async {
  final (:dio, :adaptador) = dioSimulado();
  final claves = <String?>[];
  dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (o, h) {
        if (o.path == rutaPago) {
          claves.add(o.headers['Idempotency-Key'] as String?);
        }
        h.next(o);
      },
    ),
  );
  adaptador.onGet(rutaSaldo, (s) => s.reply(200, _saldo('500.00')));
  adaptador.onPost(
    rutaPago,
    (s) => estadoDelPago == 200
        ? s.reply(200, _acreditacion('650.00'))
        : s.reply(422, {
            'codigo': 'AP-CU10-05',
            'mensaje': 'Esa recarga no tiene un QR de prueba.',
            'trazaId': 'traza-pago-de-prueba',
          }),
  );
  final haptica = _Haptica();
  await tester.pumpWidget(
    conApp(
      Scaffold(
        body: CobroDeRecarga(orden: conOrden ?? _orden(), cuentaId: cuenta),
      ),
      dio: dio,
      haptica: haptica,
      movimientoReducido: movimientoReducido,
    ),
  );
  await asentar(tester);
  return (dio: dio, haptica: haptica, claves: claves);
}

void main() {
  testWidgets(
    'muestra el QR de la orden, el sello de prueba y el saldo actual',
    (tester) async {
      await _montar(tester);

      expect(
        find.bySemanticsLabel('Código QR para pagar tu recarga'),
        findsOneWidget,
      );
      expect(find.text('QR de prueba'), findsOneWidget);
      expect(find.text('Simular pago'), findsOneWidget);
      expect(find.text('Bs 150,00'), findsOneWidget); // lo que se acredita
      expect(find.text('Bs 500,00'), findsOneWidget); // el saldo de hoy
    },
  );

  testWidgets(
    'simular el pago sube el saldo DESDE el anterior, no desde cero, y termina exacto',
    (tester) async {
      final m = await _montar(tester);

      await tester.ensureVisible(find.text('Simular pago'));
      await tester.tap(find.text('Simular pago'));
      await tester.pump(); // sale el pedido
      await tester.pump(
        const Duration(milliseconds: 250),
      ); // responde el servidor
      await tester.pump(
        const Duration(milliseconds: 60),
      ); // a mitad de la cuenta

      final aMitad = _centavosEnPantalla(tester);
      expect(
        aMitad,
        inInclusiveRange(50000, 65000),
        reason: 'nunca vuelve a cero',
      );

      await tester.pumpAndSettle();
      expect(find.text('Bs 650,00'), findsOneWidget);
      expect(find.text('Tu recarga se acreditó.'), findsOneWidget);
      expect(find.text('Simular pago'), findsNothing);
      expect(m.claves, hasLength(1));
      expect(m.claves.single, isNotNull);
      expect(m.haptica.exitos, 1);
    },
  );

  testWidgets(
    'si el pago falla, lo dice con su texto, el saldo no cambia y el reintento usa la misma clave',
    (tester) async {
      final m = await _montar(tester, estadoDelPago: 422);

      await tester.ensureVisible(find.text('Simular pago'));
      await tester.tap(find.text('Simular pago'));
      await asentar(tester);

      expect(find.text('Bs 500,00'), findsOneWidget);
      expect(find.text('Tu recarga se acreditó.'), findsNothing);
      expect(
        find.text(
          'No pudimos confirmar esta recarga. Si pasó mucho tiempo, empezá una recarga nueva.',
        ),
        findsOneWidget,
      );
      // Una sola acción: el reintento ES volver a simular el pago.
      expect(find.text('Simular pago'), findsNothing);
      expect(find.text('Volver a intentar'), findsOneWidget);

      await tester.ensureVisible(find.text('Volver a intentar'));
      await tester.tap(find.text('Volver a intentar'));
      await asentar(tester);

      expect(m.claves, hasLength(2));
      expect(
        m.claves.toSet(),
        hasLength(1),
        reason: 'misma clave de idempotencia',
      );
    },
  );

  testWidgets('un QR del banco no ofrece simular el pago ni lleva sello', (
    tester,
  ) async {
    await _montar(tester, conOrden: _orden(origen: 'BANCO'));

    expect(
      find.bySemanticsLabel('Código QR para pagar tu recarga'),
      findsOneWidget,
    );
    expect(find.text('QR de prueba'), findsNothing);
    expect(find.text('Simular pago'), findsNothing);
  });

  testWidgets('con movimiento reducido, el saldo nuevo aparece entero de una', (
    tester,
  ) async {
    await _montar(tester, movimientoReducido: true);

    await tester.ensureVisible(find.text('Simular pago'));
    await tester.tap(find.text('Simular pago'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 250));
    await tester.pump();

    expect(find.text('Bs 650,00'), findsOneWidget);
  });
}
