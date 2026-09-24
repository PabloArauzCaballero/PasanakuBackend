// F12.1 — Cobrar mi turno → deducciones → neto → línea de tiempo.
// NO EJECUTABLE EN ESTE SANDBOX (ver integration_test/LEEME.md).
import 'package:patrol/patrol.dart';

import '_soporte.dart';

void main() {
  patrolTest('cobrar mi turno muestra deducciones y neto', ($) async {
    await arrancarApp($);

    await $('Grupos').tap();
    await $('Cobrar mi turno').tap();

    await $('Deducciones').waitUntilVisible();
    await $('Neto a recibir').waitUntilVisible();
    await $('Confirmar cobro').tap();

    await $('Línea de tiempo').tap();
    await $('Entrega').waitUntilVisible();
  });
}
