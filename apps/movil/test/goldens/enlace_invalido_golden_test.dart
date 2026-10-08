import 'dart:io';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

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

  for (final (ancho, alto, brillo, escala) in [
    (320.0, 760.0, Brightness.light, 2.0),
    (320.0, 760.0, Brightness.dark, 2.0),
    (768.0, 1024.0, Brightness.light, 1.0),
  ]) {
    testWidgets(
      'enlace inválido $ancho×$alto · ${brillo.name} · texto $escala',
      (tester) async {
        tester.view.physicalSize = Size(ancho, alto);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        final router = crearEnrutador(
          inicial: 'aportaya://unirse/ABC%3Fdocumento%3DCI_FICTICIA',
        );
        addTearDown(router.dispose);
        await tester.pumpWidget(
          ProviderScope(
            child: MaterialApp.router(
              debugShowCheckedModeBanner: false,
              theme: temaDesde(Tokens.claro, Brightness.light),
              darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
              themeMode: brillo == Brightness.dark
                  ? ThemeMode.dark
                  : ThemeMode.light,
              routerConfig: router,
              builder: (context, child) => MediaQuery(
                data: MediaQuery.of(
                  context,
                ).copyWith(textScaler: TextScaler.linear(escala)),
                child: child!,
              ),
            ),
          ),
        );
        await tester.pumpAndSettle();
        expect(router.state.uri.toString(), '/pasanaku/unirse/invalida');
        expect(
          find.textContaining('El enlace de invitación no es válido.'),
          findsOneWidget,
        );
        expect(find.textContaining('Pedí uno nuevo'), findsOneWidget);
        expect(find.textContaining('CI_FICTICIA'), findsNothing);
        expect(tester.takeException(), isNull);
        await expectLater(
          find.byType(MaterialApp),
          matchesGoldenFile(
            'imagenes/enlace_invalido_${ancho.toInt()}_${brillo.name}_texto_${(escala * 100).toInt()}.png',
          ),
        );
      },
      skip: !Platform.isWindows,
    );
  }
}
