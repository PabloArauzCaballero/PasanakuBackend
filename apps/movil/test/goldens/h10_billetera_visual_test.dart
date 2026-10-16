import 'dart:async';

import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart'
    as nucleo;
import 'package:aportaya_movil/dominio/cu13_consultar_saldo.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';
import 'cargar_fuentes_de_aplicacion.dart';

const _cuenta = '11111111-1111-4111-8111-111111111111';
const _rutaSaldo = '/billetera/$_cuenta/saldo';
const _rutaMovimientos = '/billetera/$_cuenta/movimientos';
const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h10-visual/';

enum _Fixture { principal, cargando, vacio, error }

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  for (final ancho in [360, 412, 600]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      final tema = brillo == Brightness.light ? 'claro' : 'oscuro';
      for (final fixture in _Fixture.values) {
        final estado = switch (fixture) {
          _Fixture.principal => 'principal',
          _Fixture.cargando => 'cargando',
          _Fixture.vacio => 'vacio',
          _Fixture.error => 'error',
        };

        testWidgets('billetera · $estado · $tema · $ancho dp · texto 130 %', (
          tester,
        ) async {
          final alto = switch (ancho) {
            360 => 740,
            412 => 850,
            _ => 960,
          };
          tester.view.physicalSize = Size(ancho.toDouble(), alto.toDouble());
          tester.view.devicePixelRatio = 1;
          tester.platformDispatcher.textScaleFactorTestValue = 1.3;
          tester.platformDispatcher.accessibilityFeaturesTestValue =
              const FakeAccessibilityFeatures(disableAnimations: true);
          addTearDown(tester.view.reset);
          addTearDown(tester.platformDispatcher.clearAllTestValues);
          addTearDown(
            tester.platformDispatcher.clearAccessibilityFeaturesTestValue,
          );

          final (:dio, :adaptador) = dioSimulado();
          if (fixture == _Fixture.principal) {
            _responderSaldo(adaptador, _saldo());
            _responderMovimientos(adaptador, _movimientos());
          } else if (fixture == _Fixture.vacio) {
            _responderSaldo(adaptador, _saldo(cero: true));
            _responderMovimientos(adaptador, const <Object>[]);
          } else if (fixture == _Fixture.error) {
            adaptador.onGet(
              _rutaSaldo,
              (s) => s.reply(503, {
                'codigo': 'AP-GW-503',
                'mensaje': 'detalle interno no visible',
                'trazaId': 'h10-traza-503',
              }),
            );
          }

          final app = conApp(
            const PantallaDeSaldo(cuentaId: _cuenta),
            dio: dio,
            brillo: brillo,
            movimientoReducido: true,
          );
          await tester.pumpWidget(
            fixture == _Fixture.cargando
                ? ProviderScope(
                    overrides: [
                      saldoProvider(_cuenta).overrideWith(
                        (ref) => Completer<nucleo.SaldoBilletera>().future,
                      ),
                    ],
                    child: app,
                  )
                : app,
          );

          if (fixture == _Fixture.cargando) {
            await tester.pump();
          } else {
            await asentar(tester);
          }

          final pantalla = find.byType(PantallaDeSaldo);
          final contexto = tester.element(pantalla);
          expect(Theme.of(contexto).brightness, brillo);
          expect(
            MediaQuery.textScalerOf(contexto).scale(10),
            closeTo(13, 0.001),
          );
          switch (fixture) {
            case _Fixture.principal:
              expect(find.text('Tu billetera'), findsOneWidget);
              for (final etiqueta in ['Aportar', 'Recargar', 'Retirar']) {
                final texto = find.text(etiqueta);
                expect(texto, findsOneWidget);
                expect(
                  tester.renderObject<RenderParagraph>(texto).didExceedMaxLines,
                  isFalse,
                  reason: 'la etiqueta "$etiqueta" debe verse completa',
                );
              }
              expect(find.text('Recarga'), findsOneWidget);
            case _Fixture.cargando:
              expect(
                find.bySemanticsLabel('Cargando tu saldo'),
                findsOneWidget,
              );
            case _Fixture.vacio:
              expect(
                find.textContaining('Todavía no tenés movimientos'),
                findsOneWidget,
              );
            case _Fixture.error:
              expect(find.text('Volver a intentar'), findsOneWidget);
              expect(find.text('detalle interno no visible'), findsNothing);
          }
          expect(tester.takeException(), isNull);

          await expectLater(
            pantalla,
            matchesGoldenFile(
              '$_evidencia'
              'h10-billetera-$estado-$tema-$ancho-130.png',
            ),
          );
        });
      }
    }
  }
}

void _responderSaldo(DioAdapter adaptador, Map<String, dynamic> cuerpo) =>
    adaptador.onGet(_rutaSaldo, (s) => s.reply(200, cuerpo));

void _responderMovimientos(DioAdapter adaptador, List<Object> movimientos) =>
    adaptador.onGet(
      _rutaMovimientos,
      (s) => s.reply(200, {'cuentaId': _cuenta, 'movimientos': movimientos}),
      queryParameters: const {'limite': 100},
    );

Map<String, dynamic> _saldo({bool cero = false}) => {
  'cuentaId': _cuenta,
  'disponible': {'monto': cero ? '0.00' : '2450.75', 'moneda': 'BOB'},
  'retenido': {'monto': '0.00', 'moneda': 'BOB'},
  'alCorteDe': '2026-10-13T10:30:00Z',
};

List<Object> _movimientos() => [
  for (var i = 0; i < 4; i++)
    {
      'id': '00000000-0000-4000-8000-${i.toString().padLeft(12, '0')}',
      'sentido': i.isEven ? 'CREDITO' : 'DEBITO',
      'monto': {'monto': '${(i + 1) * 125}.00', 'moneda': 'BOB'},
      'saldoDisponiblePosterior': {'monto': '2450.75', 'moneda': 'BOB'},
      'saldoRetenidoPosterior': {'monto': '0.00', 'moneda': 'BOB'},
      'glosa': ['Aporte', 'Recarga', 'Pago rueda', 'Retiro'][i],
      'registradoEn': DateTime.utc(
        2026,
        10,
        13,
        10,
      ).subtract(Duration(minutes: i)).toIso8601String(),
    },
];
