import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'lienzo.dart';

/// **La plata no la tenemos nosotros**: el banco la custodia, aparte.
///
/// El banco a la izquierda con el escudo delante, una línea punteada que no se cruza,
/// y del otro lado el edificio chico de la empresa —vacío, a propósito—. El dibujo
/// dice lo que la frase promete: son dos bolsillos distintos, y el de la persona está
/// del lado del banco.
///
/// Es la ilustración que más pesa del tour, porque responde la pregunta que decide si
/// alguien abre la cuenta o no.
class PlataEnCustodia extends CustomPainter {
  const PlataEnCustodia(this.t);
  final Tokens t;

  @override
  void paint(Canvas canvas, Size size) {
    final l = Lienzo(canvas, size, t)..escenario();
    final base = l.centro.dy + l.p(0.21);
    final ejeBanco = l.centro.dx - l.p(0.13);

    _banco(canvas, l, centro: Offset(ejeBanco, base), ancho: l.p(0.23));
    // La línea no es un muro: es una regla. Pero una que no se cruza.
    _separador(canvas, l, l.centro.dx + l.p(0.155), base);
    _empresa(canvas, l, centro: Offset(l.centro.dx + l.p(0.305), base));
    // El escudo va DELANTE del banco y pisando su base: la plata está adentro y
    // protegida, no apoyada encima del techo.
    _escudoConMoneda(canvas, l, Offset(ejeBanco, base - l.p(0.04)), l.p(0.125));
  }

  void _banco(
    Canvas canvas,
    Lienzo l, {
    required Offset centro,
    required double ancho,
  }) {
    final grosor = l.p(0.026);
    final alto = l.p(0.19);
    final techo = Path()
      ..moveTo(centro.dx - ancho * 1.22, centro.dy - alto)
      ..lineTo(centro.dx, centro.dy - alto - l.p(0.115))
      ..lineTo(centro.dx + ancho * 1.22, centro.dy - alto)
      ..close();
    canvas.drawPath(techo, l.relleno(t.brand));
    for (var i = -1; i <= 1; i++) {
      final x = centro.dx + i * ancho * 0.66;
      canvas.drawLine(
        Offset(x, centro.dy - alto + l.p(0.012)),
        Offset(x, centro.dy - grosor),
        l.trazo(t.brand, grosor * 1.3),
      );
    }
    canvas.drawLine(
      Offset(centro.dx - ancho * 1.22, centro.dy),
      Offset(centro.dx + ancho * 1.22, centro.dy),
      l.trazo(t.brand, grosor * 1.8),
    );
  }

  void _separador(Canvas canvas, Lienzo l, double x, double base) {
    final pincel = l.trazo(t.text3, l.p(0.020));
    final desde = base - l.p(0.34);
    for (var y = desde; y < base + l.p(0.02); y += l.p(0.060)) {
      canvas.drawLine(Offset(x, y), Offset(x, y + l.p(0.032)), pincel);
    }
  }

  /// La empresa: un edificio chico y **vacío**, del otro lado de la línea. Sin moneda
  /// adentro — ese es el punto del dibujo.
  void _empresa(Canvas canvas, Lienzo l, {required Offset centro}) {
    final ancho = l.p(0.105);
    final alto = l.p(0.145);
    final rect = Rect.fromLTRB(
      centro.dx - ancho,
      centro.dy - alto,
      centro.dx + ancho,
      centro.dy,
    );
    canvas
      ..drawRRect(
        RRect.fromRectAndRadius(rect, Radius.circular(l.p(0.018))),
        l.trazo(t.text3, l.p(0.020)),
      )
      // Un techo mínimo: sin él la caja se leía como un documento, no como el
      // edificio de la empresa.
      ..drawLine(
        Offset(rect.left - l.p(0.022), rect.top),
        Offset(rect.right + l.p(0.022), rect.top),
        l.trazo(t.text3, l.p(0.024)),
      );
    // Dos ventanas apagadas, para que se lea «edificio» y no «caja».
    for (var i = 0; i < 2; i++) {
      canvas.drawLine(
        Offset(rect.left + l.p(0.030), rect.top + alto * (0.32 + i * 0.34)),
        Offset(rect.right - l.p(0.030), rect.top + alto * (0.32 + i * 0.34)),
        l.trazo(t.text3, l.p(0.014)),
      );
    }
  }

  void _escudoConMoneda(Canvas canvas, Lienzo l, Offset centro, double alto) {
    final ancho = alto * 0.84;
    final camino = Path()
      ..moveTo(centro.dx, centro.dy - alto)
      ..lineTo(centro.dx + ancho, centro.dy - alto * 0.50)
      ..lineTo(centro.dx + ancho, centro.dy + alto * 0.12)
      ..quadraticBezierTo(
        centro.dx + ancho,
        centro.dy + alto * 0.82,
        centro.dx,
        centro.dy + alto,
      )
      ..quadraticBezierTo(
        centro.dx - ancho,
        centro.dy + alto * 0.82,
        centro.dx - ancho,
        centro.dy + alto * 0.12,
      )
      ..lineTo(centro.dx - ancho, centro.dy - alto * 0.50)
      ..close();
    canvas
      // El halo del fondo despega el escudo del banco que tiene detrás.
      ..drawPath(camino, l.trazo(t.brandBg, l.p(0.075)))
      ..drawPath(camino, l.relleno(t.accent))
      ..drawCircle(centro, alto * 0.38, l.relleno(t.accentInk));
  }

  @override
  bool shouldRepaint(PlataEnCustodia otro) => otro.t != t;
}
