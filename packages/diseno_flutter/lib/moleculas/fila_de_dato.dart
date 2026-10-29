import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Un dato con su nombre arriba: «Huella comprometida» y debajo el valor, que se puede
/// seleccionar y copiar (ADR-053).
///
/// Las pantallas de verificación pintaban `Hash: 9f2c…` en una sola línea corrida, sin
/// separar el nombre del valor; con huellas de 64 caracteres eso era una pared de texto.
/// El valor corta donde haga falta y lleva cifras tabulares para que dos huellas se
/// comparen columna a columna.
class FilaDeDato extends StatelessWidget {
  const FilaDeDato({super.key, required this.etiqueta, required this.valor});

  final String etiqueta;
  final String valor;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: Espacio.s2),
      child: MergeSemantics(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(etiqueta, style: Tipo.etiqueta.copyWith(color: t.text3)),
            const SizedBox(height: Espacio.s1),
            SelectableText(
              valor,
              style: Tipo.cuerpoChico.copyWith(
                color: t.text,
                fontFeatures: const [FontFeature.tabularFigures()],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
