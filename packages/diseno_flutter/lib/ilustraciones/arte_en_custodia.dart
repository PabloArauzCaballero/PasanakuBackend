import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'arte_de_maqueta.dart';

/// Lámina 2 del tour, «Tu plata está en custodia»: un extracto con su comprobante
/// verificado, y el escudo naranja de la custodia en la esquina.
class ArteEnCustodia extends ArteDeMaqueta {
  const ArteEnCustodia(super.t);

  @override
  void dibujar(Canvas c) {
    c.drawRRect(
      RRect.fromRectAndRadius(
        const Rect.fromLTWH(18, 34, 104, 74),
        const Radius.circular(12),
      ),
      relleno(disco),
    );
    c.drawLine(
      const Offset(28, 60),
      const Offset(112, 60),
      trazo(Paleta.g300, 3, redondo: false),
    );

    final renglon = trazo(Paleta.g600, 4);
    c
      ..drawLine(const Offset(36, 74), const Offset(80, 74), renglon)
      ..drawLine(const Offset(36, 86), const Offset(66, 86), renglon);

    // El comprobante: el tilde dentro del círculo.
    c
      ..drawCircle(const Offset(99, 88), 18, relleno(Paleta.g600))
      ..drawPath(
        Path()
          ..moveTo(91, 88.5)
          ..lineTo(96.4, 93.9)
          ..lineTo(108, 82.5),
        trazo(Paleta.white, 4),
      );

    // El escudo de la custodia.
    c.drawPath(
      Path()
        ..moveTo(40, 22)
        ..lineTo(54, 28)
        ..lineTo(54, 37)
        ..cubicTo(54, 44, 48.4, 49.4, 40, 51.4)
        ..cubicTo(31.6, 49.4, 26, 44, 26, 37)
        ..lineTo(26, 28)
        ..close(),
      relleno(Paleta.o500),
    );
  }
}
