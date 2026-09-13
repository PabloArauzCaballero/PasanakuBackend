import 'package:flutter/material.dart';

/// El aire detrás de las pantallas de entrada: dos halos difusos y el mismo gesto del
/// isotipo repetido en grande, apenas visible.
///
/// No es decoración al azar. Los dos arcos son las curvas exteriores de la marca
/// (`M92 30 C74 66 52 108 40 168` y su espejo), estiradas a toda la pantalla: el fondo
/// dice la misma forma que el logo, tan bajo que se nota como textura y no como
/// dibujo. Un fondo plano es lo que hace que una pantalla con tres elementos se vea
/// vacía en vez de tranquila.
class PintorDeAura extends CustomPainter {
  const PintorDeAura({
    required this.halo,
    required this.brillo,
    required this.linea,
  });

  /// El halo grande de arriba, del color de marca.
  final Color halo;

  /// El halo chico de abajo, del color de acento.
  final Color brillo;

  /// El color de los dos arcos.
  final Color linea;

  @override
  void paint(Canvas lienzo, Size medida) {
    final ancho = medida.width;
    final alto = medida.height;

    void difuso(Offset centro, double radio, Color color) {
      final area = Rect.fromCircle(center: centro, radius: radio);
      lienzo.drawRect(
        area,
        Paint()
          ..shader = RadialGradient(
            colors: [color, color.withValues(alpha: 0)],
          ).createShader(area),
      );
    }

    difuso(Offset(ancho * 0.5, alto * 0.16), ancho * 0.85, halo);
    difuso(Offset(ancho * 0.92, alto * 0.82), ancho * 0.6, brillo);

    // Las dos curvas del isotipo, a escala de pantalla. Salen por arriba y por abajo:
    // se ve el tramo del medio, que es donde la curva se abre.
    //
    // Van **desenfocadas**. Con el trazo nítido se leen como dos franjas grises
    // corriendo por la pantalla —parece un defecto de render, no un fondo—; con el
    // desenfoque del ancho del propio trazo son dos haces de luz y ya nadie las mira.
    final pincel = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = ancho * 0.075
      ..strokeCap = StrokeCap.round
      ..maskFilter = MaskFilter.blur(BlurStyle.normal, ancho * 0.06)
      ..color = linea;
    final x = ancho * 0.5;
    final y = -alto * 0.1;
    final h = alto * 1.35;
    final w = ancho * 0.95;
    lienzo
      ..drawPath(
        Path()
          ..moveTo(x - w * 0.06, y)
          ..cubicTo(
            x - w * 0.19,
            y + h * 0.26,
            x - w * 0.35,
            y + h * 0.57,
            x - w * 0.43,
            y + h,
          ),
        pincel,
      )
      ..drawPath(
        Path()
          ..moveTo(x + w * 0.06, y)
          ..cubicTo(
            x + w * 0.19,
            y + h * 0.26,
            x + w * 0.35,
            y + h * 0.57,
            x + w * 0.43,
            y + h,
          ),
        pincel,
      );
  }

  @override
  bool shouldRepaint(PintorDeAura otro) =>
      otro.halo != halo || otro.brillo != brillo || otro.linea != linea;
}
