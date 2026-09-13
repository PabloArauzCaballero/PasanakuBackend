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
    final base = l.centro.dy + l.p(0.20);
    final ejeBanco = l.centro.dx - l.p(0.135);

    _piso(canvas, l, base);
    _banco(canvas, l, centro: Offset(ejeBanco, base), ancho: l.p(0.215));
    // La línea no es un muro: es una regla. Pero una que no se cruza.
    _separador(canvas, l, l.centro.dx + l.p(0.155), base);
    _empresa(canvas, l, centro: Offset(l.centro.dx + l.p(0.305), base));
    // El escudo va DELANTE del banco y pisando su base: la plata está adentro y
    // protegida, no apoyada encima del techo.
    _escudo(canvas, l, Offset(ejeBanco, base - l.p(0.055)), l.p(0.128));
  }

  /// La línea de suelo: apoya todo el conjunto en un mismo plano.
  void _piso(Canvas canvas, Lienzo l, double base) {
    canvas.drawLine(
      Offset(l.centro.dx - l.p(0.40), base),
      Offset(l.centro.dx + l.p(0.42), base),
      l.trazo(t.brandInk.withValues(alpha: 0.12), l.p(0.014)),
    );
  }

  void _banco(
    Canvas canvas,
    Lienzo l, {
    required Offset centro,
    required double ancho,
  }) {
    final grosor = l.p(0.028);
    final alto = l.p(0.185);
    final techo = Path()
      ..moveTo(centro.dx - ancho * 1.26, centro.dy - alto)
      ..lineTo(centro.dx, centro.dy - alto - l.p(0.118))
      ..lineTo(centro.dx + ancho * 1.26, centro.dy - alto)
      ..close();
    final cuerpo = Rect.fromLTRB(
      centro.dx - ancho * 1.26,
      centro.dy - alto,
      centro.dx + ancho * 1.26,
      centro.dy,
    );

    l.sombra(
      Path()
        ..addRect(Rect.fromLTRB(cuerpo.left, centro.dy - l.p(0.02), cuerpo.right, centro.dy)),
      desplazamiento: 0.016,
      difuso: 0.026,
    );

    // Las columnas: tres, con volumen, sobre el hueco oscuro del interior.
    canvas.drawRect(cuerpo, l.relleno(t.brandInk.withValues(alpha: 0.10)));
    for (var i = -1; i <= 1; i++) {
      final x = centro.dx + i * ancho * 0.68;
      final columna = Rect.fromCenter(
        center: Offset(x, centro.dy - alto / 2),
        width: grosor * 1.5,
        height: alto - grosor,
      );
      canvas.drawRect(columna, l.volumen(t.brand, columna, fuerza: 0.26));
    }

    canvas
      ..drawPath(techo, l.volumen(t.brand, techo.getBounds(), fuerza: 0.30))
      ..drawRect(
        Rect.fromLTRB(cuerpo.left, centro.dy - grosor * 1.1, cuerpo.right, centro.dy),
        l.volumen(t.brand, cuerpo, fuerza: 0.22),
      );
    l.brillo(
      Offset(centro.dx - ancho * 0.5, centro.dy - alto - l.p(0.04)),
      ancho * 0.6,
      fuerza: 0.28,
    );
  }

  void _separador(Canvas canvas, Lienzo l, double x, double base) {
    final pincel = l.trazo(t.text3.withValues(alpha: 0.75), l.p(0.020));
    for (var y = base - l.p(0.36); y < base + l.p(0.01); y += l.p(0.062)) {
      canvas.drawLine(Offset(x, y), Offset(x, y + l.p(0.032)), pincel);
    }
  }

  /// La empresa: un edificio chico y **vacío**, del otro lado de la línea. Sin moneda
  /// adentro — ese es el punto del dibujo.
  void _empresa(Canvas canvas, Lienzo l, {required Offset centro}) {
    final ancho = l.p(0.100);
    final alto = l.p(0.150);
    final rect = Rect.fromLTRB(
      centro.dx - ancho,
      centro.dy - alto,
      centro.dx + ancho,
      centro.dy,
    );
    canvas
      ..drawRRect(
        RRect.fromRectAndRadius(rect, Radius.circular(l.p(0.016))),
        l.relleno(t.text3.withValues(alpha: 0.07)),
      )
      ..drawRRect(
        RRect.fromRectAndRadius(rect, Radius.circular(l.p(0.016))),
        l.trazo(t.text3, l.p(0.019)),
      )
      // Un techo mínimo: sin él la caja se leía como un documento.
      ..drawLine(
        Offset(rect.left - l.p(0.024), rect.top),
        Offset(rect.right + l.p(0.024), rect.top),
        l.trazo(t.text3, l.p(0.024)),
      );
    for (var i = 0; i < 2; i++) {
      final y = rect.top + alto * (0.34 + i * 0.32);
      canvas.drawLine(
        Offset(rect.left + l.p(0.028), y),
        Offset(rect.right - l.p(0.028), y),
        l.trazo(t.text3.withValues(alpha: 0.65), l.p(0.013)),
      );
    }
  }

  void _escudo(Canvas canvas, Lienzo l, Offset centro, double alto) {
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
      ..drawPath(camino, l.trazo(t.brandBg, l.p(0.080)));
    l.sombra(camino, desplazamiento: 0.020, difuso: 0.026);
    final moneda = Rect.fromCircle(center: centro, radius: alto * 0.38);
    canvas
      ..drawPath(camino, l.volumen(t.accent, camino.getBounds(), fuerza: 0.30))
      ..drawOval(moneda, l.volumen(t.accentInk, moneda, fuerza: 0.45));
    l.brillo(centro - Offset(ancho * 0.34, alto * 0.44), ancho * 0.58);
  }

  @override
  bool shouldRepaint(PlataEnCustodia otro) => otro.t != t;
}
