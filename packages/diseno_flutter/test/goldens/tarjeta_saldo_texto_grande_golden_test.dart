import 'dart:io';

import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/moleculas/tarjeta_saldo.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  setUpAll(() async {
    final iconos = FontLoader('MaterialIcons')
      ..addFont(rootBundle.load('fonts/MaterialIcons-Regular.otf'));
    final bricolage = FontLoader('Bricolage')
      ..addFont(rootBundle.load('lib/fuentes/Bricolage-600.ttf'))
      ..addFont(rootBundle.load('lib/fuentes/Bricolage-700.ttf'));
    final instrument = FontLoader('Instrument')
      ..addFont(rootBundle.load('lib/fuentes/Instrument-400.ttf'))
      ..addFont(rootBundle.load('lib/fuentes/Instrument-600.ttf'));
    await Future.wait([iconos.load(), bricolage.load(), instrument.load()]);
  });

  for (final (brillo, nombre, ancho, disponible) in [
    (Brightness.light, '320_claro', 320, '1240.00'),
    (Brightness.dark, '360_oscuro', 360, '1240.00'),
    (Brightness.light, '620_claro', 620, '1240.00'),
    (Brightness.light, '640_claro', 640, '1240.00'),
    (Brightness.dark, '768_oscuro', 768, '1240.00'),
    (Brightness.light, '320_importe_largo', 320, '999999999999.99'),
  ]) {
    // Baselines Windows. La comparacion en macOS requiere capturas propias.
    testWidgets('tarjeta saldo al 200 % · $nombre', (tester) async {
      tester.view.physicalSize = Size(ancho.toDouble(), 1024);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(
        conTema(
          TarjetaSaldo(
            disponible: disponible,
            retenido: '150.00',
            enPasanakus: '2400.00',
            moneda: 'BOB',
            accionPrincipal: Boton(
              texto: 'Recargar',
              variante: BotonVariante.primario,
              onPressed: () {},
            ),
            accionSecundaria: Boton(
              texto: 'Retirar',
              variante: BotonVariante.sobreVerde,
              onPressed: () {},
            ),
          ),
          brillo: brillo,
          escala: 2,
        ),
      );
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      await expectLater(
        find.byType(TarjetaSaldo),
        matchesGoldenFile('imagenes/tarjeta_saldo_texto_grande_$nombre.png'),
      );
    }, skip: !Platform.isWindows);
  }
}
