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
    this.sinAutocorreccion = false,
    this.capitalizacion = TextCapitalization.sentences,
    this.accionDeTeclado = TextInputAction.search,
  });

  final TextEditingController controlador;
  final String etiqueta;
  final ValueChanged<String>? onSubmitted;

  /// Numérico para buscar un año o un día; texto para todo lo demás.
  final TextInputType teclado;
  final bool sinAutocorreccion;
  final TextCapitalization capitalizacion;
  final TextInputAction accionDeTeclado;

  @override
  Widget build(BuildContext context) {
    return Campo(
      etiqueta: etiqueta,
      etiquetaSemantica: etiqueta,
      controlador: controlador,
      icono: Icons.search,
      tipoDeTeclado: teclado,
      autocorreccion: !sinAutocorreccion,
      capitalizacion: capitalizacion,
      accionDeTeclado: accionDeTeclado,
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
