import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// El marco de encuadre sobre el visor, igual que Atlas: esquinas de carnet (ID-1,
/// proporción 85.6×54) para el documento, cuatro esquinas sueltas para el rostro.
class GuiaDeEncuadre extends StatelessWidget {
  const GuiaDeEncuadre({
    super.key,
    required this.esDocumento,
    this.color = Paleta.white,
    this.grosor = 4,
  });
  final bool esDocumento;

  /// Blanco sobre el visor de la cámara; el color de marca sobre la tarjeta vacía.
  final Color color;
  final double grosor;

  @override
  Widget build(BuildContext context) {
    return IgnorePointer(
      child: CustomPaint(
        size: Size.infinite,
        painter: _PintorDeGuia(
          esDocumento: esDocumento,
          color: color,
          grosor: grosor,
        ),
      ),
    );
  }
}

class _PintorDeGuia extends CustomPainter {
  _PintorDeGuia({
    required this.esDocumento,
    required this.color,
    required this.grosor,
  });
  final bool esDocumento;
  final Color color;
  final double grosor;

  static const _proporcionCarnet = 85.6 / 54;
  static const _largoEsquina = 28.0;

  @override
  void paint(Canvas canvas, Size size) {
    final trazo = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = grosor
      ..strokeCap = StrokeCap.round;

    final Rect marco;
    if (esDocumento) {
      final ancho = size.width * 0.86;
      final alto = ancho / _proporcionCarnet;
      marco = Rect.fromCenter(
        center: size.center(Offset.zero),
        width: ancho,
        height: alto,
      );
    } else {
      // Cuatro esquinas al 12 % del borde, igual que Atlas para la selfie y los
      // perfiles: no hay óvalo, hay un rectángulo amplio centrado en el rostro.
      final margen = size.shortestSide * 0.12;
      marco = Rect.fromLTRB(
        margen,
        margen,
        size.width - margen,
        size.height - margen,
      );
    }

    void esquina(Offset p, double dx, double dy) {
      canvas.drawLine(p, p.translate(dx, 0), trazo);
      canvas.drawLine(p, p.translate(0, dy), trazo);
    }

    esquina(marco.topLeft, _largoEsquina, _largoEsquina);
    esquina(marco.topRight, -_largoEsquina, _largoEsquina);
    esquina(marco.bottomLeft, _largoEsquina, -_largoEsquina);
    esquina(marco.bottomRight, -_largoEsquina, -_largoEsquina);
  }

  @override
  bool shouldRepaint(_PintorDeGuia oldDelegate) =>
      oldDelegate.esDocumento != esDocumento ||
      oldDelegate.color != color ||
      oldDelegate.grosor != grosor;
}
