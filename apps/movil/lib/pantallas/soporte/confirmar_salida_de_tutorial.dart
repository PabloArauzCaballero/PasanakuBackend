import 'package:flutter/material.dart';

import 'textos.dart';

/// Confirmación antes de abandonar a la mitad. No es un `showDialog` porque el globo ya
/// está encima de todo: se dibuja en la misma capa y no pelea por el foco.
class ConfirmarSalidaDeTutorial extends StatelessWidget {
  const ConfirmarSalidaDeTutorial({
    super.key,
    required this.alSalir,
    required this.alSeguir,
  });

  final VoidCallback alSalir;
  final VoidCallback alSeguir;

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: const Text(TextosSoporte.salirTitulo),
    content: const Text(TextosSoporte.salirCuerpo),
    actions: [
      TextButton(
        onPressed: alSalir,
        child: const Text(TextosSoporte.salirConfirmar),
      ),
      TextButton(
        onPressed: alSeguir,
        child: const Text(TextosSoporte.salirSeguir),
      ),
    ],
  );
}
