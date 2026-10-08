import 'dart:io';
import 'dart:ui' as ui;

import 'package:aportaya_diseno/atomos/campo_o_t_p.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/notifier_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_celular.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

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

  const claveCaptura = ValueKey('captura-contacto-bloqueado');
  for (final brillo in [Brightness.light, Brightness.dark]) {
    testWidgets('contacto sin contrato OTP queda pausado · ${brillo.name}', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(360, 760);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      final (:dio, adaptador: _) = dioSimulado();
      final contenedor = ProviderContainer(
        retry: (count, error) => null,
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
          dioProvider.overrideWithValue(dio),
        ],
      );
      addTearDown(contenedor.dispose);
      final alta = contenedor.read(altaProvider.notifier);
      alta.siguiente();
      alta.siguiente();
      expect(contenedor.read(altaProvider).paso, PasoAlta.celular);

      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: contenedor,
          child: MaterialApp(
            theme: temaDesde(Tokens.claro, Brightness.light),
            darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
            themeMode: brillo == Brightness.dark
                ? ThemeMode.dark
                : ThemeMode.light,
            builder: (context, child) => MediaQuery(
              data: MediaQuery.of(
                context,
              ).copyWith(textScaler: const TextScaler.linear(2)),
              child: child!,
            ),
            home: const RepaintBoundary(
              key: claveCaptura,
              child: Scaffold(
                body: SafeArea(
                  child: SingleChildScrollView(child: PasoCelular()),
                ),
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      if (Platform.environment['CAPTURAR_VERIFICACION'] == '1') {
        final limite = tester.renderObject<RenderRepaintBoundary>(
          find.byKey(claveCaptura),
        );
        await tester.runAsync(() async {
          final imagen = await limite.toImage(pixelRatio: 1);
          final png = await imagen.toByteData(format: ui.ImageByteFormat.png);
          imagen.dispose();
          final directorio =
              Platform.environment['CAPTURA_VERIFICACION_DIR'] ??
              'build/visual';
          final destino = File(
            '$directorio/contacto_bloqueado_${brillo.name}.png',
          );
          await destino.parent.create(recursive: true);
          await destino.writeAsBytes(png!.buffer.asUint8List());
        });
      }

      expect(
        find.text(TextosIdentidad.verificacionNoDisponibleTitulo),
        findsOneWidget,
      );
      expect(find.textContaining('No enviamos ningún código'), findsOneWidget);
      expect(find.byType(CampoOTP), findsNothing);
      expect(find.text(TextosIdentidad.continuar), findsNothing);
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
      await expectLater(tester, meetsGuideline(textContrastGuideline));
      expect(tester.takeException(), isNull);

      alta.siguiente();
      expect(contenedor.read(altaProvider).paso, PasoAlta.celular);
      expect(contenedor.read(altaProvider).codigoConfirmado, isFalse);
      expect(await alta.enviarAlServidor(), isNull);
      expect(
        contenedor.read(altaProvider).error,
        contains('No enviamos el alta'),
      );

      await tester.ensureVisible(
        find.text(TextosIdentidad.volverAlPasoAnterior),
      );
      await tester.pumpAndSettle();
      await tester.tap(find.text(TextosIdentidad.volverAlPasoAnterior));
      await tester.pumpAndSettle();
      expect(contenedor.read(altaProvider).paso, PasoAlta.contrasena);
    });
  }
}
