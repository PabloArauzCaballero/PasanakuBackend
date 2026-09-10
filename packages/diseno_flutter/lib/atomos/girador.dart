import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Indicador de espera con etiqueta para el lector.
class Girador extends StatelessWidget {
  const Girador({
    super.key,
    this.etiqueta = 'Cargando',
    this.tamano = Espacio.s5,
  });
  final String etiqueta;
  final double tamano;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      label: etiqueta,
      liveRegion: true,
      child: SizedBox(
        width: tamano,
        height: tamano,
        child: CircularProgressIndicator(
          strokeWidth: Borde.foco - Borde.fino,
          color: t.brand,
        ),
      ),
    );
  }
}
