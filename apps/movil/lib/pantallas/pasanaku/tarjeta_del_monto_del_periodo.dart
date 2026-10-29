import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// El importe del aporte como cifra protagonista. **No se edita**: lo fija el reglamento del
/// grupo. Antes era un campo editable que ignoraba lo que se escribía en él.
class TarjetaDelMontoDelPeriodo extends StatelessWidget {
  const TarjetaDelMontoDelPeriodo({super.key, required this.monto});

  final String monto;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Tarjeta(
      child: Column(
        children: [
          Text(
            TextosPasanaku.aporteDelPeriodo,
            style: Tipo.cuerpo.copyWith(color: t.text2),
          ),
          const SizedBox(height: Espacio.s2),
          FittedBox(
            fit: BoxFit.scaleDown,
            child: Monto(
              monto: monto,
              moneda: 'BOB',
              etiqueta: TextosPasanaku.aporteDelPeriodo,
              estilo: Tipo.cifraGrande.copyWith(color: t.text),
            ),
          ),
          const SizedBox(height: Espacio.s2),
          Text(
            TextosPasanaku.loFijaElReglamento,
            textAlign: TextAlign.center,
            style: Tipo.cuerpoChico.copyWith(color: t.text3),
          ),
        ],
      ),
    );
  }
}
