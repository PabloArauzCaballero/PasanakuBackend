import 'package:flutter/material.dart';

import '../atomos/campo.dart';

/// Búsqueda: ícono, borrar y envío con el teclado.
class CampoBusqueda extends StatelessWidget {
  const CampoBusqueda({
    super.key,
    required this.controlador,
    this.etiqueta = 'Buscar',
    this.onSubmitted,
    this.teclado = TextInputType.text,
  });

  final TextEditingController controlador;
  final String etiqueta;
  final ValueChanged<String>? onSubmitted;

  /// Numérico para buscar un año o un día; texto para todo lo demás.
  final TextInputType teclado;

  @override
  Widget build(BuildContext context) {
    return Campo(
      etiqueta: etiqueta,
      controlador: controlador,
      icono: Icons.search,
      tipoDeTeclado: teclado,
      onSubmitted: onSubmitted,
      sufijo: ListenableBuilder(
        listenable: controlador,
        builder: (context, _) => controlador.text.isEmpty
            ? const SizedBox.shrink()
            : IconButton(
                tooltip: 'Borrar búsqueda',
                icon: const Icon(Icons.close),
                onPressed: controlador.clear,
              ),
      ),
    );
  }
}
