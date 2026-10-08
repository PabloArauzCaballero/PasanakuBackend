// Evidencia visual local: estas líneas base dependen del rasterizador de Windows.
// Se inspeccionan en la revisión de diseño; no sustituyen las pruebas funcionales.

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:aportaya_movil/pantallas/pasanaku/pantalla_aportar.dart';

import '../comun.dart';

const _obligacionId = '68a98585-2854-4b71-80fd-5ef490fe60f6';
const _ruta = '/aportes/obligaciones/$_obligacionId/pagos';

void main() {
  setUpAll(() async {
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
    await Future.wait([bricolage.load(), instrument.load()]);
  });

  for (final (brillo, nombre) in [
    (Brightness.light, 'claro'),
    (Brightness.dark, 'oscuro'),
  ]) {
    testWidgets('aporte · formulario · $nombre', (tester) async {
      final (:dio, adaptador: _) = dioSimulado();
      _telefono(tester);
      await tester.pumpWidget(
        conApp(
          const PantallaAportar(
            obligacionId: _obligacionId,
            montoDelPeriodo: '125.00',
          ),
          dio: dio,
          brillo: brillo,
        ),
      );
      await tester.pumpAndSettle();

      await expectLater(
        find.byType(PantallaAportar),
        matchesGoldenFile('imagenes/aporte_formulario_$nombre.png'),
      );
    });

    testWidgets('aporte · revisión · $nombre', (tester) async {
      final (:dio, adaptador: _) = dioSimulado();
      _telefono(tester);
      await tester.pumpWidget(
        conApp(
          const PantallaAportar(
            obligacionId: _obligacionId,
            montoDelPeriodo: '125.00',
          ),
          dio: dio,
          brillo: brillo,
        ),
      );
      await tester.pump();
      await tester.enterText(find.byType(TextField), 'REF-001');
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Revisar aporte'));
      await tester.pumpAndSettle();

      await expectLater(
        find.byType(PantallaAportar),
        matchesGoldenFile('imagenes/aporte_revision_$nombre.png'),
      );
    });

    testWidgets('aporte · confirmado · $nombre', (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        _ruta,
        (servidor) => servidor.reply(
          200,
          ejemplo('aportes', 'cobrarAporte', 'ok')['cuerpo'],
        ),
      );
      _telefono(tester);
      await tester.pumpWidget(
        conApp(
          const PantallaAportar(
            obligacionId: _obligacionId,
            montoDelPeriodo: '125.00',
          ),
          dio: dio,
          brillo: brillo,
        ),
      );
      await tester.pump();
      await tester.enterText(find.byType(TextField), 'REF-001');
      await tester.tap(find.widgetWithText(FilledButton, 'Revisar aporte'));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Confirmar aporte'));
      await asentar(tester);

      await expectLater(
        find.byType(PantallaAportar),
        matchesGoldenFile('imagenes/aporte_confirmado_$nombre.png'),
      );
    });
  }
}

void _telefono(WidgetTester tester) {
  tester.view.physicalSize = const Size(360, 760);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
}
