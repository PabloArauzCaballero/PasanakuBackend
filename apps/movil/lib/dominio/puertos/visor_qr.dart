import 'package:flutter/widgets.dart';

/// Por qué no hay cámara para escanear. Es lo único que la pantalla necesita saber para
/// decirle a la persona qué hacer; el detalle del plugin no sale del adaptador.
enum ProblemaDeCamara {
  /// La persona (o el sistema) no dio permiso a la cámara.
  denegado,

  /// Este dispositivo no puede escanear (sin cámara, simulador).
  noDisponible,

  /// Cualquier otra falla.
  otro,
}

/// **El visor de QR como puerto.** Devuelve el widget que muestra la cámara y llama a
/// [alLeer] con el texto de cada QR detectado (puede repetirse muchas veces por segundo:
/// el cerrojo es de quien lo usa). Si la cámara no arranca, pinta [problema].
typedef ConstructorDeVisor =
    Widget Function({
      required ValueChanged<String> alLeer,
      required Widget Function(ProblemaDeCamara) problema,
    });
