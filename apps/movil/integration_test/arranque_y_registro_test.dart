// Recorre en iOS la entrada real de una persona sin sesión.
// El alta completa con cámara y operaciones de dinero requiere un dispositivo
// físico y la API real; ese gate no se sustituye por este recorrido.
import 'package:patrol/patrol.dart';

import '_soporte.dart';

void main() {
  // Desde 2026-10-28 «Crear mi cuenta» abre el alta directo, sin el tour (el tour
  // queda en «¿Cómo funciona?»), y el paso 1 pide contacto y contraseña; los nombres
  // van en un paso posterior.
  patrolTest('portada y formulario de registro', ($) async {
    await arrancarApp($);

    await $('Crear mi cuenta').tap();

    await $('Crear cuenta').waitUntilVisible();
    await $('Celular').waitUntilVisible();
    await $('Correo electrónico').waitUntilVisible();
    await $('Contraseña').waitUntilVisible();
  });
}
