import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Mensaje inferior efímero, con acción opcional. Se anuncia.
void mostrarSnackbar(
  BuildContext context,
  String texto, {
  String? accion,
  VoidCallback? onAccion,
}) {
  final t = Tokens.of(context);
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(
      content: Text(texto, style: TextStyle(color: t.sobreVerdeSolido)),
      backgroundColor: t.verdeSolido,
      behavior: SnackBarBehavior.floating,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(Radios.md),
      ),
      action: accion == null
          ? null
          : SnackBarAction(
              label: accion,
              textColor: t.accent,
              onPressed: onAccion ?? () {},
            ),
    ),
  );
}
