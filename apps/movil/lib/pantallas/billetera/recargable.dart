import 'package:flutter/material.dart';

/// Deslizar hacia abajo vuelve a pedir los datos (2026-10-19). Envuelve contenido que
/// no se desplaza (un estado vacío, un aviso, un formulario corto) en un desplazable
/// que ocupa todo el alto, para que el gesto funcione aunque no haya nada que
/// desplazar. El contenido queda centrado como antes.
class Recargable extends StatelessWidget {
  const Recargable({
    super.key,
    required this.alRecargar,
    required this.hijo,
    this.alineacion = Alignment.center,
  });

  final Future<void> Function() alRecargar;
  final Widget hijo;

  /// Dónde queda el contenido si es más bajo que la pantalla: centrado para un aviso,
  /// arriba para una lista o un comprobante.
  final Alignment alineacion;

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: alRecargar,
      child: LayoutBuilder(
        builder: (context, limites) => SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          child: ConstrainedBox(
            constraints: BoxConstraints(
              minHeight: limites.hasBoundedHeight ? limites.maxHeight : 0,
            ),
            child: Align(alignment: alineacion, child: hijo),
          ),
        ),
      ),
    );
  }
}
