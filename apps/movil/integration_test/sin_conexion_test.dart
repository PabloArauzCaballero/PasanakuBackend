// F12.1 — Patrol apaga la red del dispositivo: último estado visible, operaciones
// bloqueadas. NO EJECUTABLE EN ESTE SANDBOX (ver integration_test/LEEME.md). Esto es
// exactamente lo que `integration_test` solo no puede tocar: el modo avión es fuera
// del árbol de Flutter.
import 'package:integration_test/integration_test.dart';
import 'package:patrol/patrol.dart';

import '_soporte.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  patrolTest('sin red: último estado visible y operaciones bloqueadas', ($) async {
    await arrancarApp($);
    await $('Recargar saldo').waitUntilVisible();

    await $.native.disableWifi();
    await $.native.disableCellular();
    await $.pumpAndSettle();

    // El último estado (saldo, movimientos) sigue en pantalla: no se borra por
    // quedarse sin red (planes/15 §F12.4, "sin datos perdidos").
    await $('Recargar saldo').waitUntilVisible();
    await $('Recargar saldo').tap();
    await $('Sin conexión. Reintentar cuando vuelva la red.').waitUntilVisible();

    await $.native.enableWifi();
    await $.native.enableCellular();
  });
}
