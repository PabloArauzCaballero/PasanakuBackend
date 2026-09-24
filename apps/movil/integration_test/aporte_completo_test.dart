// F12.1 — Entrar al grupo → Mi aporte → pagar con saldo → ver el movimiento.
// NO EJECUTABLE EN ESTE SANDBOX (ver integration_test/LEEME.md).
import 'package:patrol/patrol.dart';

import '_soporte.dart';

void main() {
  patrolTest('pagar mi aporte con saldo de billetera', ($) async {
    await arrancarApp($);

    await $(
      'Grupos',
    ).tap(); // rama /pasanaku de la tab bar (navegacion/shell.dart)
    await $('Mi aporte').tap();
    await $('Pagar con saldo').tap();
    await $('Confirmar').tap();

    await $('Aporte registrado').waitUntilVisible();
    await $('Ver en mis movimientos').tap();
    await $('Aporte').waitUntilVisible();
  });
}
