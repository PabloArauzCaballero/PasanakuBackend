import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cu13_consultar_saldo.dart';
import 'package:aportaya_movil/dominio/cu15_emitir_extracto.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_extracto.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
// `Override` solo sale de `riverpod` (dependencia directa de flutter_riverpod).
// ignore: depend_on_referenced_packages
import 'package:riverpod/misc.dart' show Override;

Dinero _bob(String m) => Dinero(monto: m, moneda: DineroMonedaEnum.BOB);

void main() {
  final rango = DateTimeRange(
    start: DateTime(2026, 9, 1),
    end: DateTime(2026, 9, 30),
  );
  final parametros = ParametrosExtracto(
    cuentaId: 'c1',
    desde: rango.start,
    hasta: rango.end,
  );

  Future<void> montar(
    WidgetTester tester, {
    DateTimeRange? inicial,
    List<Override> overrides = const [],
  }) async {
    tester.view.physicalSize = const Size(1080, 2280);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(
      ProviderScope(
        // Riverpod 3 reintenta solo los proveedores que fallan: sin esto el error no llega.
        retry: (_, _) => null,
        overrides: overrides,
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: PantallaExtracto(cuentaId: 'c1', rangoInicial: inicial),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('sin período pide elegirlo, con una acción primaria', (
    tester,
  ) async {
    await montar(tester);
    expect(find.text('Elegí un período'), findsOneWidget);
    expect(find.text('Generar extracto'), findsOneWidget);
  });

  testWidgets('con período: saldo al cierre, certificado y detalle por día', (
    tester,
  ) async {
    await montar(
      tester,
      inicial: rango,
      overrides: [
        extractoProvider(parametros).overrideWith(
          (ref) async => SalidaExtracto(
            cuentaBilleteraId: 'c1',
            desde: rango.start,
            hasta: rango.end,
            saldoFinal: _bob('1250.50'),
            cantidadMovimientos: 1,
            emitido: true,
          ),
        ),
        movimientosBilleteraProvider('c1').overrideWith(
          (ref) async => ListaMovimientosBilletera(
            cuentaId: 'c1',
            movimientos: [
              MovimientoBilletera(
                id: 'm1',
                sentido: MovimientoBilleteraSentidoEnum.CREDITO,
                monto: _bob('100.00'),
                saldoDisponiblePosterior: _bob('100.00'),
                saldoRetenidoPosterior: _bob('0.00'),
                glosa: 'Recarga de prueba',
                registradoEn: DateTime(2026, 9, 12, 10),
              ),
            ],
          ),
        ),
      ],
    );
    expect(find.text('Saldo al cierre del período'), findsWidgets);
    expect(find.text('Compartir certificado de saldo'), findsOneWidget);
    expect(find.text('Detalle del período'), findsOneWidget);
    expect(find.text('12 sep 2026'), findsOneWidget);
    expect(find.text('Recarga de prueba'), findsOneWidget);
    expect(find.text('Elegir otro período'), findsOneWidget);
  });

  testWidgets('si el servidor falla, el error se puede reintentar', (
    tester,
  ) async {
    await montar(
      tester,
      inicial: rango,
      overrides: [
        extractoProvider(
          parametros,
        ).overrideWith((ref) async => throw Exception('caído')),
      ],
    );
    expect(find.text('Elegí un período'), findsNothing);
    expect(find.byType(EstadoError), findsOneWidget);
  });
}
