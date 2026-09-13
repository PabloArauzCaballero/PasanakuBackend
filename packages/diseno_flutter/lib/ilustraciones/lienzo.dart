import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// El envoltorio de un dibujo: tamaño cuadrado, semántica propia y el pintor que le
/// toque. Los dibujos del tour salen de `ArteDeMaqueta`.
class Ilustracion extends StatelessWidget {
  const Ilustracion({
    super.key,
    required this.pintor,
    required this.etiqueta,
    this.tamano = 160,
  });

  final CustomPainter Function(Tokens) pintor;
  final String etiqueta;
  final double tamano;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      label: etiqueta,
      image: true,
      child: SizedBox.square(
        dimension: tamano,
        child: CustomPaint(painter: pintor(t)),
      ),
    );
  }
}
