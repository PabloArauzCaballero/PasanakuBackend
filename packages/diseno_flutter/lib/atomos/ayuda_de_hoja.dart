import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// La linea de ayuda de la hoja de seleccion: un icono y el texto, sin accion.
///
/// Vivia dentro de `hoja_de_seleccion.dart`; se separo por el limite de 200 lineas.
class AyudaDeHoja extends StatelessWidget {
  const AyudaDeHoja(this.texto, {super.key});

  final String texto;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.fromLTRB(Espacio.s4, Espacio.s2, Espacio.s4, 0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.info_outline_rounded, size: 16, color: t.text3),
          const SizedBox(width: Espacio.s2),
          Expanded(
            child: Text(texto, style: Tipo.ayuda.copyWith(color: t.text2)),
          ),
        ],
      ),
    );
  }
}
