import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Bloques grises del tamaño del contenido que viene: sin salto de layout.
class Esqueleto extends StatelessWidget {
  const Esqueleto({super.key, this.filas = 3, this.alto = Espacio.s5});
  final int filas;
  final double alto;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final anchos = [0.6, 0.9, 0.4, 0.75, 0.5];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        for (var i = 0; i < filas; i += 1)
          Padding(
            padding: const EdgeInsets.only(bottom: Espacio.s3),
            child: FractionallySizedBox(
              widthFactor: anchos[i % anchos.length],
              child: Container(
                height: alto,
                decoration: BoxDecoration(
                  color: t.surface2,
                  borderRadius: BorderRadius.circular(Radios.sm),
                ),
              ),
            ),
          ),
      ],
    );
  }
}
