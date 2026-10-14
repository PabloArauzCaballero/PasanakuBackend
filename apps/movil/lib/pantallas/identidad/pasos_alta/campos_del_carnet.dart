import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// Apila los datos del carnet cuando el ancho disponible no sostiene las ayudas.
class CamposDelCarnet extends StatelessWidget {
  const CamposDelCarnet({
    super.key,
    required this.numero,
    required this.expedicion,
  });

  final Widget numero;
  final Widget expedicion;

  @override
  Widget build(BuildContext context) => LayoutBuilder(
    builder: (context, limites) {
      if (limites.maxWidth < 480) {
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            numero,
            const SizedBox(height: Espacio.s3),
            expedicion,
          ],
        );
      }
      return Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(flex: 3, child: numero),
          const SizedBox(width: Espacio.s3),
          Expanded(flex: 2, child: expedicion),
        ],
      );
    },
  );
}
