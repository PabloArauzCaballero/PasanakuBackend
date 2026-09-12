// F12.1 — Una notificación push abre la pantalla correcta y NO muestra el monto en
// la bandeja del sistema. NO EJECUTABLE EN ESTE SANDBOX (ver integration_test/LEEME.md).
// El panel de notificaciones del sistema operativo está fuera del árbol de Flutter:
// por eso esto necesita Patrol, no alcanza con `integration_test` solo.
import 'package:integration_test/integration_test.dart';
import 'package:patrol/patrol.dart';

import '_soporte.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  patrolTest('push de aporte recibido abre la pantalla y no expone el monto', ($) async {
    await arrancarApp($);
    await $.native.pressHome();

    // Simula la llegada de un push (F12.5: "ningún token, PIN, monto ni número de
    // cuenta en logs, trazas o capturas" aplica también a la bandeja del sistema).
    await $.native.openNotifications();
    final notificacion = $.native.getNativeViews(Selector(textContains: 'aportó'));
    expect(notificacion, isNotEmpty);
    for (final n in notificacion) {
      expect(n.text, isNot(contains(RegExp(r'Bs\.?\s*\d'))));
    }

    await $.native.openNotification(index: 0);
    await $('Movimientos del grupo').waitUntilVisible();
  });
}
