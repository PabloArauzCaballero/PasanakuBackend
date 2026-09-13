import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'arte_de_maqueta.dart';

/// Lámina 3 del tour, «El turno se sortea a la vista de todos»: la rueda del sorteo
/// en tres gajos y la marca naranja arriba, que dice desde dónde se cuenta.
///
/// Los arcos del SVG (`a46 46 0 0 1 …`) son `arcToPoint` con `clockwise` por
/// omisión: el `sweep-flag = 1` de SVG y el sentido horario de Flutter coinciden
/// porque los dos lienzos tienen el eje Y hacia abajo.
class ArteSorteoALaVista extends ArteDeMaqueta {
  const ArteSorteoALaVista(super.t);

  static const _radio = Radius.circular(46);
  static const _centro = Offset(70, 72);

  @override
  void dibujar(Canvas c) {
    c.drawCircle(_centro, 46, relleno(disco));

    c
      ..drawPath(
        Path()
          ..moveTo(70, 26)
          ..arcToPoint(const Offset(109.8, 48.9), radius: _radio)
          ..lineTo(70, 72)
          ..close(),
        relleno(Paleta.g600),
      )
      ..drawPath(
        Path()
          ..moveTo(109.8, 48.9)
          ..arcToPoint(const Offset(70, 118), radius: _radio)
          ..lineTo(70, 72)
          ..close(),
        relleno(Paleta.g300),
      )
      ..drawPath(
        Path()
          ..moveTo(70, 118)
          ..arcToPoint(const Offset(30.2, 48.9), radius: _radio)
          ..lineTo(70, 72)
          ..close(),
        relleno(Paleta.g500),
      );

    c.drawCircle(_centro, 13, relleno(crema));

    // La marca de inicio del sorteo.
    c.drawPath(
      Path()
        ..moveTo(70, 12)
        ..lineTo(62, 26)
        ..lineTo(78, 26)
        ..close(),
      relleno(Paleta.o500),
    );
  }
}
