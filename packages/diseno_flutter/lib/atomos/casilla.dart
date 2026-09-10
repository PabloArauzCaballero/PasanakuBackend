import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Casilla con su etiqueta como una sola cosa tocable (48 dp).
class Casilla extends StatelessWidget {
  const Casilla({
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
    return CheckboxListTile(
      value: valor,
      onChanged: onChanged == null ? null : (v) => onChanged!(v ?? false),
      title: Text(etiqueta),
      subtitle: detalle == null ? null : Text(detalle!),
      controlAffinity: ListTileControlAffinity.leading,
      activeColor: t.verdeSolido,
      checkColor: t.sobreVerdeSolido,
      contentPadding: EdgeInsets.zero,
      minTileHeight: Tactil.minimo,
    );
  }
}
