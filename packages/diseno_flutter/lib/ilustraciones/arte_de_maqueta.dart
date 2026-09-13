import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// La base de los dibujos del tour: el lienzo de 140×140 de la maqueta, escalado y
/// centrado al tamaño que toque.
///
/// **No son una reinterpretación.** Cada círculo y cada camino sale de
/// `docs/Views/AportaYa-Maqueta.html` (`const TOUR`, atributo `art`), traducido de
/// SVG a `Path` coordenada por coordenada. La maqueta es el criterio de aceptación
/// visual (skill `disenar-frontend`): lo que se aprobó ahí es lo que se dibuja acá.
///
/// Los colores son los de la paleta con su nombre, salvo dos que se leen del tema
/// para que el dibujo no quede como un parche claro sobre el fondo oscuro: el disco
/// de fondo (`g100` en claro) y el centro crema.
abstract class ArteDeMaqueta extends CustomPainter {
  const ArteDeMaqueta(this.t);
  final Tokens t;

  static const _lienzo = 140.0;

  /// Dibuja en coordenadas de la maqueta, de 0 a 140.
  void dibujar(Canvas c);

  Color get disco => t.brandBg;
  Color get crema => t.bg;

  Paint relleno(Color color) => Paint()..color = color;

  Paint trazo(Color color, double grosor, {bool redondo = true}) => Paint()
    ..color = color
    ..style = PaintingStyle.stroke
    ..strokeWidth = grosor
    ..strokeCap = redondo ? StrokeCap.round : StrokeCap.butt
    ..strokeJoin = StrokeJoin.round;

  @override
  void paint(Canvas canvas, Size size) {
    final escala = size.shortestSide / _lienzo;
    canvas
      ..save()
      ..translate(
        (size.width - _lienzo * escala) / 2,
        (size.height - _lienzo * escala) / 2,
      )
      ..scale(escala);
    dibujar(canvas);
    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant ArteDeMaqueta otro) => otro.t != t;
}
