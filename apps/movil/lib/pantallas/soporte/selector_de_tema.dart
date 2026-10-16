import 'package:aportaya_diseno/atomos/campo_de_seleccion.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// Selector de categorías del catálogo que realmente ofrece el centro de ayuda.
class SelectorDeTemaDeAyuda extends StatelessWidget {
  const SelectorDeTemaDeAyuda({
    super.key,
    required this.categorias,
    required this.valor,
    required this.onChanged,
  });

  final List<String> categorias;
  final String? valor;
  final ValueChanged<String?> onChanged;

  @override
  Widget build(BuildContext context) => CampoDeSeleccion<String>(
    etiqueta: TextosSoporte.temaDelTutorial,
    opciones: [
      (valor: '', texto: TextosSoporte.todosLosTemas, detalle: null),
      for (final categoria in categorias)
        (valor: categoria, texto: categoria, detalle: null),
    ],
    valor: valor ?? '',
    ayuda: TextosSoporte.filtrarAyuda,
    onElegida: (seleccion) => onChanged(seleccion.isEmpty ? null : seleccion),
  );
}
