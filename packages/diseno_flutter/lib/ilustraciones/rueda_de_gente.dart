import 'dart:math' as math;
import 'dart:ui' as ui;

import 'package:flutter/material.dart';

import '../atomos/colores_de_turno.dart';
import '../tokens/tokens.dart';
import 'lienzo.dart';

/// **Qué es un pasanaku**: una rueda de personas que se turnan el pozo.
///
/// Ocho personas alrededor de un anillo segmentado, cada una del color de su turno, y
/// en el centro el pozo de monedas. Es la definición del producto en un dibujo: no
/// hace falta leer nada para entender que se trata de un grupo que da la vuelta.
class RuedaDeGente extends CustomPainter {
  const RuedaDeGente(this.t);
  final Tokens t;

  static const _cuantos = 8;
  static const _cobrados = 3;

  @override
  void paint(Canvas canvas, Size size) {
    final l = Lienzo(canvas, size, t)..escenario();
    final radioAnillo = l.p(0.255);
    final radioGente = l.p(0.395);
    final grosor = l.p(0.062);
    final rect = Rect.fromCircle(center: l.centro, radius: radioAnillo);
    final paso = math.pi * 2 / _cuantos;
    const hueco = 0.11;

    // El canal del anillo, hundido: le da al aro un lugar donde apoyarse.
    canvas.drawCircle(
      l.centro,
      radioAnillo,
      l.trazo(t.brandInk.withValues(alpha: 0.07), grosor * 1.25),
    );

    for (var i = 0; i < _cuantos; i++) {
      final cobrado = i < _cobrados;
      final desde = -math.pi / 2 + i * paso + hueco / 2;
      final color = cobrado ? ColoresDeTurno.de(i) : t.border;
      final pincel = l.trazo(color, grosor)
        ..shader = cobrado
            ? ui.Gradient.linear(
                rect.topLeft,
                rect.bottomRight,
                [Color.lerp(color, Paleta.white, 0.28)!, color],
              )
            : null;
      canvas.drawArc(rect, desde, paso - hueco, false, pincel);
    }

    _pozo(canvas, l, l.centro, l.p(0.108));

    for (var i = 0; i < _cuantos; i++) {
      final angulo = -math.pi / 2 + (i + 0.5) * paso;
      final donde =
          l.centro + Offset(math.cos(angulo), math.sin(angulo)) * radioGente;
      l.persona(donde, l.p(0.118), ColoresDeTurno.de(i));
    }
  }

  /// El pozo: tres monedas apiladas, con canto y brillo. La de arriba es naranja
  /// porque es la que se lleva quien cobra este turno.
  void _pozo(Canvas canvas, Lienzo l, Offset centro, double radio) {
    final base = Path()
      ..addOval(
        Rect.fromCenter(
          center: Offset(centro.dx, centro.dy + radio * 0.52),
          width: radio * 2.1,
          height: radio * 0.8,
        ),
      );
    l.sombra(base);

    for (var i = 0; i < 3; i++) {
      final y = centro.dy + radio * 0.46 - i * radio * 0.44;
      final color = i == 2 ? t.accent : t.brand;
      final cara = Rect.fromCenter(
        center: Offset(centro.dx, y),
        width: radio * 2,
        height: radio * 0.76,
      );
      final canto = Rect.fromCenter(
        center: Offset(centro.dx, y + radio * 0.09),
        width: radio * 2,
        height: radio * 0.76,
      );
      canvas
        ..drawOval(canto, l.relleno(Color.lerp(color, Paleta.g900, 0.28)!))
        ..drawOval(cara, l.volumen(color, cara, fuerza: 0.30));
      if (i == 2) {
        l.brillo(
          Offset(centro.dx - radio * 0.35, y - radio * 0.12),
          radio * 0.55,
          fuerza: 0.45,
        );
      }
    }
  }

  @override
  bool shouldRepaint(RuedaDeGente otro) => otro.t != t;
}
