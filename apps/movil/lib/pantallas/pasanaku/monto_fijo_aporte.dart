import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

class MontoFijoAporte extends StatelessWidget {
  const MontoFijoAporte({super.key, required this.monto});

  final String monto;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Container(
      padding: const EdgeInsets.all(Espacio.s4),
      decoration: BoxDecoration(
        color: t.surface,
        borderRadius: BorderRadius.circular(Radios.lg),
        border: Border.all(color: t.border, width: Borde.fino),
        boxShadow: [t.sombra1],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            TextosPasanaku.aporteMontoTitulo,
            style: Tipo.cuerpoFuerte.copyWith(color: t.text2),
          ),
          const SizedBox(height: Espacio.s2),
          Monto(
            monto: monto,
            moneda: 'BOB',
            etiqueta: TextosPasanaku.aporteMontoTitulo,
            estilo: Tipo.cifraGrande.copyWith(color: t.brandInk),
          ),
          const SizedBox(height: Espacio.s2),
          Text(
            TextosPasanaku.aporteMontoAyuda,
            style: Tipo.cuerpoChico.copyWith(color: t.text2),
          ),
        ],
      ),
    );
  }
}
