import 'dart:ui' as ui;

import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/widgets.dart';

import 'persona_de_ejemplo.dart';

/// Lentes oscuros con reflejo y una gorra con visera.
void pintarAccesoriosDePersona(Canvas canvas) {
  final gorra = Path()
    ..addArc(cabezaDePersona.inflate(6), 3.14159, 3.14159)
    ..close();
  canvas.save();
  canvas.clipRect(const Rect.fromLTWH(0, 0, 300, 100));
  canvas.drawPath(
    gorra,
    Paint()
      ..shader = ui.Gradient.linear(
        const Offset(150, 56),
        const Offset(150, 100),
        [Paleta.g500, Paleta.g800],
      ),
  );
  canvas.restore();
  canvas.drawCircle(const Offset(150, 58), 4, Paint()..color = Paleta.g800);
  canvas.drawRRect(
    RRect.fromLTRBR(88, 90, 226, 102, const Radius.circular(6)),
    Paint()
      ..shader = ui.Gradient.linear(
        const Offset(150, 90),
        const Offset(150, 102),
        [Paleta.g600, Paleta.g900],
      ),
  );
  for (final x in [111.0, 154.0]) {
    final lente = RRect.fromLTRBR(
      x,
      117,
      x + 35,
      140,
      const Radius.circular(10),
    );
    canvas.drawRRect(
      lente,
      Paint()
        ..shader = ui.Gradient.linear(Offset(x, 117), Offset(x + 35, 140), [
          Paleta.slate,
          Paleta.ink,
        ]),
    );
    canvas.drawLine(
      Offset(x + 8, 122),
      Offset(x + 15, 122),
      Paint()
        ..color = Paleta.white.withValues(alpha: 0.6)
        ..strokeWidth = 2.5
        ..strokeCap = StrokeCap.round,
    );
  }
  canvas.drawRect(
    const Rect.fromLTWH(146, 124, 8, 4),
    Paint()..color = Paleta.ink,
  );
}
