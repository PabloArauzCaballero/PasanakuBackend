import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../atomos/colores_de_turno.dart';
import '../tokens/tokens.dart';
import 'lienzo.dart';

/// **Qué es un pasanaku**: una rueda de personas que se turnan el pozo.
///
/// Ocho personas alrededor de un anillo segmentado, cada una del color de su turno, y
/// en el centro el pozo. Es la definición del producto en un dibujo: no hace falta
/// leer nada para entender que se trata de un grupo que da la vuelta.
class RuedaDeGente extends CustomPainter {
  const RuedaDeGente(this.t);
  final Tokens t;

  static const _cuantos = 8;

  @override
  void paint(Canvas canvas, Size size) {
    final l = Lienzo(canvas, size, t)..escenario();
    final radioAnillo = l.p(0.27);
    final radioGente = l.p(0.42);
    final grosor = l.p(0.055);

    // El anillo: los turnos ya cobrados en color, los que faltan en línea.
    final rect = Rect.fromCircle(center: l.centro, radius: radioAnillo);
    final paso = math.pi * 2 / _cuantos;
    const hueco = 0.10;
    for (var i = 0; i < _cuantos; i++) {
      final pintado = i < 3;
      canvas.drawArc(
        rect,
        -math.pi / 2 + i * paso + hueco / 2,
        paso - hueco,
        false,
        l.trazo(
          pintado ? ColoresDeTurno.de(i) : t.border,
          grosor,
        ),
      );
    }

    // El pozo en el centro: las monedas que se juntan y se entregan enteras.
    _pozo(canvas, l, l.centro, l.p(0.115));

    // La gente alrededor, mirando al centro.
    for (var i = 0; i < _cuantos; i++) {
      final angulo = -math.pi / 2 + (i + 0.5) * paso;
      final donde =
          l.centro + Offset(math.cos(angulo), math.sin(angulo)) * radioGente;
      l.persona(donde, l.p(0.115), ColoresDeTurno.de(i));
    }
  }

  void _pozo(Canvas canvas, Lienzo l, Offset centro, double radio) {
    for (var i = 0; i < 3; i++) {
      final y = centro.dy + radio * 0.5 - i * radio * 0.46;
      final ovalo = Rect.fromCenter(
        center: Offset(centro.dx, y),
        width: radio * 2,
        height: radio * 0.78,
      );
      canvas
        ..drawOval(ovalo, l.relleno(i == 2 ? t.accent : t.brand))
        ..drawOval(ovalo, l.trazo(t.brandBg, l.p(0.012)));
    }
  }

  @override
  bool shouldRepaint(RuedaDeGente otro) => otro.t != t;
}
