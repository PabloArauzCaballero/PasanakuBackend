// F12.1 — `aportaya://unirse/{codigo}` con la app cerrada y abierta.
// Estado de ejecución y límites: integration_test/LEEME.md.
//
// Nota: existe `apps/movil/test/widget/deep_link_test.dart`, que prueba
// `enlaces_profundos.dart` (la función pura que traduce el URI a ruta interna) sin
// SO real. Este archivo es el complemento con Patrol: abre el deep link como lo haría
// Android/iOS de verdad, con la app cerrada.
import 'package:patrol/patrol.dart';

import '_soporte.dart';

void main() {
  const codigo =
      '12345678-1234-4234-8234-123456789abc.'
      '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';

  patrolTest('deep link con la app cerrada abre la pantalla de unirse', (
    $,
  ) async {
    await $.native.openApp(); // instala/lanza en frío, sin estado previo.
    await $.native.openUrl('aportaya://unirse/$codigo');

    await $('Invitación al grupo').waitUntilVisible();
    await $('Ingresar').waitUntilVisible();
  });

  patrolTest('deep link con la app abierta navega sin reiniciar', ($) async {
    await arrancarApp($);
    await $('Crear mi cuenta').waitUntilVisible(); // la portada está viva

    await $.native.openUrl('aportaya://unirse/$codigo');
    await $('Invitación al grupo').waitUntilVisible();
    await $('Ingresar').waitUntilVisible();
  });
}
