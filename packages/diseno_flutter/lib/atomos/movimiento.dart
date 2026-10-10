import 'package:flutter/animation.dart';

/// **El movimiento de AportaYa, en un solo lugar.** Duraciones, curvas y resortes.
///
/// La app mueve plata de gente que confía: el movimiento tiene que sentirse firme y
/// rápido, nunca juguetón. Nada disparado por la persona pasa de 320 ms; la salida es
/// siempre más corta que la entrada; y nada escala la pantalla entera — los objetos se
/// desplazan y aparecen, no se inflan.
abstract final class Movimiento {
  /// Respuesta al dedo: hover, apretar, cambiar un color de estado.
  static const micro = Duration(milliseconds: 120);

  /// Un elemento que aparece o cambia de forma dentro de la pantalla.
  static const entrada = Duration(milliseconds: 220);

  /// Una pantalla nueva que llega.
  static const pagina = Duration(milliseconds: 300);

  /// Una pantalla que se va al volver: más corta que la que llega.
  static const paginaDeVuelta = Duration(milliseconds: 240);

  /// Lo que tarda el brillo en cruzar un botón principal.
  static const brillo = Duration(milliseconds: 1100);

  /// Cada cuánto vuelve a cruzar: lo bastante espaciado para no ser un parpadeo.
  static const pausaDelBrillo = Duration(milliseconds: 4200);

  /// Desacelera al llegar: lo que entra (Material «emphasized decelerate»).
  static const llega = Cubic(0.05, 0.7, 0.1, 1);

  /// Acelera al irse: lo que sale.
  static const sale = Cubic(0.3, 0, 0.8, 0.15);

  /// Para lo que va y vuelve dentro del mismo lugar.
  static const pareja = Curves.easeInOutCubic;

  /// El resorte del botón al soltarlo: pasa apenas de su tamaño y se asienta.
  static const resorte = SpringDescription(
    mass: 1,
    stiffness: 520,
    damping: 19,
  );

  /// Cuánto se corre lateralmente una pantalla al entrar, en píxeles lógicos.
  /// Treinta es lo que usa Material para su eje compartido: se lee como dirección
  /// sin que la pantalla viaje.
  static const corrimiento = 30.0;
}
