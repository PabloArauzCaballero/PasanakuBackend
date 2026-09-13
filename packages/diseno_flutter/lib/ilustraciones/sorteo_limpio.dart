import 'dart:math' as math;
import 'dart:ui' as ui;

import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'lienzo.dart';

/// **El turno no se arregla**: la ruleta sorteada, con el sello que deja comprobarlo.
///
/// La aguja dice «salió sorteado»; el escudo dice «y podés comprobarlo», que es la
/// mitad que importa: un sorteo sin verificación es una promesa, no una garantía.
///
/// Los gajos alternan con el verde de marca al 16 % —y no `surface`/`surface2`, que en
/// tema oscuro son casi el mismo negro y aplanaban la rueda en un disco—. El gajo que
/// salió es el único naranja del dibujo: el ojo va ahí solo.
class SorteoLimpio extends CustomPainter {
  const SorteoLimpio(this.t);
  final Tokens t;

  static const _cuantos = 8;
  static const _ganador = 6;

  @override
  void paint(Canvas canvas, Size size) {
    final l = Lienzo(canvas, size, t)..escenario();
    final centro = l.centro - Offset(0, l.p(0.075));
    final radio = l.p(0.250);
    final paso = math.pi * 2 / _cuantos;
    final rect = Rect.fromCircle(center: centro, radius: radio);

    l.sombra(Path()..addOval(rect), desplazamiento: 0.030, difuso: 0.034);

    for (var i = 0; i < _cuantos; i++) {
      final desde = -math.pi / 2 + i * paso;
      final ganador = i == _ganador;
      final color = ganador
          ? t.accent
          : (i.isEven ? t.surface : Color.lerp(t.surface, t.brand, 0.16)!);
      canvas
        ..drawArc(
          rect,
          desde,
          paso,
          true,
          l.volumen(color, rect, fuerza: ganador ? 0.26 : 0.10),
        )
        ..drawArc(
          rect,
          desde,
          paso,
          true,
          l.trazo(t.border.withValues(alpha: 0.7), l.p(0.012)),
        );
    }

    // El aro exterior, con luz arriba: convierte el círculo en un objeto.
    canvas.drawCircle(
      centro,
      radio,
      l.trazo(t.brand, l.p(0.030))
        ..shader = ui.Gradient.linear(
          rect.topCenter,
          rect.bottomCenter,
          [Color.lerp(t.brand, Paleta.white, 0.35)!, t.brandInk],
        ),
    );
    l.brillo(
      centro - Offset(radio * 0.36, radio * 0.44),
      radio * 0.55,
      fuerza: 0.30,
    );

    _aguja(canvas, l, centro, radio, -math.pi / 2 + (_ganador + 0.5) * paso);
    _sello(canvas, l, l.centro + Offset(0, l.p(0.365)), l.p(0.108));
  }

  /// Una cuña, no una raya: a este tamaño una línea de dos píxeles desaparece, y la
  /// aguja es lo que dice cuál turno salió. Lleva halo claro porque sin él los dos
  /// naranjas —aguja y gajo— se fundían en una sola mancha.
  void _aguja(
    Canvas canvas,
    Lienzo l,
    Offset centro,
    double radio,
    double angulo,
  ) {
    final punta =
        centro + Offset(math.cos(angulo), math.sin(angulo)) * radio * 0.94;
    final ancho = l.p(0.042);
    final perpendicular = angulo + math.pi / 2;
    final costado = Offset(math.cos(perpendicular), math.sin(perpendicular));
    final camino = Path()
      ..moveTo(punta.dx, punta.dy)
      ..lineTo(centro.dx + costado.dx * ancho, centro.dy + costado.dy * ancho)
      ..lineTo(centro.dx - costado.dx * ancho, centro.dy - costado.dy * ancho)
      ..close();
    canvas
      ..drawPath(camino, l.trazo(t.surface, l.p(0.040)))
      ..drawPath(camino, l.volumen(t.brandInk, camino.getBounds(), fuerza: 0.35))
      ..drawCircle(
        centro,
        l.p(0.052),
        l.volumen(
          t.brandInk,
          Rect.fromCircle(center: centro, radius: l.p(0.052)),
          fuerza: 0.35,
        ),
      )
      ..drawCircle(centro, l.p(0.020), l.relleno(t.surface));
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
    l.sombra(camino, desplazamiento: 0.018, difuso: 0.024);
    canvas
      ..drawPath(camino, l.volumen(t.brand, camino.getBounds(), fuerza: 0.30))
      ..drawPath(
        Path()
          ..moveTo(centro.dx - alto * 0.36, centro.dy)
          ..lineTo(centro.dx - alto * 0.08, centro.dy + alto * 0.26)
          ..lineTo(centro.dx + alto * 0.40, centro.dy - alto * 0.30),
        l.trazo(t.sobreVerdeSolido, l.p(0.028)),
      );
    l.brillo(
      centro - Offset(ancho * 0.35, alto * 0.42),
      ancho * 0.55,
      fuerza: 0.30,
    );
  }

  @override
  bool shouldRepaint(SorteoLimpio otro) => otro.t != t;
}
