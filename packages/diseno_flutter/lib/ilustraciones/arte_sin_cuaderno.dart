import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'arte_de_maqueta.dart';

/// Lámina 1 del tour, «El pasanaku de siempre, sin el cuaderno»: tres personas y, al
/// frente, el aporte que juntan entre todas.
class ArteSinCuaderno extends ArteDeMaqueta {
  const ArteSinCuaderno(super.t);

  @override
  void dibujar(Canvas c) {
    c.drawCircle(const Offset(70, 70), 52, relleno(disco));

    // La persona del centro.
    c.drawCircle(const Offset(70, 42), 11, relleno(Paleta.g600));
    c.drawPath(
      Path()
        ..moveTo(52, 74)
        ..cubicTo(52, 65, 60, 59, 70, 59)
        ..cubicTo(80, 59, 88, 65, 88, 74)
        ..close(),
      relleno(Paleta.g600),
    );

    // Las dos de los costados.
    c.drawCircle(const Offset(36, 58), 8.5, relleno(Paleta.g300));
    c.drawPath(
      Path()
        ..moveTo(22, 84)
        ..cubicTo(22, 76.5, 28, 71.5, 36, 71.5)
        ..cubicTo(39, 71.5, 41.7, 72.2, 44, 73.5)
        ..close(),
      relleno(Paleta.g300),
    );
    c.drawCircle(const Offset(104, 58), 8.5, relleno(Paleta.g300));
    c.drawPath(
      Path()
        ..moveTo(118, 84)
        ..cubicTo(118, 76.5, 112, 71.5, 104, 71.5)
        ..cubicTo(101, 71.5, 98.3, 72.2, 96, 73.5)
        ..close(),
      relleno(Paleta.g300),
    );

    // El aporte en común: la caja naranja con su signo más.
    c.drawRRect(
      RRect.fromRectAndRadius(
        const Rect.fromLTWH(46, 92, 48, 30),
        const Radius.circular(7),
      ),
      relleno(Paleta.o500),
    );
    final signo = trazo(Paleta.white, 3.4);
    c
      ..drawLine(const Offset(60, 107), const Offset(80, 107), signo)
      ..drawLine(const Offset(70, 99), const Offset(70, 115), signo);
  }
}
