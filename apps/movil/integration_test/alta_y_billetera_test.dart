// F12.1 — CU de alta con cámara → contrato → recarga → movimientos.
// NO EJECUTABLE EN ESTE SANDBOX: falta `patrol` e `integration_test` en
// apps/movil/pubspec.yaml (ver integration_test/LEEME.md). Andamiaje listo para
// correr en cuanto ese micro-PR exista.
import 'package:integration_test/integration_test.dart';
import 'package:patrol/patrol.dart';

import '_soporte.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  patrolTest('registro con cámara, contrato, recarga y movimientos', ($) async {
    await arrancarApp($);

    // El punto de entrada real es la billetera (initialLocation de crearEnrutador).
    await $('Perfil').tap(); // navega a /identidad, donde arranca el alta si no hay sesión.
    await $('Empezar mi registro').tap();

    // Patrol acepta el diálogo NATIVO de permiso de cámara — esto es exactamente lo
    // que `integration_test` solo no puede hacer (planes/15 §F12.1).
    await $.native.grantPermissionWhenInUse();

    await $('Tomar foto del documento').tap();
    await $.pumpAndSettle();
    await $('Firmar el contrato de adhesión').tap();
    await $('Acepto').tap();

    await $('Recargar saldo').tap();
    await $(TextField).enterText('100');
    await $('Confirmar recarga').tap();

    await $('Ver movimientos').tap();
    await $('Recarga').waitUntilVisible();
  });
}
