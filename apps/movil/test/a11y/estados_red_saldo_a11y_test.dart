import 'dart:io';
import 'dart:ui' as ui;

import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const _cuenta = '11111111-1111-4111-8111-111111111111';
const _ruta = '/billetera/$_cuenta/saldo';
const _captura = ValueKey('captura-estados-red-saldo');

void main() {
  setUpAll(() async {
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
    await Future.wait([bricolage.load(), instrument.load()]);
  });

  for (final brillo in [Brightness.light, Brightness.dark]) {
    for (final tipo in [
      DioExceptionType.connectionError,
      DioExceptionType.receiveTimeout,
    ]) {
      testWidgets('saldo ${tipo.name} al 200 % · ${brillo.name}', (
        tester,
      ) async {
        tester.view.physicalSize = const Size(360, 760);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onGet(
          _ruta,
          (s) => s.throws(
            0,
            DioException(
              requestOptions: RequestOptions(path: _ruta),
              type: tipo,
            ),
          ),
        );
        await tester.pumpWidget(
          conApp(
            const RepaintBoundary(
              key: _captura,
              child: PantallaDeSaldo(cuentaId: _cuenta),
            ),
            dio: dio,
            brillo: brillo,
            escalaDeTexto: 2,
          ),
        );
        await asentar(tester);
        await tester.pumpAndSettle();

        final sinRed = tipo == DioExceptionType.connectionError;
        expect(
          find.textContaining(
            sinRed ? 'No pudimos conectarnos' : 'La respuesta tardó demasiado',
          ),
          findsOneWidget,
        );
        expect(
          find.byIcon(sinRed ? Icons.wifi_off_outlined : Icons.error_outline),
          findsOneWidget,
        );
        expect(find.text('Volver a intentar'), findsOneWidget);
        await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
        await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
        await expectLater(tester, meetsGuideline(textContrastGuideline));
        expect(tester.takeException(), isNull);

        if (Platform.environment['CAPTURAR_RED'] == '1') {
          final limite = tester.renderObject<RenderRepaintBoundary>(
            find.byKey(_captura),
          );
          await tester.runAsync(() async {
            final imagen = await limite.toImage(pixelRatio: 1);
            final png = await imagen.toByteData(format: ui.ImageByteFormat.png);
            imagen.dispose();
            final directorio =
                Platform.environment['CAPTURA_RED_DIR'] ?? 'build/visual';
            final destino = File(
              '$directorio/saldo_${tipo.name}_${brillo.name}.png',
            );
            await destino.parent.create(recursive: true);
            await destino.writeAsBytes(png!.buffer.asUint8List());
          });
        }
      });
    }
  }
}
