import 'dart:io';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/localizacion.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// Ayudas de las capturas de evidencia (H17): montan una pantalla con el tema real, a un tamano y a una escala de texto,
/// y la fotografian con las fuentes reales. NO son goldens de comparacion (el CI de macOS es la autoridad de los goldens).
const tamanosDeCaptura = {
  'movil': Size(360, 740),
  'tablet': Size(768, 1024),
  'escritorio': Size(1280, 900),
};

Future<void> montarParaCaptura(
  WidgetTester t,
  Widget pantalla,
  Dio dio,
  Brightness brillo,
  Size tam, {
  bool conIdioma = false,
  double escalaDeTexto = 1.3,
}) async {
  t.view.physicalSize = tam;
  t.view.devicePixelRatio = 1;
  t.platformDispatcher.textScaleFactorTestValue = escalaDeTexto;
  addTearDown(t.view.reset);
  addTearDown(t.platformDispatcher.clearAllTestValues);
  await t.pumpWidget(
    ProviderScope(
      retry: (r, e) => null,
      overrides: [
        almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
        dioProvider.overrideWithValue(dio),
      ],
      child: MaterialApp(
        debugShowCheckedModeBanner: false,
        theme: temaDesde(Tokens.claro, Brightness.light),
        darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
        themeMode: brillo == Brightness.dark ? ThemeMode.dark : ThemeMode.light,
        supportedLocales: conIdioma ? idiomasDeLaApp : const [Locale('en')],
        localizationsDelegates: conIdioma ? delegadosDeIdioma : null,
        home: pantalla,
      ),
    ),
  );
}

/// Fotografia la app SIN exigir que no haya desborde: el "antes" de un defecto visual tiene que poder capturarse.
Future<void> fotoSinExigir(WidgetTester t, String ruta) =>
    expectLater(find.byType(MaterialApp), matchesGoldenFile(ruta));

Future<void> fotoDeCaptura(WidgetTester t, String ruta, String nombre) async {
  expect(t.takeException(), isNull, reason: '$nombre: sin desborde ni error');
  await fotoSinExigir(t, ruta);
}

/// En `flutter test` el texto sale con la fuente de prueba (bloques) salvo que se carguen las reales: las del paquete de diseno
/// y los iconos de Material del SDK. Sin esto la captura no se puede leer.
Future<void> cargarFuentesReales() async {
  final repo = raizDelRepositorio();
  Future<void> familia(String nombre, List<String> rutas) async {
    final cargador = FontLoader(nombre);
    for (final r in rutas) {
      cargador.addFont(
        File(r).readAsBytes().then((b) => ByteData.view(b.buffer)),
      );
    }
    await cargador.load();
  }

  const dir = 'packages/diseno_flutter/lib/fuentes';
  await familia('Bricolage', [
    for (final w in [600, 700, 800]) '$repo/$dir/Bricolage-$w.ttf',
  ]);
  await familia('Instrument', [
    for (final w in [400, 500, 600]) '$repo/$dir/Instrument-$w.ttf',
  ]);
  final sdk =
      Platform.environment['FLUTTER_ROOT'] ?? '/tmp/claude-0/flutter-3448';
  await familia('MaterialIcons', [
    '$sdk/bin/cache/artifacts/material_fonts/MaterialIcons-Regular.otf',
  ]);
  // Los íconos Lucide (ADR-053) vienen de un paquete: su familia lleva el prefijo.
  final cache =
      Platform.environment['PUB_CACHE'] ??
      '${Platform.environment['HOME']}/.pub-cache';
  await familia('packages/lucide_icons_flutter/Lucide', [
    '$cache/hosted/pub.dev/lucide_icons_flutter-3.1.20/assets/lucide.ttf',
  ]);
}
