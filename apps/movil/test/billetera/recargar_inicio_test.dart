import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cu13_cuenta_propia.dart';
import 'package:aportaya_movil/dominio/cu15_emitir_extracto.dart';
import 'package:aportaya_movil/dominio/errores.dart';
import 'package:aportaya_movil/pantallas/billetera/resolver_cuenta.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// 2026-10-19: Inicio y Movimientos se recargan deslizando hacia abajo, también
/// mientras la billetera no está activa (el estado en que la persona más necesita
/// saber si ya cambió algo). La cuenta propia la contesta un doble.
void main() {
  Future<List<String>> montar(
    WidgetTester tester,
    Future<CuentaPropia?> Function() respuesta,
  ) async {
    final pedidos = <String>[];
    await tester.pumpWidget(
      ProviderScope(
        retry: (_, _) => null,
        overrides: [
          cuentaPropiaProvider.overrideWith((ref) {
            pedidos.add('cuenta');
            return respuesta();
          }),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: Scaffold(
            body: ResolverCuenta(construir: (_) => const Text('Billetera')),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    return pedidos;
  }

  Future<void> deslizarHaciaAbajo(WidgetTester tester) async {
    await tester.fling(
      find.byType(SingleChildScrollView),
      const Offset(0, 400),
      1000,
    );
    await tester.pumpAndSettle();
  }

  testWidgets('correcto: billetera inactiva → deslizar vuelve a preguntar', (
    tester,
  ) async {
    final pedidos = await montar(tester, () async => null);
    expect(pedidos, ['cuenta']);
    await deslizarHaciaAbajo(tester);
    expect(pedidos, ['cuenta', 'cuenta']);
  });

  testWidgets('inválido: con error del servidor también se puede reintentar '
      'deslizando', (tester) async {
    final pedidos = await montar(
      tester,
      () async => throw ErrorDeApi(codigo: 'AP-X', trazaId: 't', estado: 500),
    );
    await deslizarHaciaAbajo(tester);
    expect(pedidos.length, 2);
  });

  testWidgets('límite: con la cuenta resuelta no se agrega un segundo gesto', (
    tester,
  ) async {
    await montar(
      tester,
      () async => const CuentaPropia(
        cuentaId: 'cuenta-sintetica',
        moneda: 'BOB',
        estado: 'ACTIVA',
      ),
    );
    expect(find.text('Billetera'), findsOneWidget);
    expect(find.byType(RefreshIndicator), findsNothing);
  });

  test('el extracto del mismo período es la misma clave', () {
    final desde = DateTime.utc(2026, 10, 1);
    final hasta = DateTime.utc(2026, 10, 19);
    expect(
      ParametrosExtracto(cuentaId: 'c', desde: desde, hasta: hasta),
      ParametrosExtracto(cuentaId: 'c', desde: desde, hasta: hasta),
    );
    expect(
      ParametrosExtracto(cuentaId: 'c', desde: desde, hasta: hasta),
      isNot(ParametrosExtracto(cuentaId: 'd', desde: desde, hasta: hasta)),
    );
  });
}
