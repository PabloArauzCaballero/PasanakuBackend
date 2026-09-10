import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// El paso N de M del alta: barra segmentada y el nombre del paso.
class BarraDePasos extends StatelessWidget {
  const BarraDePasos({
    super.key,
    required this.total,
    required this.actual,
    required this.nombre,
  });
  final int total;
  final int actual;
  final String nombre;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Semantics(
      label: 'Paso $actual de $total: $nombre',
      excludeSemantics: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              for (var i = 1; i <= total; i += 1)
                Expanded(
                  child: Container(
                    height: Espacio.s1,
                    margin: EdgeInsets.only(right: i == total ? 0 : Espacio.s1),
                    decoration: BoxDecoration(
                      color: i <= actual ? t.brand : t.border,
                      borderRadius: BorderRadius.circular(Radios.pill),
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(height: Espacio.s2),
          Text(
            'Paso $actual de $total · $nombre',
            style: texto.labelLarge?.copyWith(color: t.text2),
          ),
        ],
      ),
    );
  }
}
