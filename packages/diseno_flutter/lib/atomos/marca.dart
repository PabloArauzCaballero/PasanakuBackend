import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'pintor_de_marca.dart';

/// El isotipo de AportaYa: dos trazos que se abren como un techo, el arco que los
/// une abajo, y adentro el mismo gesto en naranja.
///
/// Los seis trazos son los de `docs/Views/AportaYa-logo-horizontal.svg`, punto por
/// punto (el `symbol #ay-marca` de la maqueta). Se dibuja, no se importa como imagen,
/// para que siga el tema y escale sin pixelarse.
class Marca extends StatelessWidget {
  const Marca({super.key, this.tamano = 40, this.colorDelTrazo, this.avance = 1});

  final double tamano;

  /// El color de los tres trazos exteriores. Por defecto, el verde de marca del tema.
  /// Sobre relleno verde sólido se le pasa [Tokens.sobreVerdeSolido].
  final Color? colorDelTrazo;

  /// Cuánto del trazo está dibujado, de 0 a 1. Lo usa la apertura para que la marca
  /// se escriba sola.
  final double avance;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      label: 'AportaYa',
      image: true,
      child: SizedBox.square(
        dimension: tamano,
        child: CustomPaint(
          painter: PintorDeMarca(
            trazo: colorDelTrazo ?? t.brand,
            acento: t.accent,
            avance: avance,
          ),
        ),
      ),
    );
  }
}
