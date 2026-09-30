// F12.1 — URI de inicio y URL nativa con la app abierta.
// Estado de ejecución y límites: integration_test/LEEME.md.
//
// Nota: existe `apps/movil/test/widget/deep_link_test.dart`, que prueba
// `enlaces_profundos.dart` (la función pura que traduce el URI a ruta interna) sin
// SO real. Patrol comprueba la URI de inicio y la apertura nativa en caliente.
import 'package:patrol/patrol.dart';

import '_soporte.dart';

void main() {
  const codigoInicial =
      '12345678-1234-4234-8234-123456789abc.'
      '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';
  const codigoEnCaliente =
      '22345678-1234-4234-8234-123456789abc.'
      'fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210';

  patrolTest('01 URI de inicio abre la pantalla de unirse', ($) async {
    await arrancarApp($, inicial: 'aportaya://unirse/$codigoInicial');

    await $('Invitación al grupo').waitUntilVisible();
    await $('Ingresar').waitUntilVisible();
  });

  patrolTest('02 deep link con la app abierta navega sin reiniciar', ($) async {
    await arrancarApp($);
    await $('Crear mi cuenta').waitUntilVisible(); // la portada está viva

    await $.native.openUrl('aportaya://unirse/$codigoEnCaliente');
    await $('Invitación al grupo').waitUntilVisible();
    await $('Ingresar').waitUntilVisible();
  });
}
