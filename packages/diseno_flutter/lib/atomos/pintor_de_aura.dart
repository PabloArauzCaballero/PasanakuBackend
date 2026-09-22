import 'package:flutter/material.dart';

/// El aire detrás de las pantallas de entrada: tres luces, los arcos de la marca y un
/// viñeteo.
///
/// Un fondo de un solo color hace que cualquier pantalla con tres elementos se vea
/// vacía en vez de tranquila. Acá hay una escena de luz, en este orden:
///
/// 1. **La luz de arriba**, pálida y ancha, como la que entra por una ventana.
/// 2. **El verde de marca**, corrido a la izquierda, que tiñe sin teñir el centro.
/// 3. **El naranja**, abajo a la derecha y más chico: el contrapunto cálido.
/// 4. **Los dos arcos del isotipo** a escala de pantalla y desenfocados. Nítidos se
///    leen como franjas grises —parece un defecto de render—; desenfocados son luz.
/// 5. **El viñeteo**, que apaga las esquinas. Es lo que en una foto separa el sujeto
///    del fondo, y acá hace que el contenido del medio parezca estar más cerca.
///
/// Las cinco capas son degradados: se dibujan nítidas a cualquier densidad de
/// pantalla y no hay ninguna imagen que se pixele.
class PintorDeAura extends CustomPainter {
  const PintorDeAura({
    required this.luz,
    required this.halo,
    required this.brillo,
    required this.linea,
    required this.vineta,
  });

  /// La luz pálida de arriba.
  final Color luz;

  /// El verde de marca de la izquierda.
  final Color halo;

  /// El naranja de abajo a la derecha.
  final Color brillo;

  /// El color de los dos arcos.
  final Color linea;

  /// El color con el que se apagan las esquinas.
  final Color vineta;

  @override
  void paint(Canvas lienzo, Size medida) {
    final ancho = medida.width;
    final alto = medida.height;
    final todo = Offset.zero & medida;

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

    difuso(Offset(ancho * 0.5, -alto * 0.04), ancho * 1.05, luz);
    difuso(Offset(ancho * 0.08, alto * 0.3), ancho * 0.9, halo);
    difuso(Offset(ancho * 0.98, alto * 0.82), ancho * 0.7, brillo);

    _arcos(lienzo, ancho, alto);

    lienzo.drawRect(
      todo,
      Paint()
        ..shader = RadialGradient(
          radius: 0.95,
          colors: [vineta.withValues(alpha: 0), vineta],
          stops: const [0.5, 1],
        ).createShader(todo),
    );
  }

  /// Las dos curvas exteriores del isotipo (`M92 30 C74 66 52 108 40 168` y su
  /// espejo), estiradas a toda la pantalla y desenfocadas: el fondo dice la misma
  /// forma que el logo, tan bajo que se nota como textura y no como dibujo.
  void _arcos(Canvas lienzo, double ancho, double alto) {
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
    for (final lado in [-1.0, 1.0]) {
      lienzo.drawPath(
        Path()
          ..moveTo(x + lado * w * 0.06, y)
          ..cubicTo(
            x + lado * w * 0.19,
            y + h * 0.26,
            x + lado * w * 0.35,
            y + h * 0.57,
            x + lado * w * 0.43,
            y + h,
          ),
        pincel,
      );
    }
  }

  @override
  bool shouldRepaint(PintorDeAura otro) =>
      otro.luz != luz ||
      otro.halo != halo ||
      otro.brillo != brillo ||
      otro.linea != linea ||
      otro.vineta != vineta;
}
