import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Opciones excluyentes con su etiqueta.
class GrupoRadio<T> extends StatelessWidget {
  const GrupoRadio({
    super.key,
    required this.opciones,
    required this.valor,
    required this.onChanged,
  });

  final Map<T, String> opciones;
  final T? valor;
  final ValueChanged<T?> onChanged;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return RadioGroup<T>(
      groupValue: valor,
      onChanged: onChanged,
      child: Column(
        children: [
          for (final e in opciones.entries)
            RadioListTile<T>(
              value: e.key,
              title: Text(e.value),
              activeColor: t.verdeSolido,
              contentPadding: EdgeInsets.zero,
            ),
        ],
      ),
    );
  }
}
