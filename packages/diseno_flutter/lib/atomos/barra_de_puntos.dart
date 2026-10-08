import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Progreso del tour: un punto por pantalla, el actual en marca.
class BarraDePuntos extends StatelessWidget {
  const BarraDePuntos({super.key, required this.total, required this.actual});
  final int total;
  final int actual;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final quieto = MediaQuery.disableAnimationsOf(context);
    return Semantics(
      label: 'Pantalla ${actual + 1} de $total',
      excludeSemantics: true,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (var i = 0; i < total; i += 1)
            AnimatedContainer(
              duration: quieto ? Duration.zero : MovimientoDeInterfaz.rapido,
              margin: const EdgeInsets.symmetric(horizontal: Espacio.s1),
              width: i == actual ? Espacio.s5 : Espacio.s2,
              height: Espacio.s2,
              decoration: BoxDecoration(
                color: i == actual ? t.brand : t.border,
                borderRadius: BorderRadius.circular(Radios.pill),
              ),
            ),
        ],
      ),
    );
  }
}
