import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'arte_de_maqueta.dart';

/// Lámina 4 del tour, «Si alguien no aporta, el grupo no se frena»: el paraguas del
/// fondo de garantía cubriendo la moneda de la cuota.
class ArteFondoDeGarantia extends ArteDeMaqueta {
  const ArteFondoDeGarantia(super.t);

  @override
  void dibujar(Canvas c) {
    c.drawLine(
      const Offset(70, 24),
      const Offset(70, 34),
      trazo(Paleta.g600, 5),
    );

    // La copa del paraguas: el semicírculo relleno y su borde más oscuro.
    final copa = Path()
      ..moveTo(14, 76)
      ..arcToPoint(const Offset(126, 76), radius: const Radius.circular(56));
    c
      ..drawPath(Path.from(copa)..close(), relleno(Paleta.g600))
      ..drawPath(copa, trazo(Paleta.g700, 3, redondo: false));

    // El mango, que termina en gancho.
    c.drawPath(
      Path()
        ..moveTo(70, 76)
        ..lineTo(70, 110)
        ..arcToPoint(
          const Offset(96, 110),
          radius: const Radius.circular(13),
          clockwise: false,
        ),
      trazo(Paleta.g600, 5.5),
    );

    // La moneda de la cuota, con su signo.
    c.drawCircle(const Offset(44, 104), 15, relleno(Paleta.o500));
    final signo = trazo(Paleta.white, 2.8);
    c
      ..drawLine(const Offset(44, 96), const Offset(44, 112), signo)
      ..drawPath(
        Path()
          ..moveTo(39.5, 100.5)
          ..lineTo(46.5, 100.5)
          ..arcToPoint(
            const Offset(46.5, 108.5),
            radius: const Radius.circular(4),
          )
          ..lineTo(39.5, 108.5),
        signo,
      );
  }
}
