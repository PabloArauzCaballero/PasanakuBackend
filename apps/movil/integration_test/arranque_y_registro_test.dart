// Recorre en iOS la entrada real de una persona sin sesión.
// El alta completa con cámara y operaciones de dinero requiere un dispositivo
// físico y la API real; ese gate no se sustituye por este recorrido.
import 'package:patrol/patrol.dart';

import '_soporte.dart';

void main() {
  patrolTest('portada, tour y formulario de registro', ($) async {
    await arrancarApp($);

    await $('Crear mi cuenta').tap();
    await $('Saltar').tap();

    await $('Crear cuenta').waitUntilVisible();
    await $('Nombres').waitUntilVisible();
    await $('Correo electrónico').waitUntilVisible();
  });
}
