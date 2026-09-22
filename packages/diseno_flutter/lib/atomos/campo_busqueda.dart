import 'package:flutter/material.dart';

import '../atomos/campo.dart';

/// Búsqueda: ícono, borrar y envío con el teclado.
class CampoBusqueda extends StatelessWidget {
  const CampoBusqueda({
    super.key,
    required this.controlador,
    this.etiqueta = 'Buscar',
    this.onSubmitted,
  });

  final TextEditingController controlador;
  final String etiqueta;
  final ValueChanged<String>? onSubmitted;

  @override
  Widget build(BuildContext context) {
    return Campo(
      etiqueta: etiqueta,
      controlador: controlador,
      icono: Icons.search,
      tipoDeTeclado: TextInputType.text,
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
