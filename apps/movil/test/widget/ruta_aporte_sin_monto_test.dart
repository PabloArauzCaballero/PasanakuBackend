import 'dart:io';
import 'dart:ui' as ui;

import 'package:aportaya_diseno/moviles/barra_pestanas.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_aportar.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_aporte_no_disponible.dart';
import 'package:aportaya_movil/pantallas/pasanaku/textos.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
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

  const ruta =
      '/pasanaku/obligaciones/22222222-2222-4222-8222-222222222222/aportar';

  for (final sufijo in ['', '?monto=125.00&referencia=secreta']) {
    testWidgets('la ruta $sufijo no inicia un pago sin monto verificado', (
      tester,
    ) async {
      final router = crearEnrutador(inicial: '$ruta$sufijo');
      addTearDown(router.dispose);
      final simulado = dioSimulado();
      var solicitudes = 0;
      simulado.dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            solicitudes++;
            handler.next(options);
          },
        ),
      );

      await tester.pumpWidget(
        ProviderScope(
          retry: (retryCount, error) => null,
          overrides: [
            almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
            dioProvider.overrideWithValue(simulado.dio),
          ],
          child: MaterialApp.router(
            theme: temaDesde(Tokens.claro, Brightness.light),
            routerConfig: router,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(router.state.uri.toString(), ruta);
      expect(find.byType(PantallaAporteNoDisponible), findsOneWidget);
      expect(find.byType(PantallaAportar), findsNothing);
      expect(
        find.text(TextosPasanaku.aporteSinMontoVerificado),
        findsOneWidget,
      );
      expect(solicitudes, 0);
      expect(tester.takeException(), isNull);
    });
  }

  testWidgets('la salida a ayuda funciona sin enviar un pago', (tester) async {
    final router = crearEnrutador(inicial: ruta);
    addTearDown(router.dispose);
    final simulado = dioSimulado();
    var solicitudes = 0;
    simulado.dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          solicitudes++;
          handler.next(options);
        },
      ),
    );

    await tester.pumpWidget(
      ProviderScope(
        retry: (retryCount, error) => null,
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
          dioProvider.overrideWithValue(simulado.dio),
        ],
        child: MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
          routerConfig: router,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text(TextosPasanaku.irAAyuda));
    await tester.pumpAndSettle();
    expect(router.state.uri.toString(), '/soporte/ayuda');
    expect(solicitudes, 0);
  });

  for (final brillo in [Brightness.light, Brightness.dark]) {
    testWidgets('el aviso cabe al 200 % en tema ${brillo.name}', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(360, 760);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      final router = crearEnrutador(inicial: ruta);
      addTearDown(router.dispose);
      const claveCaptura = ValueKey('aporte-no-disponible');

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
          ],
          child: RepaintBoundary(
            key: claveCaptura,
            child: MaterialApp.router(
              debugShowCheckedModeBanner: false,
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
              routerConfig: router,
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(
        find.text(TextosPasanaku.aporteSinMontoVerificado),
        findsOneWidget,
      );
      expect(find.text(TextosPasanaku.irAAyuda), findsOneWidget);
      final barra = tester.getRect(find.byType(BarraPestanas));
      final etiqueta = tester.getRect(find.text('Movimientos'));
      expect(etiqueta.top, greaterThanOrEqualTo(barra.top));
      expect(etiqueta.bottom, lessThanOrEqualTo(barra.bottom));
      expect(tester.takeException(), isNull);

      // Evidencia sintética opt-in para inspección, no golden de CI.
      if (Platform.environment['CAPTURAR_APORTE'] == '1') {
        final limite = tester.renderObject<RenderRepaintBoundary>(
          find.byKey(claveCaptura),
        );
        await tester.runAsync(() async {
          final imagen = await limite.toImage(pixelRatio: 1);
          final png = await imagen.toByteData(format: ui.ImageByteFormat.png);
          imagen.dispose();
          final directorio =
              Platform.environment['CAPTURA_APORTE_DIR'] ?? 'build/visual';
          final destino = File(
            '$directorio/aporte_bloqueado_${brillo.name}.png',
          );
          await destino.parent.create(recursive: true);
          await destino.writeAsBytes(png!.buffer.asUint8List());
        });
      }
    });
  }
}
