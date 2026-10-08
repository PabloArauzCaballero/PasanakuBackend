import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

class ConfirmacionAporte extends StatelessWidget {
  const ConfirmacionAporte({
    super.key,
    required this.monto,
    required this.esNuevo,
  });

  final String monto;
  final bool esNuevo;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      liveRegion: true,
      child: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(Espacio.s5),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                esNuevo ? Icons.check_circle_outline : Icons.verified_outlined,
                size: Espacio.s7,
                color: t.okTexto,
              ),
              const SizedBox(height: Espacio.s3),
              Text(
                TextosPasanaku.aporteConfirmado,
                textAlign: TextAlign.center,
                style: Tipo.titulo2.copyWith(color: t.text),
              ),
              const SizedBox(height: Espacio.s3),
              Monto(
                monto: monto,
                moneda: 'BOB',
                etiqueta: TextosPasanaku.aporteConfirmado,
                estilo: Tipo.cifra.copyWith(color: t.brandInk),
              ),
              const SizedBox(height: Espacio.s3),
              Text(
                esNuevo
                    ? TextosPasanaku.aporteConfirmadoDetalle
                    : TextosPasanaku.aporteYaRegistradoDetalle,
                textAlign: TextAlign.center,
                style: Tipo.cuerpo.copyWith(color: t.text2),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
