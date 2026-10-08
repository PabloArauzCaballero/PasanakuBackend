import 'package:aportaya_movil/pantallas/billetera/pantalla_retirar.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_transferir.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

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

  const cuenta = '22222222-2222-4222-8222-222222222222';
  for (final (brillo, nombre) in [
    (Brightness.light, 'claro'),
    (Brightness.dark, 'oscuro'),
  ]) {
    for (final (pantalla, id) in [
      (const PantallaRetirar(cuentaId: cuenta), 'retirar'),
      (const PantallaTransferir(cuentaOrigenId: cuenta), 'transferir'),
    ]) {
      testWidgets('$id · formulario · $nombre', (tester) async {
        tester.view.physicalSize = const Size(360, 760);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        await tester.pumpWidget(
          conApp(pantalla, dio: dioSimulado().dio, brillo: brillo),
        );
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);

        await expectLater(
          find.byType(pantalla.runtimeType),
          matchesGoldenFile('imagenes/${id}_formulario_$nombre.png'),
        );
      });
    }
  }
}
