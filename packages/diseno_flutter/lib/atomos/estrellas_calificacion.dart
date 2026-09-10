import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Calificación de 1 a 5, tocable o solo de lectura.
class EstrellasCalificacion extends StatelessWidget {
  const EstrellasCalificacion({super.key, required this.valor, this.onChanged});
  final int valor;
  final ValueChanged<int>? onChanged;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      label: 'Calificación',
      value: '$valor de 5',
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (var i = 1; i <= 5; i += 1)
            IconButton(
              onPressed: onChanged == null ? null : () => onChanged!(i),
              tooltip: '$i de 5',
              icon: Icon(
                i <= valor ? Icons.star : Icons.star_border,
                color: i <= valor ? t.warn : t.text3,
              ),
              constraints: const BoxConstraints.tightFor(
                width: Tactil.minimo,
                height: Tactil.minimo,
              ),
            ),
        ],
      ),
    );
  }
}
