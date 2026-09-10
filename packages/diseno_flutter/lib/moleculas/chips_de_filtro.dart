import 'package:flutter/material.dart';

import '../atomos/chip_elegible.dart';
import '../tokens/tokens.dart';

/// Chips de filtro **en varias líneas**, cada uno con su ícono, sin «otros» (gate de F4).
class ChipsDeFiltro<T> extends StatelessWidget {
  const ChipsDeFiltro({
    super.key,
    required this.opciones,
    required this.valor,
    required this.onChanged,
  });
  final List<({T clave, String texto, IconData icono})> opciones;
  final T valor;
  final ValueChanged<T> onChanged;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: Espacio.s2,
      runSpacing: Espacio.s2,
      children: [
        for (final o in opciones)
          ChipElegible(
            texto: o.texto,
            icono: o.icono,
            seleccionado: o.clave == valor,
            onTap: () => onChanged(o.clave),
          ),
      ],
    );
  }
}
