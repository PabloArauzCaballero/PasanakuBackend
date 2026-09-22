import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Pestañas de contenido, el activo en marca.
class Pestanas extends StatelessWidget {
  const Pestanas({
    super.key,
    required this.titulos,
    required this.actual,
    required this.onChanged,
  });
  final List<String> titulos;
  final int actual;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Row(
      children: [
        for (var i = 0; i < titulos.length; i += 1)
          Expanded(
            child: Semantics(
              button: true,
              selected: i == actual,
              label: titulos[i],
              child: InkWell(
                onTap: () => onChanged(i),
                child: Container(
                  height: Tactil.minimo,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    border: Border(
                      bottom: BorderSide(
                        color: i == actual ? t.brand : t.border,
                        width: i == actual ? Borde.foco : Borde.fino,
                      ),
                    ),
                  ),
                  child: Text(
                    titulos[i],
                    style: texto.labelLarge?.copyWith(
                      color: i == actual ? t.brandTexto : t.text2,
                      fontWeight: i == actual
                          ? FontWeight.w700
                          : FontWeight.w500,
                    ),
                  ),
                ),
              ),
            ),
          ),
      ],
    );
  }
}
