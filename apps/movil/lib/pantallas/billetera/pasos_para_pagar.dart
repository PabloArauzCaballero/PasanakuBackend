import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// Los tres pasos para pagar el QR, numerados y cortos. El tercero dice cuándo sube el saldo:
/// cuando el banco confirma, no cuando la persona escanea (regla 91.3).
class PasosParaPagar extends StatelessWidget {
  const PasosParaPagar({super.key});

  static const _pasos = [
    TextosBilletera.pasoAbriTuBanco,
    TextosBilletera.pasoEscaneaElQr,
    TextosBilletera.pasoSubeTuSaldo,
  ];

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Semantics(
          header: true,
          child: Text(
            TextosBilletera.comoPagar,
            style: Tipo.titulo3.copyWith(color: t.text),
          ),
        ),
        const SizedBox(height: Espacio.s3),
        for (final (i, paso) in _pasos.indexed) ...[
          if (i > 0) const SizedBox(height: Espacio.s3),
          _Paso(numero: i + 1, texto: paso),
        ],
      ],
    );
  }
}

class _Paso extends StatelessWidget {
  const _Paso({required this.numero, required this.texto});
  final int numero;
  final String texto;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      label: 'Paso $numero: $texto',
      excludeSemantics: true,
      child: Row(
        children: [
          DecoratedBox(
            decoration: BoxDecoration(color: t.brandBg, shape: BoxShape.circle),
            child: SizedBox.square(
              dimension: Espacio.s6,
              child: Center(
                child: Text(
                  '$numero',
                  style: Tipo.cuerpoFuerte.copyWith(color: t.brandTexto),
                ),
              ),
            ),
          ),
          const SizedBox(width: Espacio.s3),
          Expanded(
            child: Text(texto, style: Tipo.cuerpo.copyWith(color: t.text)),
          ),
        ],
      ),
    );
  }
}
