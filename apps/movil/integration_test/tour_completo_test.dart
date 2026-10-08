import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:patrol/patrol.dart';

import '_soporte.dart';

void main() {
  patrolTest('las cuatro láminas avanzan con el botón hasta el alta', (
    $,
  ) async {
    await arrancarApp($);

    await $(TextosIdentidad.portadaCrearCuenta).tap();
    for (final texto in [
      TextosIdentidad.tour1Texto,
      TextosIdentidad.tour2Texto,
      TextosIdentidad.tour3Texto,
      TextosIdentidad.tour4Texto,
    ]) {
      await $(texto).waitUntilVisible();
      if (texto != TextosIdentidad.tour4Texto) {
        await $(TextosIdentidad.tourSiguiente).tap();
      }
    }

    await $(TextosIdentidad.portadaCrearCuenta).tap();
    await $(TextosIdentidad.registroTitulo).waitUntilVisible();
  });
}
