import 'dart:ui' as ui;

import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/widgets.dart';

/// Cara y pelo de la persona de ejemplo, en el lienzo de 300 × 300 de `persona_de_ejemplo.dart`.

void pintarRasgosDePersona(Canvas canvas) {
  final trazo = Paint()
    ..style = PaintingStyle.stroke
    ..strokeCap = StrokeCap.round;
  // Cejas.
  for (final s in [-1.0, 1.0]) {
    canvas.drawPath(
      Path()
        ..moveTo(150 + s * 9, 113)
        ..quadraticBezierTo(150 + s * 20, 106, 150 + s * 31, 112),
      Paint.from(trazo)
        ..color = Paleta.slate
        ..strokeWidth = 4,
    );
  }
  // Ojos con un brillo, para que miren.
  for (final x in [131.0, 169.0]) {
    canvas.drawOval(
      Rect.fromCenter(center: Offset(x, 129), width: 9, height: 11),
      Paint()..color = Paleta.ink,
    );
    canvas.drawCircle(
      Offset(x + 1.8, 126.5),
      1.8,
      Paint()..color = Paleta.white,
    );
  }
  // Mejillas.
  for (final x in [124.0, 176.0]) {
    canvas.drawCircle(
      Offset(x, 152),
      9,
      Paint()
        ..color = Paleta.o400.withValues(alpha: 0.3)
        ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 4),
    );
  }
  // Nariz.
  canvas.drawPath(
    Path()
      ..moveTo(151, 132)
      ..quadraticBezierTo(146, 147, 145, 152)
      ..quadraticBezierTo(150, 156, 155, 153),
    Paint.from(trazo)
      ..color = Paleta.o600.withValues(alpha: 0.6)
      ..strokeWidth = 2.6,
  );
  // Sonrisa.
  canvas.drawPath(
    Path()
      ..moveTo(137, 166)
      ..quadraticBezierTo(150, 176, 163, 166),
    Paint.from(trazo)
      ..color = Paleta.o700
      ..strokeWidth = 3.4,
  );
}

void pintarPeloDePersona(Canvas canvas) {
  final pelo = Path()
    ..moveTo(96, 140)
    ..cubicTo(86, 74, 118, 48, 150, 50)
    ..cubicTo(184, 48, 214, 74, 204, 140)
    ..cubicTo(200, 116, 194, 100, 182, 92)
    ..cubicTo(166, 102, 136, 96, 122, 88)
    ..cubicTo(108, 98, 100, 114, 96, 140)
    ..close();
  canvas.drawPath(
    pelo,
    Paint()
      ..shader = ui.Gradient.linear(
        const Offset(120, 50),
        const Offset(180, 140),
        [Paleta.slate, Paleta.ink],
      ),
  );
  // Un brillo sobre el pelo.
  canvas.drawPath(
    Path()
      ..moveTo(118, 66)
      ..quadraticBezierTo(140, 56, 164, 60),
    Paint()
      ..color = Paleta.muted.withValues(alpha: 0.7)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 4
      ..strokeCap = StrokeCap.round,
  );
}
