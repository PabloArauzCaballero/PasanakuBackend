import 'package:flutter/material.dart';

import '../atomos/pintor_de_aura.dart';
import '../tokens/tokens.dart';

/// Pone [hijo] sobre el aura de marca, que es el fondo de las pantallas de entrada.
///
/// El aura va **detrás y sin tocar el toque**: es `IgnorePointer` y `ExcludeSemantics`,
/// así que ni recibe dedos ni le aparece a quien navega con lector de pantalla.
///
/// Las intensidades son bajas a propósito —ninguna luz llega al 10 %—: se tiene que
/// notar que el fondo no es un papel liso, sin que nadie pueda decir qué vio. En
/// oscuro se suben, porque sobre un fondo oscuro la misma transparencia se pierde, y
/// el viñeteo se hace más profundo, que es como se ve la luz de verdad en un cuarto
/// oscuro.
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
                  luz: t.surface.withValues(alpha: oscuro ? 0.10 : 0.85),
                  halo: t.brand.withValues(alpha: oscuro ? 0.18 : 0.09),
                  brillo: t.accent.withValues(alpha: oscuro ? 0.14 : 0.08),
                  linea: t.brand.withValues(alpha: oscuro ? 0.10 : 0.06),
                  vineta: Paleta.g900.withValues(alpha: oscuro ? 0.30 : 0.06),
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
