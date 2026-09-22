import 'package:flutter/material.dart';

import '../atomos/tono.dart';
import '../tokens/tokens.dart';

/// Progreso determinado, en barra o en anillo, con su valor anunciado.
class Progreso extends StatelessWidget {
  const Progreso({
    super.key,
    required this.valor,
    required this.etiqueta,
    this.anillo = false,
    this.tono = Tono.marca,
  });

  /// 0..1
  final double valor;
  final String etiqueta;
  final bool anillo;
  final Tono tono;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final color = tono == Tono.marca ? t.brand : tono.coloresDe(t).$2;
    final porcentaje = '${(valor.clamp(0, 1) * 100).round()} por ciento';
    return Semantics(
      label: etiqueta,
      value: porcentaje,
      excludeSemantics: true,
      child: anillo
          ? SizedBox(
              width: Espacio.s7,
              height: Espacio.s7,
              child: CircularProgressIndicator(
                value: valor.clamp(0, 1),
                strokeWidth: Espacio.s1,
                color: color,
                backgroundColor: t.surface2,
              ),
            )
          : LinearProgressIndicator(
              value: valor.clamp(0, 1),
              minHeight: Espacio.s2,
              color: color,
              backgroundColor: t.surface2,
              borderRadius: BorderRadius.circular(Radios.pill),
            ),
    );
  }
}
