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
    assert(opciones.isNotEmpty, 'GrupoRadio requiere opciones.');
    if (opciones.isEmpty) return const SizedBox.shrink();
    if (opciones.length == 1) {
      return Semantics(
        container: true,
        label: opciones.values.single,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: Espacio.s2),
          child: Text(
            opciones.values.single,
            style: Theme.of(
              context,
            ).textTheme.bodyMedium?.copyWith(color: t.text2),
          ),
        ),
      );
    }
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
