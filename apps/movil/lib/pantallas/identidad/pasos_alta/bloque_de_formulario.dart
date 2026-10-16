import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// Agrupa campos relacionados bajo un encabezado con icono, como `FormSection`.
class BloqueDeFormulario extends StatelessWidget {
  const BloqueDeFormulario({
    super.key,
    required this.icono,
    required this.titulo,
    required this.hijos,
  });

  final IconData icono;
  final String titulo;
  final List<Widget> hijos;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            Icon(icono, size: 14, color: t.text3),
            const SizedBox(width: Espacio.s2),
            Expanded(
              child: Text(
                titulo.toUpperCase(),
                style: Tipo.etiqueta.copyWith(color: t.text2),
              ),
            ),
          ],
        ),
        const SizedBox(height: Espacio.s2),
        Tarjeta(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              for (var i = 0; i < hijos.length; i++) ...[
                if (i > 0) const SizedBox(height: Espacio.s4),
                hijos[i],
              ],
            ],
          ),
        ),
      ],
    );
  }
}
