import 'dart:async';

import 'package:flutter/material.dart';

/// `requestFocus` no mueve el scroll cuando el campo ya estaba enfocado.
void mostrarPrimerError(
  FocusNode foco,
  BuildContext pantalla, {
  double alineacion = 0.15,
}) {
  foco.requestFocus();
  WidgetsBinding.instance.addPostFrameCallback((_) {
    if (!pantalla.mounted) return;
    final contexto = foco.context;
    if (contexto != null) {
      unawaited(
        Scrollable.ensureVisible(
          contexto,
          alignment: alineacion,
          duration: Duration.zero,
        ),
      );
    }
  });
}
