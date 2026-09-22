// Goldens: se corren con `yarn test:goldens`, NO en el CI.
//
// Comparan pixeles, y el rasterizador de Flutter no da el mismo resultado en macOS que en
// Linux aunque la fuente sea la misma. Estas lineas base estan hechas en macOS; la primera
// vez que el CI llego a ejecutarlas —hasta hoy el paso de lint fallaba antes— reporto
// diferencias de 2,5% a 3,9% en quince imagenes, sin que nadie hubiera tocado el diseño.
// Un gate que falla por la maquina donde corre no mide el diseño: mide la maquina.
//
// Para que el CI pueda exigirlas hay que generar las lineas base en linux/amd64, que es
// donde corre, y regenerarlas siempre ahi. Mientras tanto siguen siendo lo que el propio
// catalogo dice que son: la herramienta de la revision visual contra la maqueta, que se
// mira con ojos y no con un umbral de pixeles.

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
