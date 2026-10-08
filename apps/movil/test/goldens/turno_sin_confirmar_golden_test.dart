import 'dart:io';

import 'package:aportaya_movil/pantallas/pasanaku/pantalla_turno.dart';
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

  for (final (brillo, nombre, tamano, escala) in [
    (Brightness.light, '360_claro', const Size(360, 760), 1.0),
    (Brightness.dark, '360_oscuro', const Size(360, 760), 1.0),
    (Brightness.light, '360_texto_200_claro', const Size(360, 760), 2.0),
    (Brightness.dark, '360_texto_200_oscuro', const Size(360, 760), 2.0),
    (Brightness.light, '768_claro', const Size(768, 1024), 1.0),
  ]) {
    // Referencias rasterizadas en Windows; macOS necesita sus propias bases.
    testWidgets('turno sin confirmar · $nombre', (tester) async {
      tester.view.physicalSize = tamano;
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      const sorteoId = '11111111-1111-4111-8111-111111111111';
      final simulado = dioSimulado();
      simulado.adaptador.onGet(
        '/grupos/sorteos/$sorteoId/paquete',
        (s) => s.reply(
          200,
          ejemplo('grupos', 'consultarPaqueteDelSorteo', 'ok')['cuerpo'],
        ),
      );
      await tester.pumpWidget(
        conApp(
          const PantallaTurno(sorteoId: sorteoId),
          dio: simulado.dio,
          brillo: brillo,
          escalaDeTexto: escala,
        ),
      );
      await asentar(tester);
      expect(tester.takeException(), isNull);
      await expectLater(
        find.byType(PantallaTurno),
        matchesGoldenFile('imagenes/turno_sin_confirmar_$nombre.png'),
      );
    }, skip: !Platform.isWindows);
  }
}
