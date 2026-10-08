import 'dart:async';
import 'dart:io';

import 'package:aportaya_movil/dominio/puertos/conectividad.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_recargar.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_aportar.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

class _SondaPendiente implements Conectividad {
  final primera = Completer<bool>();

  @override
  Future<bool> hayConexion() => primera.future;

  @override
  Stream<bool> get cambios => const Stream<bool>.empty();
}

class _SondaFallida implements Conectividad {
  @override
  Future<bool> hayConexion() async => throw StateError('sonda');

  @override
  Stream<bool> get cambios => const Stream<bool>.empty();
}

void main() {
  const cuenta = '11111111-1111-4111-8111-111111111111';
  setUpAll(() async {
    final iconos = FontLoader('MaterialIcons')
      ..addFont(rootBundle.load('fonts/MaterialIcons-Regular.otf'));
    final bricolage = FontLoader('Bricolage')
      ..addFont(
        rootBundle.load('packages/aportaya_diseno/fuentes/Bricolage-600.ttf'),
      )
      ..addFont(
        rootBundle.load('packages/aportaya_diseno/fuentes/Bricolage-700.ttf'),
      );
    final instrument = FontLoader('Instrument')
      ..addFont(
        rootBundle.load('packages/aportaya_diseno/fuentes/Instrument-400.ttf'),
      )
      ..addFont(
        rootBundle.load('packages/aportaya_diseno/fuentes/Instrument-600.ttf'),
      );
    await Future.wait([iconos.load(), bricolage.load(), instrument.load()]);
  });

  for (final brillo in [Brightness.light, Brightness.dark]) {
    for (final estado in ['pendiente', 'fallida']) {
      testWidgets(
        'recarga con sonda $estado · 360/200 · ${brillo.name}',
        (tester) async {
          tester.view.physicalSize = const Size(360, 760);
          tester.view.devicePixelRatio = 1;
          addTearDown(tester.view.reset);
          final pendiente = _SondaPendiente();
          await tester.pumpWidget(
            conApp(
              const PantallaRecargar(cuentaId: cuenta),
              dio: dioSimulado().dio,
              conectividad: estado == 'pendiente' ? pendiente : _SondaFallida(),
              brillo: brillo,
              escalaDeTexto: 2,
            ),
          );
          await asentar(tester);
          expect(tester.takeException(), isNull);
          await expectLater(
            find.byType(PantallaRecargar),
            matchesGoldenFile(
              'imagenes/recarga_sonda_${estado}_360_texto_200_${brillo.name}.png',
            ),
          );
          if (estado == 'pendiente') pendiente.primera.complete(false);
        },
        skip: !Platform.isWindows,
      );
    }

    testWidgets(
      'revisión de aporte espera sonda · 360/200 · ${brillo.name}',
      (tester) async {
        tester.view.physicalSize = const Size(360, 760);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        final pendiente = _SondaPendiente();
        await tester.pumpWidget(
          conApp(
            const PantallaAportar(
              obligacionId: '68a98585-2854-4b71-80fd-5ef490fe60f6',
              montoDelPeriodo: '125.00',
            ),
            dio: dioSimulado().dio,
            conectividad: pendiente,
            brillo: brillo,
            escalaDeTexto: 2,
          ),
        );
        await asentar(tester);
        await tester.enterText(find.byType(TextField), 'REF-SONDA');
        final revisar = find.widgetWithText(FilledButton, 'Revisar aporte');
        await tester.ensureVisible(revisar);
        await tester.pumpAndSettle();
        await tester.tap(revisar);
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);
        await expectLater(
          find.byType(PantallaAportar),
          matchesGoldenFile(
            'imagenes/aporte_sonda_pendiente_360_texto_200_${brillo.name}.png',
          ),
        );
        pendiente.primera.complete(false);
      },
      skip: !Platform.isWindows,
    );
  }

  testWidgets('saldo con sonda pendiente · 768 claro', (tester) async {
    tester.view.physicalSize = const Size(768, 1024);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final pendiente = _SondaPendiente();
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      '/billetera/$cuenta/saldo',
      (servidor) => servidor.reply(
        200,
        ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
      ),
    );
    await tester.pumpWidget(
      conApp(
        const PantallaDeSaldo(cuentaId: cuenta),
        dio: dio,
        conectividad: pendiente,
      ),
    );
    await asentar(tester);
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(PantallaDeSaldo),
      matchesGoldenFile('imagenes/saldo_sonda_pendiente_768_light.png'),
    );
    pendiente.primera.complete(false);
  }, skip: !Platform.isWindows);
}
