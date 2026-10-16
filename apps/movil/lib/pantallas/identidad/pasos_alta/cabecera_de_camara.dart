import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// El título del paso y la pista arriba del visor — "Gira la cabeza hacia tu
/// IZQUIERDA hasta que se vea tu oreja derecha.", igual que Atlas.
class CabeceraDeCamara extends StatelessWidget {
  const CabeceraDeCamara({
    super.key,
    required this.titulo,
    required this.pista,
    required this.onVolver,
  });

  final String titulo;
  final String pista;
  final VoidCallback onVolver;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Row(
        children: [
          IconButton(
            icon: const Icon(Icons.arrow_back, color: Paleta.white),
            onPressed: onVolver,
          ),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  titulo,
                  style: Theme.of(
                    context,
                  ).textTheme.titleMedium?.copyWith(color: Paleta.white),
                ),
                Text(
                  pista,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: Paleta.white.withValues(alpha: 0.7),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
