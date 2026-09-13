import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'lienzo.dart';

/// **El turno no se arregla**: la ruleta sorteada, con el sello que deja comprobarlo.
///
/// La aguja dice «salió sorteado»; el escudo dice «y podés comprobarlo», que es la
/// mitad que importa: un sorteo sin verificación es una promesa, no una garantía.
///
/// Los gajos alternan dos tonos para que la rueda se lea como rueda —con todos
/// blancos parecía un plato vacío— y el que salió es el naranja de acción, el único
/// color fuerte del dibujo. El ojo va ahí solo.
class SorteoLimpio extends CustomPainter {
  const SorteoLimpio(this.t);
  final Tokens t;

  static const _cuantos = 8;
  static const _ganador = 6;

  @override
  void paint(Canvas canvas, Size size) {
    final l = Lienzo(canvas, size, t)..escenario();
    final centro = l.centro - Offset(0, l.p(0.07));
    final radio = l.p(0.255);
    final paso = math.pi * 2 / _cuantos;
    final rect = Rect.fromCircle(center: centro, radius: radio);

    for (var i = 0; i < _cuantos; i++) {
      final desde = -math.pi / 2 + i * paso;
      // La alternancia sale del verde de marca con transparencia y no de
      // `surface`/`surface2`: en oscuro esos dos son casi el mismo negro y la rueda
      // se aplanaba en un disco. Así contrasta en los dos temas.
      final color = i == _ganador
          ? t.accent
          : (i.isEven ? t.surface : t.brand.withValues(alpha: 0.16));
      canvas
        ..drawArc(rect, desde, paso, true, l.relleno(color))
        ..drawArc(rect, desde, paso, true, l.trazo(t.border, l.p(0.014)));
    }
    canvas.drawCircle(centro, radio, l.trazo(t.brand, l.p(0.026)));

    _aguja(canvas, l, centro, radio, -math.pi / 2 + (_ganador + 0.5) * paso);
    _sello(canvas, l, l.centro + Offset(0, l.p(0.36)), l.p(0.105));
  }

  /// Una cuña, no una raya: a este tamaño una línea de dos píxeles desaparece, y la
  /// aguja es lo que dice cuál turno salió.
  void _aguja(
    Canvas canvas,
    Lienzo l,
    Offset centro,
    double radio,
    double angulo,
  ) {
    final punta = centro + Offset(math.cos(angulo), math.sin(angulo)) * radio;
    final ancho = l.p(0.045);
    final perpendicular = angulo + math.pi / 2;
    final costado = Offset(math.cos(perpendicular), math.sin(perpendicular));
    final camino = Path()
      ..moveTo(punta.dx, punta.dy)
      ..lineTo(
        centro.dx + costado.dx * ancho,
        centro.dy + costado.dy * ancho,
      )
      ..lineTo(
        centro.dx - costado.dx * ancho,
        centro.dy - costado.dy * ancho,
      )
      ..close();
    canvas
      // El halo despega la aguja del gajo naranja: sin él, los dos naranjas se
      // fundían en una sola mancha y no se entendía que algo estaba apuntando.
      ..drawPath(camino, l.trazo(t.surface, l.p(0.038)))
      ..drawPath(camino, l.relleno(t.brandInk))
      ..drawCircle(centro, l.p(0.050), l.relleno(t.brandInk))
      ..drawCircle(centro, l.p(0.021), l.relleno(t.surface));
  }

  /// El escudo con el tilde: el sorteo quedó registrado y cualquiera del grupo puede
  /// volver a comprobarlo.
  void _sello(Canvas canvas, Lienzo l, Offset centro, double alto) {
    final ancho = alto * 0.86;
    final camino = Path()
      ..moveTo(centro.dx, centro.dy - alto)
      ..lineTo(centro.dx + ancho, centro.dy - alto * 0.52)
      ..lineTo(centro.dx + ancho, centro.dy + alto * 0.10)
      ..quadraticBezierTo(
        centro.dx + ancho,
        centro.dy + alto * 0.80,
        centro.dx,
        centro.dy + alto,
      )
      ..quadraticBezierTo(
        centro.dx - ancho,
        centro.dy + alto * 0.80,
        centro.dx - ancho,
        centro.dy + alto * 0.10,
      )
      ..lineTo(centro.dx - ancho, centro.dy - alto * 0.52)
      ..close();
    canvas
      ..drawPath(camino, l.relleno(t.brand))
      ..drawPath(
        Path()
          ..moveTo(centro.dx - alto * 0.36, centro.dy)
          ..lineTo(centro.dx - alto * 0.08, centro.dy + alto * 0.26)
          ..lineTo(centro.dx + alto * 0.40, centro.dy - alto * 0.30),
        l.trazo(t.sobreVerdeSolido, l.p(0.030)),
      );
  }

  @override
  bool shouldRepaint(SorteoLimpio otro) => otro.t != t;
}
