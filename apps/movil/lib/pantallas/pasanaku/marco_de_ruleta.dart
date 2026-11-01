import 'dart:math' as math;

import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// Lo que no gira: el aro verde con sus focos, el brillo de vidrio y la aguja.
///
/// Los focos se encienden de a uno sí y uno no y la marcha avanza con [luces] (la ruleta
/// la acelera mientras gira); con [todas] se prenden juntos, para el cierre.
class PintorDeMarco extends CustomPainter {
  PintorDeMarco({required this.luces, required this.todas});

  final double luces;
  final bool todas;

  static const focos = 24;

  @override
  void paint(Canvas canvas, Size size) {
    final c = size.center(Offset.zero);
    final r = size.width / 2;
    final aro = Rect.fromCircle(center: c, radius: r);
    canvas.drawCircle(
      c,
      r * 0.93,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = r * 0.14
        ..shader = const SweepGradient(
          colors: [
            Paleta.g700,
            Paleta.g900,
            Paleta.g600,
            Paleta.g900,
            Paleta.g700,
          ],
        ).createShader(aro),
    );
    final bisel = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = r * 0.012
      ..color = Paleta.g300.withValues(alpha: 0.7);
    canvas.drawCircle(c, r * 0.995, bisel);
    canvas.drawCircle(c, r * 0.862, bisel);
    final marcha = luces.floor();
    for (var k = 0; k < focos; k++) {
      final a = -math.pi / 2 + k * 2 * math.pi / focos;
      final p = c + Offset(math.cos(a), math.sin(a)) * (r * 0.93);
      final prendido = todas || (k + marcha).isEven;
      if (prendido) {
        canvas.drawCircle(
          p,
          r * 0.05,
          Paint()
            ..color = Paleta.o300.withValues(alpha: 0.55)
            ..maskFilter = MaskFilter.blur(BlurStyle.normal, r * 0.03),
        );
      }
      canvas.drawCircle(
        p,
        r * 0.026,
        Paint()..color = prendido ? Paleta.crema : Paleta.g800,
      );
    }
    // Vidrio: una luz quieta arriba a la izquierda, que no gira con el disco.
    canvas.drawCircle(
      c,
      r * 0.86,
      Paint()
        ..shader = RadialGradient(
          center: const Alignment(-0.45, -0.55),
          radius: 0.9,
          colors: [
            Paleta.white.withValues(alpha: 0.22),
            Paleta.white.withValues(alpha: 0),
          ],
        ).createShader(aro),
    );
  }

  @override
  bool shouldRepaint(PintorDeMarco viejo) =>
      viejo.luces.floor() != luces.floor() || viejo.todas != todas;
}

/// La aguja: una gota naranja con borde claro que cuelga de arriba y golpea las
/// clavijas. [aleta] es cuánto la empuja la última clavija (radianes).
class PintorDeAguja extends CustomPainter {
  PintorDeAguja({required this.aleta});

  final double aleta;

  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    final h = size.height;
    canvas.save();
    canvas.translate(w / 2, h * 0.18);
    canvas.rotate(aleta);
    canvas.translate(-w / 2, -h * 0.18);
    final gota = Path()
      ..moveTo(w / 2, h)
      ..cubicTo(w * 0.05, h * 0.45, w * 0.05, 0, w / 2, 0)
      ..cubicTo(w * 0.95, 0, w * 0.95, h * 0.45, w / 2, h)
      ..close();
    canvas.drawShadow(gota, Paleta.ink, w * 0.12, false);
    canvas.drawPath(
      gota,
      Paint()
        ..shader = const LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Paleta.o400, Paleta.o600],
        ).createShader(Offset.zero & size),
    );
    canvas.drawPath(
      gota,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = w * 0.07
        ..color = Paleta.crema,
    );
    canvas.drawCircle(
      Offset(w / 2, h * 0.3),
      w * 0.13,
      Paint()..color = Paleta.crema,
    );
    canvas.restore();
  }

  @override
  bool shouldRepaint(PintorDeAguja viejo) => viejo.aleta != aleta;
}
