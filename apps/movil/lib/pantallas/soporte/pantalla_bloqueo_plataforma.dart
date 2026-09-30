import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// Se muestra en vez de la app entera cuando, en un RELEASE de iOS, el almacén
/// seguro o la protección de pantalla no están soportados (H2.S2.M3, madre
/// H6.S2.M3). `app.dart` la elige ANTES de construir el router y de leer
/// `verificacionContratoProvider` -- que es lo que crea el cliente HTTP
/// (`dioProvider`, en `dominio/cliente.dart`) -- así que ningún dato de sesión ni
/// ninguna petición sale del teléfono mientras esta pantalla está en foco.
///
/// No tiene botón de "continuar igual": las dos capacidades que protege son las que
/// evitan que el token de sesión quede legible en el dispositivo (almacén) o que una
/// captura de pantalla se lleve el saldo (protección de pantalla). Ninguna de las
/// dos es negociable.
class PantallaBloqueoPlataforma extends StatelessWidget {
  const PantallaBloqueoPlataforma({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(Espacio.s5),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.lock_outline, size: Espacio.s7),
                const SizedBox(height: Espacio.s4),
                Text(
                  'AportaYa no puede abrir en este dispositivo',
                  style: Theme.of(context).textTheme.titleLarge,
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: Espacio.s2),
                Text(
                  'Esta versión no puede proteger tu sesión ni tu saldo en este '
                  'teléfono todavía. Actualizá la app cuando haya una versión '
                  'nueva, o contactá a soporte si el problema sigue.',
                  style: Theme.of(context).textTheme.bodyMedium,
                  textAlign: TextAlign.center,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
