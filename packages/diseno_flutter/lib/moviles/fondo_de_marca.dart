import 'package:flutter/material.dart';

import '../atomos/pintor_de_aura.dart';
import '../tokens/tokens.dart';

/// Pone [hijo] sobre el aura de marca, que es el fondo de las pantallas de entrada.
///
/// El aura va **detrás y sin tocar el toque**: es `IgnorePointer` y `ExcludeSemantics`,
/// así que ni recibe dedos ni le aparece a quien navega con lector de pantalla.
///
/// Las intensidades son bajas a propósito —el halo de marca no llega al 9 % y los
/// arcos no pasan del 4 %—: se tiene que notar que el fondo no es un papel liso, sin
/// que nadie pueda decir qué vio. En oscuro se sube apenas, porque sobre un fondo
/// oscuro la misma transparencia se pierde.
class FondoDeMarca extends StatelessWidget {
  const FondoDeMarca({super.key, required this.hijo});

  final Widget hijo;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final oscuro = Theme.of(context).brightness == Brightness.dark;
    return ColoredBox(
      color: t.bg,
      child: Stack(
        fit: StackFit.expand,
        children: [
          IgnorePointer(
            child: ExcludeSemantics(
              child: CustomPaint(
                painter: PintorDeAura(
                  halo: t.brand.withValues(alpha: oscuro ? 0.16 : 0.085),
                  brillo: t.accent.withValues(alpha: oscuro ? 0.12 : 0.07),
                  linea: t.brand.withValues(alpha: oscuro ? 0.10 : 0.06),
                ),
              ),
            ),
          ),
          hijo,
        ],
      ),
    );
  }
}
