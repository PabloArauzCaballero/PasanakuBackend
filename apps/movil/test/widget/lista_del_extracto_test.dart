import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cu13_consultar_saldo.dart';
import 'package:aportaya_movil/dominio/puertos/compartir.dart';
import 'package:aportaya_movil/pantallas/billetera/boton_de_certificado.dart';
import 'package:aportaya_movil/pantallas/billetera/lista_del_extracto.dart';
import 'package:aportaya_movil/proveedores/compartir.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
// `Override` solo sale de `riverpod` (dependencia directa de flutter_riverpod).
// ignore: depend_on_referenced_packages
import 'package:riverpod/misc.dart' show Override;
import 'package:flutter_test/flutter_test.dart';

MovimientoBilletera _mov(
  String id,
  DateTime cuando, {
  bool credito = true,
}) => MovimientoBilletera(
  id: id,
  sentido: credito
      ? MovimientoBilleteraSentidoEnum.CREDITO
      : MovimientoBilleteraSentidoEnum.DEBITO,
  monto: Dinero(monto: '10.00', moneda: DineroMonedaEnum.BOB),
  saldoDisponiblePosterior: Dinero(monto: '0.00', moneda: DineroMonedaEnum.BOB),
  saldoRetenidoPosterior: Dinero(monto: '0.00', moneda: DineroMonedaEnum.BOB),
  glosa: 'Glosa $id',
  registradoEn: cuando,
);

class _Compartir implements Compartir {
  _Compartir(this.abre);
  final bool abre;
  String? texto;
  @override
  Future<bool> conOtraApp({required String texto}) async {
    this.texto = texto;
    return abre;
  }

  @override
  Future<bool> porSms({
    required String telefono,
    required String texto,
  }) async => abre;
  @override
  Future<bool> porWhatsApp({
    required String telefono,
    required String texto,
  }) async => abre;
}

Widget _app(Widget hijo, List<Override> overrides) => ProviderScope(
  overrides: overrides,
  child: MaterialApp(
    theme: temaDesde(Tokens.claro, Brightness.light),
    home: Scaffold(body: SingleChildScrollView(child: hijo)),
  ),
);

void main() {
  final movs = [
    _mov('a', DateTime(2026, 9, 15, 9)),
    _mov('b', DateTime(2026, 9, 15, 18), credito: false),
    _mov('c', DateTime(2026, 9, 3, 12)),
    _mov('x', DateTime(2026, 10, 3, 12)),
  ];
  final lectura = ListaMovimientosBilletera(cuentaId: 'c1', movimientos: movs);
  final lista = ListaDelExtracto(
    cuentaId: 'c1',
    desde: DateTime(2026, 9, 1),
    hasta: DateTime(2026, 9, 30),
  );
  final overrides = [
    movimientosBilleteraProvider('c1').overrideWith((ref) async => lectura),
  ];

  testWidgets('agrupa por día, deja afuera lo de otro período y filtra', (
    tester,
  ) async {
    await tester.pumpWidget(_app(lista, overrides));
    await tester.pumpAndSettle();
    expect(find.text('15 sep 2026'), findsOneWidget);
    expect(find.text('3 sep 2026'), findsOneWidget);
    expect(find.text('Glosa x'), findsNothing);
    expect(find.text('Glosa a'), findsOneWidget);

    await tester.tap(find.text('Salidas'));
    await tester.pumpAndSettle();
    expect(find.text('Glosa b'), findsOneWidget);
    expect(find.text('Glosa a'), findsNothing);
    expect(find.text('3 sep 2026'), findsNothing);
  });

  testWidgets('un filtro sin resultados orienta, no queda mudo', (
    tester,
  ) async {
    await tester.pumpWidget(
      _app(lista, [
        movimientosBilleteraProvider('c1').overrideWith(
          (ref) async => ListaMovimientosBilletera(
            cuentaId: 'c1',
            movimientos: [_mov('a', DateTime(2026, 9, 15, 9))],
          ),
        ),
      ]),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Salidas'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Probá con «Todos»'), findsOneWidget);
  });

  testWidgets('si la lectura falla ofrece reintentar', (tester) async {
    await tester.pumpWidget(
      _app(lista, [
        movimientosBilleteraProvider(
          'c1',
        ).overrideWith((ref) async => throw Exception('caído')),
      ]),
    );
    await tester.pumpAndSettle();
    expect(find.text('Reintentar'), findsOneWidget);
  });

  testWidgets('el certificado lleva período y saldo, y nada más', (
    tester,
  ) async {
    final c = _Compartir(true);
    await tester.pumpWidget(
      _app(
        const BotonDeCertificado(
          periodo: '1 sep 2026 – 30 sep 2026',
          moneda: 'BOB',
          saldo: '1250.50',
        ),
        [compartirProvider.overrideWithValue(c)],
      ),
    );
    await tester.tap(find.text('Compartir certificado de saldo'));
    await tester.pumpAndSettle();
    expect(c.texto, contains('Período: 1 sep 2026 – 30 sep 2026'));
    expect(c.texto, contains('Saldo al cierre: BOB 1250.50'));
  });

  testWidgets('si ninguna app abre, lo avisa', (tester) async {
    await tester.pumpWidget(
      _app(
        const BotonDeCertificado(periodo: 'p', moneda: 'BOB', saldo: '1.00'),
        [compartirProvider.overrideWithValue(_Compartir(false))],
      ),
    );
    await tester.tap(find.text('Compartir certificado de saldo'));
    await tester.pumpAndSettle();
    expect(find.textContaining('No pudimos abrir otra app'), findsOneWidget);
  });
}
