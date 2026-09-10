import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Interruptor: activo en `ok` (es un estado, no una acción).
class Interruptor extends StatelessWidget {
  const Interruptor({
    super.key,
    required this.etiqueta,
    required this.valor,
    required this.onChanged,
    this.detalle,
  });

  final String etiqueta;
  final String? detalle;
  final bool valor;
  final ValueChanged<bool>? onChanged;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return SwitchListTile(
      value: valor,
      onChanged: onChanged,
      title: Text(etiqueta),
      subtitle: detalle == null ? null : Text(detalle!),
      activeTrackColor: t.ok,
      contentPadding: EdgeInsets.zero,
    );
  }
}
