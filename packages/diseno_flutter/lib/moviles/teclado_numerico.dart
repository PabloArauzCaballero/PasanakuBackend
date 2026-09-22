import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Grilla 3×4 para montos y PIN: dígitos, coma y borrar. Cada tecla ≥ 48 dp.
class TecladoNumerico extends StatelessWidget {
  const TecladoNumerico({
    super.key,
    required this.onTecla,
    required this.onBorrar,
    this.conComa = true,
  });
  final ValueChanged<String> onTecla;
  final VoidCallback onBorrar;
  final bool conComa;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    Widget tecla(
      String? v, {
      IconData? icono,
      String? etiqueta,
      VoidCallback? onTap,
    }) => Expanded(
      child: Semantics(
        button: v != null || icono != null,
        label: etiqueta ?? v,
        excludeSemantics: true,
        child: InkWell(
          onTap: v == null && icono == null
              ? null
              : (onTap ?? () => onTecla(v!)),
          borderRadius: BorderRadius.circular(Radios.md),
          child: Container(
            height: Tactil.minimo + Espacio.s3,
            margin: const EdgeInsets.all(Espacio.s1),
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: v == null && icono == null
                  ? Colors.transparent
                  : t.surface2,
              borderRadius: BorderRadius.circular(Radios.md),
            ),
            child: icono != null
                ? Icon(icono, color: t.text)
                : Text(
                    v ?? '',
                    style: texto.headlineSmall?.copyWith(
                      fontFamily: Fuente.display,
                      color: t.text,
                    ),
                  ),
          ),
        ),
      ),
    );
    return Column(
      children: [
        for (final fila in const [
          ['1', '2', '3'],
          ['4', '5', '6'],
          ['7', '8', '9'],
        ])
          Row(children: [for (final v in fila) tecla(v)]),
        Row(
          children: [
            conComa ? tecla(',', etiqueta: 'coma decimal') : tecla(null),
            tecla('0'),
            tecla(
              null,
              icono: Icons.backspace_outlined,
              etiqueta: 'Borrar',
              onTap: onBorrar,
            ),
          ],
        ),
      ],
    );
  }
}
