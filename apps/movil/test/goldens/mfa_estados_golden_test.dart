import 'dart:io';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/moviles/barra_pestanas.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_sesion.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_mfa.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

class _MfaPendiente extends SesionIdentidadNotifier {
  @override
  EstadoSesion build() => const EstadoSesion(paso: PasoSesion.mfa);
}

class _MfaEnviando extends SesionIdentidadNotifier {
  @override
  EstadoSesion build() =>
      const EstadoSesion(paso: PasoSesion.mfa, enviando: true);
}

class _MfaConError extends SesionIdentidadNotifier {
  @override
  EstadoSesion build() => const EstadoSesion(
    paso: PasoSesion.mfa,
    error: 'El código no coincide. Revisalo e intentá de nuevo.',
  );
}

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

  for (final pendiente in [false, true]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      testWidgets(
        'MFA ${pendiente ? 'pendiente' : 'sin desafío'} · 320/200 · ${brillo.name}',
        (tester) async {
          tester.view.physicalSize = const Size(320, 760);
          tester.view.devicePixelRatio = 1;
          addTearDown(tester.view.reset);
          await tester.pumpWidget(
            ProviderScope(
              overrides: pendiente
                  ? [sesionIdentidadProvider.overrideWith(_MfaPendiente.new)]
                  : [],
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
                home: const PantallaDeMfa(),
              ),
            ),
          );
          await tester.pumpAndSettle();
          expect(tester.takeException(), isNull);
          await expectLater(
            find.byType(Scaffold),
            matchesGoldenFile(
              'imagenes/mfa_${pendiente ? 'pendiente' : 'sin_desafio'}_320_texto_200_${brillo.name}.png',
            ),
          );
        },
        skip: !Platform.isWindows,
      );
    }
  }

  testWidgets('MFA sin desafío · 768 · light', (tester) async {
    tester.view.physicalSize = const Size(768, 1024);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const PantallaDeMfa(),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(Scaffold),
      matchesGoldenFile('imagenes/mfa_sin_desafio_768_light.png'),
    );
  }, skip: !Platform.isWindows);

  for (final (nombre, brillo, crear) in [
    ('enviando', Brightness.light, _MfaEnviando.new),
    ('error', Brightness.dark, _MfaConError.new),
  ]) {
    testWidgets('MFA $nombre · 320/200 · ${brillo.name}', (tester) async {
      tester.view.physicalSize = const Size(320, 760);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(
        ProviderScope(
          overrides: [sesionIdentidadProvider.overrideWith(crear)],
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
            home: const PantallaDeMfa(),
          ),
        ),
      );
      await tester.pump();
      expect(tester.takeException(), isNull);
      await expectLater(
        find.byType(Scaffold),
        matchesGoldenFile(
          'imagenes/mfa_${nombre}_320_texto_200_${brillo.name}.png',
        ),
      );
    }, skip: !Platform.isWindows);
  }

  testWidgets('ruta real MFA sin desafío · 320/200 · light', (tester) async {
    tester.view.physicalSize = const Size(320, 760);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final router = crearEnrutador(inicial: '/identidad/mfa');
    addTearDown(router.dispose);
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
          builder: (context, child) => MediaQuery(
            data: MediaQuery.of(
              context,
            ).copyWith(textScaler: const TextScaler.linear(2)),
            child: child!,
          ),
          routerConfig: router,
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.byType(BarraPestanas), findsNothing);
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(Scaffold),
      matchesGoldenFile('imagenes/mfa_ruta_real_320_texto_200_light.png'),
    );
  }, skip: !Platform.isWindows);
}
