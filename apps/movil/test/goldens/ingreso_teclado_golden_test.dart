import 'dart:io';

import 'package:aportaya_movil/pantallas/identidad/pantalla_sesion.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
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
    testWidgets('ingreso 320/200 con teclado ${brillo.name}', (tester) async {
      tester.view.physicalSize = const Size(320, 760);
      tester.view.devicePixelRatio = 1;
      tester.view.viewInsets = const FakeViewPadding(bottom: 300);
      addTearDown(tester.view.reset);

      await tester.pumpWidget(
        conApp(
          const PantallaDeSesion(),
          dio: dioSimulado().dio,
          brillo: brillo,
          escalaDeTexto: 2,
        ),
      );
      await tester.ensureVisible(find.byType(TextField).at(0));
      await tester.pumpAndSettle();
      await tester.tap(find.byType(TextField).at(0));
      await tester.testTextInput.receiveAction(TextInputAction.next);
      await tester.pumpAndSettle();

      expect(tester.takeException(), isNull);
      await expectLater(
        find.byType(Scaffold),
        matchesGoldenFile(
          'imagenes/ingreso_320_texto_200_teclado_${brillo.name}.png',
        ),
      );
    }, skip: !Platform.isWindows);
  }

  testWidgets(
    'ingreso 320/200 recuperación visible con teclado oscuro',
    (tester) async {
      tester.view.physicalSize = const Size(320, 760);
      tester.view.devicePixelRatio = 1;
      tester.view.viewInsets = const FakeViewPadding(bottom: 300);
      addTearDown(tester.view.reset);
      await tester.pumpWidget(
        conApp(
          const PantallaDeSesion(),
          dio: dioSimulado().dio,
          brillo: Brightness.dark,
          escalaDeTexto: 2,
        ),
      );
      await tester.ensureVisible(find.text(TextosIdentidad.sesionOlvide));
      await tester.pumpAndSettle();

      expect(tester.takeException(), isNull);
      await expectLater(
        find.byType(Scaffold),
        matchesGoldenFile(
          'imagenes/ingreso_320_texto_200_recuperacion_dark.png',
        ),
      );
    },
    skip: !Platform.isWindows,
  );

  testWidgets('ingreso tablet 768 claro', (tester) async {
    tester.view.physicalSize = const Size(768, 1024);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      conApp(const PantallaDeSesion(), dio: dioSimulado().dio),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(Scaffold),
      matchesGoldenFile('imagenes/ingreso_768_light.png'),
    );
  }, skip: !Platform.isWindows);
}
