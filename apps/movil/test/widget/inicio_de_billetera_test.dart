import 'dart:io';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/cu13_cuenta_propia.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';
import 'package:aportaya_movil/pantallas/billetera/resolver_cuenta.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// D4 (carril H3): el inicio de la billetera ya no consulta una cuenta de id fijo; le pregunta al
/// servidor cuál es la de la persona (`GET /billetera/cuentas/propia`). Los estados, en tema claro
/// y oscuro, a 360 px de ancho con el texto al 130 %.
const _fijaDeAntes = '11111111-1111-4111-8111-111111111111';
const _propia = '334cc0fa-e284-49fa-82e3-7570015d12af';
const _rutaPropia = '/billetera/cuentas/propia';
const _rutaSaldo = '/billetera/$_propia/saldo';

Map<String, dynamic> _saldoDeLaPropia() => {
  ...(ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo']
      as Map<String, dynamic>),
  'cuentaId': _propia,
};

Map<String, dynamic> _saldoEnCero() => {
  'cuentaId': _propia,
  'disponible': {'monto': '0.00', 'moneda': 'BOB'},
  'retenido': {'monto': '0.00', 'moneda': 'BOB'},
  'alCorteDe': '2026-01-15T10:30:00Z',
};

Map<String, dynamic> _sinBilletera() => {
  'codigo': 'AP-CU13-01',
  'mensaje': 'Esa cuenta no existe',
  'trazaId': 'traza-sin-billetera',
};

Object? _cuerpoPropia() =>
    ejemplo('nucleo-financiero', 'consultarCuentaPropia', 'ok')['cuerpo'];

void _propiaOk(DioAdapter a) => a.onGet(
  _rutaPropia,
  (s) => s.reply(200, _cuerpoPropia()),
  queryParameters: {'moneda': 'BOB'},
);

