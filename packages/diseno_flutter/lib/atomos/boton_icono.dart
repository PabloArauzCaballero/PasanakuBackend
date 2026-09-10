import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Botón de ícono cuadrado (40 dp visibles, 48 dp táctiles) para barras y filas.
class BotonIcono extends StatelessWidget {
  const BotonIcono({
    super.key,
    required this.icono,
    required this.etiqueta,
    required this.onPressed,
  });

  final IconData icono;

  /// Lo que dice el lector: un botón de ícono sin etiqueta no existe para nadie.
  final String etiqueta;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return IconButton(
      onPressed: onPressed,
      icon: Icon(icono),
      tooltip: etiqueta,
      color: t.brandTexto,
      constraints: const BoxConstraints.tightFor(
        width: Tactil.minimo,
        height: Tactil.minimo,
      ),
    );
  }
}
