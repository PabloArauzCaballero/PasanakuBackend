import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:flutter/material.dart';

class CuentaAtrasGrande extends StatelessWidget {
  const CuentaAtrasGrande({super.key, required this.segundos});

  final int? segundos;

  @override
  Widget build(BuildContext context) {
    final cuenta = segundos;
    if (cuenta == null) return const SizedBox.expand();
    final texto = Text(
      '$cuenta',
      key: ValueKey(cuenta),
      style: Tipo.cuentaAtras.copyWith(
        color: Tokens.of(context).camaraTinta,
        shadows: [Shadow(color: Tokens.of(context).camaraVelo, blurRadius: 18)],
      ),
    );
    final cambio = reducirMovimiento(context)
        ? texto
        : AnimatedSwitcher(
            duration: duracion(context, Movimiento.cambioDeCuentaAtras),
            child: texto,
          );
    return RepaintBoundary(
      child: Center(
        child: Semantics(liveRegion: true, label: '$cuenta', child: cambio),
      ),
    );
  }
}
