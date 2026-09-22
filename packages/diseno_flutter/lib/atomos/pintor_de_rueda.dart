import 'dart:math' as math;

import 'package:flutter/material.dart';

/// Dibuja los segmentos de [Rueda]. Aparte por el barrido de 200 líneas, y porque un
/// pintor sin estado se prueba solo.
class PintorDeRueda extends CustomPainter {
  const PintorDeRueda({
    required this.turnos,
    required this.cobrados,
    required this.miTurno,
    required this.turnoActual,
    required this.colorDeTurno,
    required this.colorPendiente,
    required this.colorDelPunto,
    required this.colorDelBorde,
  });

  final int turnos;
  final int cobrados;
  final int? miTurno;
  final int? turnoActual;
  final Color Function(int) colorDeTurno;
  final Color colorPendiente;
  final Color colorDelPunto;
  final Color colorDelBorde;

  @override
  void paint(Canvas lienzo, Size medida) {
    final lado = medida.shortestSide;
    // El trazo crece con el diámetro pero no linealmente: a 36 px un trazo proporcional
    // al de 236 px taparía el hueco del centro y la rueda se vería como un disco.
    final grosor = (lado * 0.11).clamp(4.0, 22.0);
    final radio = (lado - grosor) / 2;
    final centro = Offset(medida.width / 2, medida.height / 2);
    // Un respiro entre segmentos, en radianes, para que se lean como tramos separados.
    final hueco = math.min(0.09, (math.pi * 2 / turnos) * 0.18);
    final paso = math.pi * 2 / turnos;
    final rectangulo = Rect.fromCircle(center: centro, radius: radio);

    final pincel = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = grosor
      ..strokeCap = StrokeCap.butt;

    for (var i = 0; i < turnos; i++) {
      final desde = -math.pi / 2 + i * paso + hueco / 2;
      pincel.color = switch (i) {
        _ when i < cobrados => colorDeTurno(i),
        _ when i == turnoActual => colorDeTurno(i).withValues(alpha: 0.4),
        _ => colorPendiente,
      };
      lienzo.drawArc(rectangulo, desde, paso - hueco, false, pincel);
    }

    final mio = miTurno;
    if (mio != null && mio >= 0 && mio < turnos) {
      final angulo = -math.pi / 2 + (mio + 0.5) * paso;
      final punto = centro + Offset(math.cos(angulo), math.sin(angulo)) * radio;
      final radioDelPunto = (lado * 0.035).clamp(1.8, 5.0);
      lienzo.drawCircle(
        punto,
        radioDelPunto + (lado >= 120 ? 2.5 : 1.2),
        Paint()..color = colorDelBorde,
      );
      lienzo.drawCircle(punto, radioDelPunto, Paint()..color = colorDelPunto);
    }
  }

  @override
  bool shouldRepaint(PintorDeRueda otro) =>
      otro.turnos != turnos ||
      otro.cobrados != cobrados ||
      otro.miTurno != miTurno ||
      otro.turnoActual != turnoActual ||
      otro.colorPendiente != colorPendiente ||
      otro.colorDelPunto != colorDelPunto ||
      otro.colorDelBorde != colorDelBorde;
}
