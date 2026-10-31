import 'package:flutter/widgets.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../tokens/tokens.dart';

/// El color y el ícono de cada etapa del camino de reputación, por su **orden** (0 es
/// la de abajo). El sistema de diseño no conoce los códigos del catálogo: recibe la
/// posición, igual que [ColoresDeTurno] recibe el número de turno.
///
/// Todos salen de la paleta de AportaYa, sin un solo color nuevo. El orden cuenta un
/// camino que sube, de la recuperación al pilar: verdes que crecen, naranjas cálidos,
/// el azul, el plateado y el oro al final. El color **no es la única señal**: la
/// insignia siempre va con el nombre de la etapa al lado.
///
/// El ícono va en [Paleta.ink] o en [Paleta.white], el que más contraste dé contra el
/// relleno (≥ 3:1, WCAG 1.4.11, en los once).
abstract final class AspectoDeEtapa {
  static const _rellenos = [
    Paleta.muted, // 0 · recuperación, sobria
    Paleta.g300, // 1 · inicial
    Paleta.g400, // 2 · en marcha
    Paleta.pii, // 3 · constante
    Paleta.o400, // 4 · puntual
    Paleta.o600, // 5 · comprometido
    Paleta.o700, // 6 · sólido
    Paleta.info, // 7 · destacado
    Paleta.n400, // 8 · experto
    Paleta.n600, // 9 · ejemplar
    Paleta.warn, // 10 · pilar: oro
  ];

  static const _iconos = [
    LucideIcons.tent,
    LucideIcons.sprout,
    LucideIcons.leaf,
    LucideIcons.flower2,
    LucideIcons.store,
    LucideIcons.shoppingBasket,
    LucideIcons.mountain,
    LucideIcons.gem,
    LucideIcons.mountainSnow,
    LucideIcons.bird,
    LucideIcons.sun,
  ];

  /// Cuántas etapas tienen un aspecto propio. Más arriba se repite el último: un
  /// catálogo más largo no rompe nada, solo comparte el oro.
  static int get cuantas => _rellenos.length;

  static int _indice(int orden) => orden.clamp(0, _rellenos.length - 1);

  /// El relleno de la insignia de la etapa `orden`.
  static Color relleno(int orden) => _rellenos[_indice(orden)];

  /// El color del ícono sobre [relleno]: tinta o blanco, el de más contraste.
  static Color tintaSobre(int orden) {
    final fondo = relleno(orden).computeLuminance();
    final conTinta = (fondo + 0.05) / (Paleta.ink.computeLuminance() + 0.05);
    final conBlanco = (Paleta.white.computeLuminance() + 0.05) / (fondo + 0.05);
    return conTinta >= conBlanco ? Paleta.ink : Paleta.white;
  }

  /// El ícono de la etapa `orden`.
  static IconData icono(int orden) => _iconos[_indice(orden)];

  /// El ícono de una etapa que todavía no se alcanzó.
  static const IconData bloqueada = LucideIcons.lock;
}
