import 'dart:io';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
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

  Future<void> montar(
    WidgetTester tester, {
    required Size tamano,
    required Brightness brillo,
    required double escala,
  }) async {
    tester.view.physicalSize = tamano;
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final router = crearEnrutador(inicial: '/tour');
    addTearDown(router.dispose);
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
        ],
        child: MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
          darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
          themeMode: brillo == Brightness.dark
              ? ThemeMode.dark
              : ThemeMode.light,
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
    await asentar(tester);
  }

  for (final brillo in [Brightness.light, Brightness.dark]) {
    testWidgets('tour 320/200 primer encuadre ${brillo.name}', (tester) async {
      await montar(
        tester,
        tamano: const Size(320, 760),
        brillo: brillo,
        escala: 2,
      );
      expect(tester.takeException(), isNull);
      await expectLater(
        find.byType(Scaffold),
        matchesGoldenFile('imagenes/tour_320_texto_200_${brillo.name}.png'),
      );
    }, skip: !Platform.isWindows);

    testWidgets('tour 320/200 texto leído ${brillo.name}', (tester) async {
      await montar(
        tester,
        tamano: const Size(320, 760),
        brillo: brillo,
        escala: 2,
      );
      await tester.drag(
        find.byType(SingleChildScrollView).first,
        const Offset(0, -600),
      );
      await asentar(tester);
      expect(tester.takeException(), isNull);
      await expectLater(
        find.byType(Scaffold),
        matchesGoldenFile(
          'imagenes/tour_320_texto_200_scroll_${brillo.name}.png',
        ),
      );
    }, skip: !Platform.isWindows);
  }

  testWidgets('tour 320/200 última lámina dark', (tester) async {
    await montar(
      tester,
      tamano: const Size(320, 760),
      brillo: Brightness.dark,
      escala: 2,
    );
    for (var i = 0; i < 3; i++) {
      await tester.tap(find.text(TextosIdentidad.tourSiguiente));
      await asentar(tester);
    }
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(Scaffold),
      matchesGoldenFile('imagenes/tour_320_texto_200_ultima_dark.png'),
    );
  }, skip: !Platform.isWindows);

  testWidgets('tour tablet 768 light', (tester) async {
    await montar(
      tester,
      tamano: const Size(768, 1024),
      brillo: Brightness.light,
      escala: 1,
    );
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(Scaffold),
      matchesGoldenFile('imagenes/tour_768_light.png'),
    );
  }, skip: !Platform.isWindows);
}
