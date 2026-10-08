import 'dart:io';

import 'package:aportaya_movil/pantallas/pasanaku/pantalla_accion_personal_no_disponible.dart';
import 'package:aportaya_movil/pantallas/pasanaku/textos.dart';
import 'package:flutter/material.dart';
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

  for (final brillo in [Brightness.light, Brightness.dark]) {
    testWidgets('acción personal bloqueada · 320/200 · ${brillo.name}', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(320, 760);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(
        conApp(
          const PantallaAccionPersonalNoDisponible(
            titulo: TextosPasanaku.tituloRetiro,
          ),
          dio: dioSimulado().dio,
          brillo: brillo,
          escalaDeTexto: 2,
        ),
      );
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      await expectLater(
        find.byType(Scaffold),
        matchesGoldenFile(
          'imagenes/accion_personal_bloqueada_320_texto_200_${brillo.name}.png',
        ),
      );
    }, skip: !Platform.isWindows);
  }

  testWidgets('acción personal bloqueada · 768 · light', (tester) async {
    tester.view.physicalSize = const Size(768, 1024);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      conApp(
        const PantallaAccionPersonalNoDisponible(
          titulo: TextosPasanaku.tituloRetiro,
        ),
        dio: dioSimulado().dio,
      ),
    );
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(Scaffold),
      matchesGoldenFile('imagenes/accion_personal_bloqueada_768_light.png'),
    );
  }, skip: !Platform.isWindows);
}
