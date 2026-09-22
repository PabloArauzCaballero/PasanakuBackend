import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Acción principal flotante: 56 dp, naranja. Una por pantalla, si hay.
class BotonFlotante extends StatelessWidget {
  const BotonFlotante({
    super.key,
    required this.icono,
    required this.etiqueta,
    required this.onPressed,
  });

  final IconData icono;
  final String etiqueta;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return FloatingActionButton(
      onPressed: onPressed,
      tooltip: etiqueta,
      backgroundColor: t.accent,
      foregroundColor: t.accentInk,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(Radios.lg),
      ),
      child: Icon(icono),
    );
  }
}
