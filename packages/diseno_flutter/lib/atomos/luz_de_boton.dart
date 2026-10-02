import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// El filo de luz de arriba y la franja diagonal que cruza.
class LuzDeBoton extends CustomPainter {
  const LuzDeBoton({required this.avance, required this.filo});

  final double? avance;
  final Color filo;

  @override
  void paint(Canvas canvas, Size size) {
    canvas.drawRect(
      Rect.fromLTWH(0, 0, size.width, Borde.fino),
      Paint()..color = filo.withValues(alpha: 0.28),
    );
    final a = avance;
    if (a == null) return;
    final ancho = size.height * 1.6;
    final x = -ancho + (size.width + 2 * ancho) * a;
    final franja = Rect.fromLTWH(x, 0, ancho, size.height);
    canvas.drawRect(
      Rect.fromLTWH(0, 0, size.width, size.height),
      Paint()
        ..shader = LinearGradient(
          begin: Alignment.centerLeft,
          end: Alignment.centerRight,
          transform: const GradientRotation(0.35),
          colors: [
            filo.withValues(alpha: 0),
            filo.withValues(alpha: 0.32),
            filo.withValues(alpha: 0),
          ],
        ).createShader(franja),
    );
  }

  @override
  bool shouldRepaint(LuzDeBoton vieja) => vieja.avance != avance;
}
