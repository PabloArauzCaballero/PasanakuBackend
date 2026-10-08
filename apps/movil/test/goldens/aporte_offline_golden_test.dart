import 'dart:io';

import 'package:aportaya_movil/dominio/puertos/conectividad.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_aportar.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

class _SinConexion implements Conectividad {
  @override
  Stream<bool> get cambios => const Stream<bool>.empty();

  @override
  Future<bool> hayConexion() async => false;
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

  for (final brillo in [Brightness.light, Brightness.dark]) {
    for (final estado in ['formulario', 'revision', 'revision_accion']) {
      testWidgets(
        'aporte offline $estado · 360 texto 200 · ${brillo.name}',
        (tester) async {
          tester.view.physicalSize = const Size(360, 760);
          tester.view.devicePixelRatio = 1;
          addTearDown(tester.view.reset);
          await tester.pumpWidget(
            conApp(
              const PantallaAportar(
                obligacionId: '68a98585-2854-4b71-80fd-5ef490fe60f6',
                montoDelPeriodo: '125.00',
              ),
              dio: dioSimulado().dio,
              conectividad: _SinConexion(),
              brillo: brillo,
              escalaDeTexto: 2,
            ),
          );
          await asentar(tester);
          if (estado != 'formulario') {
            await tester.enterText(find.byType(TextField), 'REF-OFFLINE');
            final revisar = find.widgetWithText(FilledButton, 'Revisar aporte');
            await tester.ensureVisible(revisar);
            await tester.pumpAndSettle();
            await tester.tap(revisar);
            await tester.pumpAndSettle();
            if (estado == 'revision') {
              final desplazamiento = tester
                  .state<ScrollableState>(find.byType(Scrollable).first)
                  .position
                  .pixels;
              expect(desplazamiento, 0);
              expect(
                tester.getRect(find.text('Sin conexión')).top,
                lessThan(100),
              );
            }
            if (estado == 'revision_accion') {
              await tester.ensureVisible(
                find.widgetWithText(FilledButton, 'Confirmar aporte'),
              );
              await tester.pumpAndSettle();
            }
          }
          expect(tester.takeException(), isNull);
          await expectLater(
            find.byType(PantallaAportar),
            matchesGoldenFile(
              'imagenes/aporte_offline_${estado}_360_texto_200_${brillo.name}.png',
            ),
          );
        },
        skip: !Platform.isWindows,
      );
    }

    testWidgets('aporte offline revision · 768 · ${brillo.name}', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(768, 1024);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(
        conApp(
          const PantallaAportar(
            obligacionId: '68a98585-2854-4b71-80fd-5ef490fe60f6',
            montoDelPeriodo: '125.00',
          ),
          dio: dioSimulado().dio,
          conectividad: _SinConexion(),
          brillo: brillo,
        ),
      );
      await asentar(tester);
      await tester.enterText(find.byType(TextField), 'REF-OFFLINE');
      final revisar = find.widgetWithText(FilledButton, 'Revisar aporte');
      await tester.ensureVisible(revisar);
      await tester.pumpAndSettle();
      await tester.tap(revisar);
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      await expectLater(
        find.byType(PantallaAportar),
        matchesGoldenFile(
          'imagenes/aporte_offline_revision_768_${brillo.name}.png',
        ),
      );
    }, skip: !Platform.isWindows);
  }
}
