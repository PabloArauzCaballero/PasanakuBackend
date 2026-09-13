import 'package:aportaya_diseno/moleculas/opcion_seleccionable.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// Las opciones excluyentes, en una o dos columnas según cuánto texto llevan.
class GrillaDeOpciones extends StatelessWidget {
  const GrillaDeOpciones({
    super.key,
    required this.opciones,
    required this.elegida,
    required this.onElegir,
    this.columnas = 2,
  });

  final List<({String clave, String titulo, String detalle})> opciones;
  final String elegida;
  final ValueChanged<String> onElegir;
  final int columnas;

  @override
  Widget build(BuildContext context) {
    final filas = <Widget>[];
    for (var i = 0; i < opciones.length; i += columnas) {
      final grupo = opciones.skip(i).take(columnas).toList();
      filas.add(
        IntrinsicHeight(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              for (var j = 0; j < columnas; j++) ...[
                if (j > 0) const SizedBox(width: Espacio.s2),
                Expanded(
                  child: j < grupo.length
                      ? OpcionSeleccionable(
                          titulo: grupo[j].titulo,
                          detalle: grupo[j].detalle,
                          elegida: grupo[j].clave == elegida,
                          onTap: () => onElegir(grupo[j].clave),
                        )
                      : const SizedBox.shrink(),
                ),
              ],
            ],
          ),
        ),
      );
    }
    return Column(
      children: [
        for (var i = 0; i < filas.length; i++) ...[
          if (i > 0) const SizedBox(height: Espacio.s2),
          filas[i],
        ],
      ],
    );
  }
}
