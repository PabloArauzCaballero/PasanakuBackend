import 'dart:io';

import 'package:aportaya_movil/pantallas/identidad/pantalla_baja.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_contrasena.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_dispositivos.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_perfil.dart';
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

  final pantallas = <({String nombre, Widget pantalla})>[
    (nombre: 'dispositivos', pantalla: const PantallaDeDispositivos()),
    (nombre: 'contrasena', pantalla: const PantallaDeContrasena()),
    (nombre: 'baja', pantalla: const PantallaDeBaja()),
    (nombre: 'perfil', pantalla: const PantallaDePerfil()),
  ];
  for (final pantalla in pantallas) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      testWidgets('${pantalla.nombre} · 320/200 · ${brillo.name}', (
        tester,
      ) async {
        tester.view.physicalSize = const Size(320, 760);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        await tester.pumpWidget(
          conApp(
            pantalla.pantalla,
            dio: dioSimulado().dio,
            brillo: brillo,
            escalaDeTexto: 2,
          ),
        );
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);
        await expectLater(
          find.byType(Scaffold).first,
          matchesGoldenFile(
            'imagenes/${pantalla.nombre}_sin_gestion_320_texto_200_${brillo.name}.png',
          ),
        );
      }, skip: !Platform.isWindows);
    }
  }

  for (final (nombre, pantalla, brillo) in [
    ('perfil', const PantallaDePerfil(), Brightness.light),
    ('baja', const PantallaDeBaja(), Brightness.dark),
  ]) {
    testWidgets('$nombre · 768 · ${brillo.name}', (tester) async {
      tester.view.physicalSize = const Size(768, 1024);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(
        conApp(pantalla, dio: dioSimulado().dio, brillo: brillo),
      );
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      await expectLater(
        find.byType(Scaffold).first,
        matchesGoldenFile(
          'imagenes/${nombre}_sin_gestion_768_${brillo.name}.png',
        ),
      );
    }, skip: !Platform.isWindows);
  }
}