void main() {
  test('el ejemplo del contrato encaja en CuentaPropia', () {
    final c = CuentaPropia.fromJson(_cuerpoPropia()! as Map<String, dynamic>);
    expect(c.cuentaId, _propia);
    expect(c.moneda, 'BOB');
  });

  test('el id fijo de antes ya no existe en las rutas de la billetera', () {
    final rutas = File(
      '${raizDelRepositorio()}/apps/movil/lib/pantallas/billetera/rutas.dart',
    ).readAsStringSync();
    expect(
      rutas.contains(_fijaDeAntes),
      isFalse,
      reason: 'la cuenta se resuelve contra el servidor, no con un id fijo',
    );
    expect(rutas.contains('_cuentaPorOmision'), isFalse);
  });

  for (final (brillo, nombreTema) in [
    (Brightness.light, 'claro'),
    (Brightness.dark, 'oscuro'),
  ]) {
    group('360 px · texto 130 % · tema $nombreTema', () {
      Future<void> montar(WidgetTester tester, Widget pantalla, Dio dio) async {
        tester.view.physicalSize = const Size(360, 740);
        tester.view.devicePixelRatio = 1;
        tester.platformDispatcher.textScaleFactorTestValue = 1.3;
        addTearDown(tester.view.reset);
        addTearDown(tester.platformDispatcher.clearAllTestValues);
        await tester.pumpWidget(
          ProviderScope(
            retry: (r, e) => null,
            overrides: [
              almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
              dioProvider.overrideWithValue(dio),
            ],
            child: MaterialApp(
              theme: temaDesde(Tokens.claro, Brightness.light),
              darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
              themeMode: brillo == Brightness.dark
                  ? ThemeMode.dark
                  : ThemeMode.light,
              home: pantalla,
            ),
          ),
        );
      }

      void sinDesborde(WidgetTester tester) {
        final contexto = tester.element(find.byType(Scaffold).first);
        expect(Theme.of(contexto).brightness, brillo);
        expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
        expect(tester.takeException(), isNull, reason: 'sin desborde ni error');
      }

      testWidgets('cargando: dice qué espera, no queda en blanco', (
        tester,
      ) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onGet(
          _rutaPropia,
          (s) => s.reply(
            200,
            _cuerpoPropia(),
            delay: const Duration(milliseconds: 80),
          ),
          queryParameters: {'moneda': 'BOB'},
        );
        adaptador.onGet(_rutaSaldo, (s) => s.reply(200, _saldoDeLaPropia()));
        await montar(tester, const PantallaDeSaldo(), dio);
        await tester.pump();
        expect(find.bySemanticsLabel('Buscando tu billetera'), findsOneWidget);
        sinDesborde(tester);
        await asentar(tester);
      });

      testWidgets(
        'con datos: pregunta la cuenta propia y muestra SU saldo, sin id fijo ni banco inventado',
        (tester) async {
          final (:dio, :adaptador) = dioSimulado();
          _propiaOk(adaptador);
          // Si la app consultara el id fijo de antes, el adaptador no tiene esa ruta y falla.
          adaptador.onGet(_rutaSaldo, (s) => s.reply(200, _saldoDeLaPropia()));
          await montar(tester, const PantallaDeSaldo(), dio);
          await asentar(tester);
          expect(
            find.bySemanticsLabel(RegExp(r'^Saldo disponible: ')),
            findsOneWidget,
          );
          expect(find.text('Recargar'), findsOneWidget);
          expect(find.textContaining('Banco Unión'), findsNothing);
          expect(
            find.textContaining('En custodia, separado del dinero de la empresa'),
            findsOneWidget,
          );
          sinDesborde(tester);
        },
      );

      testWidgets('vacío: cuenta propia en cero dice por qué y qué esperar', (
        tester,
      ) async {
        final (:dio, :adaptador) = dioSimulado();
        _propiaOk(adaptador);
        adaptador.onGet(_rutaSaldo, (s) => s.reply(200, _saldoEnCero()));
        await montar(tester, const PantallaDeSaldo(), dio);
        await asentar(tester);
        expect(
          find.textContaining('Todavía no tenés movimientos'),
          findsOneWidget,
        );
        sinDesborde(tester);
      });

      testWidgets(
        'AP-CU13-01: «tu billetera todavía no está activa», no una falla genérica; revisar de nuevo la encuentra',
        (tester) async {
          final (:dio, :adaptador) = dioSimulado();
          adaptador.onGet(
            _rutaPropia,
            (s) => s.reply(422, _sinBilletera()),
            queryParameters: {'moneda': 'BOB'},
          );
          await montar(tester, const PantallaDeSaldo(), dio);
          await asentar(tester);
          expect(
            find.textContaining('Tu billetera todavía no está activa'),
            findsOneWidget,
          );
          expect(find.textContaining('Algo salió mal'), findsNothing);
          expect(find.text('Volver a intentar'), findsNothing);
          expect(find.text('Volver a revisar'), findsOneWidget);
          sinDesborde(tester);

          // Se activó mientras tanto: «Volver a revisar» vuelve a preguntar al servidor.
          _propiaOk(adaptador);
          adaptador.onGet(_rutaSaldo, (s) => s.reply(200, _saldoDeLaPropia()));
          await tester.tap(find.text('Volver a revisar'));
          await asentar(tester);
          expect(
            find.bySemanticsLabel(RegExp(r'^Saldo disponible: ')),
            findsOneWidget,
          );
          expect(find.text('Volver a revisar'), findsNothing);
        },
      );

      testWidgets('error: otro rechazo sí es falla, con reintento y traza', (
        tester,
      ) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onGet(
          _rutaPropia,
          (s) => s.reply(503, {
            'codigo': 'AP-GW-503',
            'mensaje': '',
            'trazaId': 'traza-503',
          }),
          queryParameters: {'moneda': 'BOB'},
        );
        await montar(tester, const PantallaDeSaldo(), dio);
        await asentar(tester);
        expect(find.text('Volver a intentar'), findsOneWidget);
        expect(
          find.textContaining('Código de seguimiento: traza-503'),
          findsOneWidget,
        );
        expect(find.text('Volver a revisar'), findsNothing);
        sinDesborde(tester);
      });

      testWidgets('con ?cuenta= la pantalla no pregunta por la cuenta propia', (
        tester,
      ) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onGet(_rutaSaldo, (s) => s.reply(200, _saldoDeLaPropia()));
        await montar(tester, const PantallaDeSaldo(cuentaId: _propia), dio);
        await asentar(tester);
        expect(
          find.bySemanticsLabel(RegExp(r'^Saldo disponible: ')),
          findsOneWidget,
        );
        sinDesborde(tester);
      });

      testWidgets(
        'pantallas interiores (recargar…): sin billetera activa lo dicen, con su título',
        (tester) async {
          final (:dio, :adaptador) = dioSimulado();
          adaptador.onGet(
            _rutaPropia,
            (s) => s.reply(422, _sinBilletera()),
            queryParameters: {'moneda': 'BOB'},
          );
          await montar(
            tester,
            PantallaConCuenta(
              titulo: 'Recargar',
              cuentaId: null,
              construir: (id) => Text('pantalla de $id'),
            ),
            dio,
          );
          await asentar(tester);
          expect(find.text('Recargar'), findsOneWidget);
          expect(
            find.textContaining('Tu billetera todavía no está activa'),
            findsOneWidget,
          );
          expect(find.textContaining('pantalla de'), findsNothing);
          sinDesborde(tester);
        },
      );
    });
  }
}
