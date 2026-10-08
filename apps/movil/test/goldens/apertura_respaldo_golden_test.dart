import 'dart:io';

import 'package:aportaya_diseno/moviles/apertura_de_marca.dart';
import 'package:aportaya_movil/app.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
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

  for (final brillo in [Brightness.light, Brightness.dark]) {
    testWidgets(
      'portada reducida en primer cuadro · ${brillo.name}',
      (tester) async {
        tester.view.physicalSize = const Size(360, 760);
        tester.view.devicePixelRatio = 1;
        tester.platformDispatcher.platformBrightnessTestValue = brillo;
        tester.platformDispatcher.accessibilityFeaturesTestValue =
            const FakeAccessibilityFeatures(disableAnimations: true);
        addTearDown(tester.view.reset);
        addTearDown(tester.platformDispatcher.clearPlatformBrightnessTestValue);
        addTearDown(
          tester.platformDispatcher.clearAccessibilityFeaturesTestValue,
        );
        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
              dioProvider.overrideWithValue(dioSimulado().dio),
            ],
            child: AppAportaYa(),
          ),
        );
        expect(find.byType(AperturaDeMarca), findsNothing);
        expect(tester.takeException(), isNull);
        await expectLater(
          find.byType(MaterialApp),
          matchesGoldenFile(
            'imagenes/apertura_reducida_360_${brillo.name}.png',
          ),
        );
        await tester.pumpAndSettle();
      },
      skip: !Platform.isWindows,
    );

    testWidgets(
      'portada tras respaldo de apertura · ${brillo.name}',
      (tester) async {
        tester.view.physicalSize = const Size(360, 760);
        tester.view.devicePixelRatio = 1;
        tester.platformDispatcher.platformBrightnessTestValue = brillo;
        addTearDown(tester.view.reset);
        addTearDown(tester.platformDispatcher.clearPlatformBrightnessTestValue);
        final binding = tester.binding as AutomatedTestWidgetsFlutterBinding;
        final app = AppAportaYa();
        final dio = dioSimulado().dio;

        Widget montar({required bool ticker}) => ProviderScope(
          overrides: [
            almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
            dioProvider.overrideWithValue(dio),
          ],
          child: TickerMode(enabled: ticker, child: app),
        );

        await tester.pumpWidget(montar(ticker: false));
        expect(find.byType(AperturaDeMarca), findsOneWidget);
        await binding.delayed(const Duration(seconds: 2));
        await tester.pump();
        expect(find.byType(AperturaDeMarca), findsNothing);

        await tester.pumpWidget(montar(ticker: true));
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);
        await expectLater(
          find.byType(MaterialApp),
          matchesGoldenFile(
            'imagenes/apertura_respaldo_360_${brillo.name}.png',
          ),
        );
        await tester.tap(find.text(TextosIdentidad.portadaCrearCuenta));
        await tester.pumpAndSettle();
        expect(find.byType(PageView), findsOneWidget);
      },
      skip: !Platform.isWindows,
    );
  }
}
