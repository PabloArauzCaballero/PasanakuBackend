import 'package:flutter/material.dart';

import '../atomos/monto.dart';
import '../tokens/tokens.dart';

/// Un valor **estimado** entre un mínimo y un máximo (D-20): nunca un número exacto.
class MedidorDeRango extends StatelessWidget {
  const MedidorDeRango({
    super.key,
    required this.etiqueta,
    required this.minimo,
    required this.maximo,
    required this.moneda,
    this.estimado,
  });
  final String etiqueta;
  final String minimo;
  final String maximo;
  final String moneda;
  final String? estimado;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '$etiqueta (estimado)',
          style: texto.labelLarge?.copyWith(color: t.text2),
        ),
        const SizedBox(height: Espacio.s2),
        Container(
          height: Espacio.s2,
          decoration: BoxDecoration(
            gradient: LinearGradient(colors: [Paleta.g200, t.brand]),
            borderRadius: BorderRadius.circular(Radios.pill),
          ),
        ),
        const SizedBox(height: Espacio.s1),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Monto(
              monto: minimo,
              moneda: moneda,
              etiqueta: 'Mínimo',
              estilo: texto.bodySmall?.copyWith(color: t.text3),
            ),
            if (estimado != null)
              Monto(
                monto: estimado!,
                moneda: moneda,
                etiqueta: 'Estimado',
                estilo: texto.titleMedium?.copyWith(color: t.brandTexto),
              ),
            Monto(
              monto: maximo,
              moneda: moneda,
              etiqueta: 'Máximo',
              estilo: texto.bodySmall?.copyWith(color: t.text3),
            ),
          ],
        ),
      ],
    );
  }
}
