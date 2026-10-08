import 'dart:io';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/puertos/conectividad.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_recargar.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_retirar.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_transferir.dart';
import 'package:aportaya_movil/proveedores/conexion.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

class _SinConexion implements Conectividad {
  @override
  Stream<bool> get cambios => const Stream<bool>.empty();

  @override
  Future<bool> hayConexion() async => false;
}

Widget _app(Widget pantalla, Dio dio, Brightness brillo, double escala) =>
    ProviderScope(
      retry: (retryCount, error) => null,
      overrides: [
        almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
        dioProvider.overrideWithValue(dio),
        conectividadProvider.overrideWithValue(_SinConexion()),
      ],
      child: MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
        themeMode: brillo == Brightness.dark ? ThemeMode.dark : ThemeMode.light,
        builder: (context, child) => MediaQuery(
          data: MediaQuery.of(
            context,
          ).copyWith(textScaler: TextScaler.linear(escala)),
          child: child!,
        ),
        home: pantalla,
      ),
    );

void main() {
  const cuenta = '11111111-1111-4111-8111-111111111111';

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

  for (final (nombre, brillo, tamano, escala) in [
    ('360_claro', Brightness.light, const Size(360, 760), 1.0),
    ('360_oscuro', Brightness.dark, const Size(360, 760), 1.0),
    ('360_texto_200_claro', Brightness.light, const Size(360, 760), 2.0),
    ('360_texto_200_oscuro', Brightness.dark, const Size(360, 760), 2.0),
    ('768_claro', Brightness.light, const Size(768, 1024), 1.0),
  ]) {
    testWidgets('saldo offline · $nombre', (tester) async {
      tester.view.physicalSize = tamano;
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      final simulado = dioSimulado();
      simulado.adaptador.onGet(
        '/billetera/$cuenta/saldo',
        (s) => s.reply(
          200,
          ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
        ),
      );
      await tester.pumpWidget(
        _app(
          const PantallaDeSaldo(cuentaId: cuenta),
          simulado.dio,
          brillo,
          escala,
        ),
      );
      await asentar(tester);
      expect(tester.takeException(), isNull);
      await expectLater(
        find.byType(PantallaDeSaldo),
        matchesGoldenFile('imagenes/saldo_offline_$nombre.png'),
      );
    }, skip: !Platform.isWindows);
  }

  for (final (nombre, pantalla) in [
    ('recargar', const PantallaRecargar(cuentaId: cuenta)),
    ('retirar', const PantallaRetirar(cuentaId: cuenta)),
    ('transferir', const PantallaTransferir(cuentaOrigenId: cuenta)),
  ]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      testWidgets(
        '$nombre offline · 360 texto 200 · ${brillo.name}',
        (tester) async {
          tester.view.physicalSize = const Size(360, 760);
          tester.view.devicePixelRatio = 1;
          addTearDown(tester.view.reset);
          await tester.pumpWidget(_app(pantalla, dioSimulado().dio, brillo, 2));
          await asentar(tester);
          expect(tester.takeException(), isNull);
          await expectLater(
            find.byType(pantalla.runtimeType),
            matchesGoldenFile(
              'imagenes/${nombre}_offline_360_texto_200_${brillo.name}.png',
            ),
          );
        },
        skip: !Platform.isWindows,
      );
    }
  }
}
