import 'dart:io';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_cuenta_no_disponible.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
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

  for (final (brillo, nombre, tamano, escala) in [
    (Brightness.light, 'claro', const Size(360, 760), 1.0),
    (Brightness.dark, 'oscuro', const Size(360, 760), 1.0),
    (Brightness.light, 'tablet_claro', const Size(768, 1024), 1.0),
    (Brightness.dark, 'texto_200_oscuro', const Size(360, 760), 2.0),
  ]) {
    // Estas lineas base se capturaron en Windows. El rasterizador de macOS
    // requiere sus propias referencias antes de usar esta prueba como gate.
    testWidgets('billetera sin titularidad · $nombre', (tester) async {
      tester.view.physicalSize = tamano;
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      final router = crearEnrutador(
        inicial:
            '/billetera/inicio?cuenta=22222222-2222-4222-8222-222222222222',
      );
      addTearDown(router.dispose);
      final simulado = dioSimulado();
      await tester.pumpWidget(
        ProviderScope(
          retry: (retryCount, error) => null,
          overrides: [
            almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
            dioProvider.overrideWithValue(simulado.dio),
          ],
          child: MaterialApp.router(
            theme: temaDesde(
              brillo == Brightness.light ? Tokens.claro : Tokens.oscuro,
              brillo,
            ),
            builder: (context, child) => MediaQuery(
              data: MediaQuery.of(
                context,
              ).copyWith(textScaler: TextScaler.linear(escala)),
              child: child!,
            ),
            routerConfig: router,
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      await expectLater(
        find.byType(PantallaCuentaNoDisponible),
        matchesGoldenFile('imagenes/billetera_sin_titularidad_$nombre.png'),
      );
    }, skip: !Platform.isWindows);
  }
}
