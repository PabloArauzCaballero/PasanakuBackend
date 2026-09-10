import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Número de cuenta enmascarado: últimos cuatro, en pantalla y en el lector.
class CuentaEnmascarada extends StatelessWidget {
  const CuentaEnmascarada({super.key, required this.numero, this.banco});
  final String numero;
  final String? banco;

  static String enmascarar(String numero) {
    final limpio = numero.replaceAll(RegExp(r'\s'), '');
    final ultimos = limpio.length >= 4
        ? limpio.substring(limpio.length - 4)
        : limpio;
    return '•••• $ultimos';
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = enmascarar(numero);
    return Semantics(
      label:
          '${banco ?? 'Cuenta'} terminada en ${texto.substring(texto.length - 4)}',
      excludeSemantics: true,
      child: Text(
        banco == null ? texto : '$banco · $texto',
        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
          fontFamily: Fuente.display,
          color: t.text,
        ),
      ),
    );
  }
}
