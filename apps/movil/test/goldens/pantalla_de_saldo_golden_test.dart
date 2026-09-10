import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';

const cuenta = '11111111-1111-4111-8111-111111111111';

/// Goldens en claro y oscuro: el píxel es el criterio, y se compara contra la maqueta
/// en la revisión visual. `--update-goldens` no se corre a ciegas.
void main() {
  for (final (brillo, nombre) in [
    (Brightness.light, 'claro'),
    (Brightness.dark, 'oscuro'),
  ]) {
    testWidgets('pantalla de saldo · éxito · $nombre', (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        '/billetera/$cuenta/saldo',
        (s) => s.reply(
          200,
          ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
        ),
      );
      tester.view.physicalSize = const Size(360, 640);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(
        conApp(
          const PantallaDeSaldo(cuentaId: cuenta),
          dio: dio,
          brillo: brillo,
        ),
      );
      await asentar(tester);
      await expectLater(
        find.byType(PantallaDeSaldo),
        matchesGoldenFile('imagenes/pantalla_de_saldo_$nombre.png'),
      );
    });
  }
}
