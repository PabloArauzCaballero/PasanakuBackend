import 'avance.dart';
import 'modelo.dart';

/// **¿Se le ofrece sola la guía de inicio a quien está mirando?**
///
/// Solo si ese tutorial nunca se hizo (o cambió de versión: [estadoDe] ya lo cuenta como
/// pendiente). Un tutorial empezado, omitido o completado no se vuelve a lanzar solo:
/// insistirle a quien lo saltó es molestar, y repetirlo es decisión suya desde Perfil o
/// el centro de ayuda. Sin IO y sin estado, como el resto de las cuentas del avance.
bool debeAutolanzar(
  TutorialDefinicion? tutorial,
  ProgresoDeTutorial? progreso,
) =>
    tutorial != null &&
    tutorial.pasos.isNotEmpty &&
    estadoDe(progreso, tutorial) == EstadoDeProgreso.pendiente;
