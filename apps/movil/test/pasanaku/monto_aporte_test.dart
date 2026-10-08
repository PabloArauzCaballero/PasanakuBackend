import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/pasanaku/dominio/cu21_cobrar_aporte.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_aportar.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const _obligacion = '68a98585-2854-4b71-80fd-5ef490fe60f6';

void main() {
  test('monto ausente, cero o no finito nunca llega al cobro', () async {
    for (final monto in [
      '',
      '0.00',
      '000.00',
      '-1.00',
      'NaN',
      '125',
      '0.001',
    ]) {
      expect(montoDeAporteValido(monto), false);
    }
    expect(montoDeAporteValido('0.01'), true);
    expect(montoDeAporteValido('125.00'), true);

    final (:dio, adaptador: _) = dioSimulado();
    var peticiones = 0;
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          if (options.method == 'POST') peticiones++;
          handler.next(options);
        },
      ),
    );
    final contenedor = ProviderContainer(
      retry: (retryCount, error) => null,
      overrides: [
        dioProvider.overrideWithValue(dio),
        almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
      ],
    );
    addTearDown(contenedor.dispose);
    await contenedor.read(cobrarAporteProvider(_obligacion).future);

    await contenedor
        .read(cobrarAporteProvider(_obligacion).notifier)
        .enviar(
          monto: '0.00',
          canal: 'BILLETERA_MOVIL',
          referenciaProveedor: 'REF-SINTETICA',
        );

    expect(peticiones, 0);
    expect(
      contenedor.read(cobrarAporteProvider(_obligacion)).error,
      isA<ErrorDeMontoNoVerificable>(),
    );
  });

  for (final brillo in [Brightness.light, Brightness.dark]) {
    testWidgets('sin monto, bloquea el pago al 200 % en ${brillo.name}', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(360, 760);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);

      await tester.pumpWidget(
        conApp(
          const PantallaAportar(
            obligacionId: _obligacion,
            montoDelPeriodo: '0.00',
          ),
          dio: dioSimulado().dio,
          brillo: brillo,
          escalaDeTexto: 2,
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text(ErrorDeMontoNoVerificable.texto), findsOneWidget);
      expect(find.widgetWithText(FilledButton, 'Aportar'), findsNothing);
      expect(tester.takeException(), isNull);
    });
  }
}
