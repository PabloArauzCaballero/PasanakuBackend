import 'package:flutter/material.dart';

import '../atomos/tono.dart';
import '../atomos/movimiento.dart';
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
    final valorFinal = valor.clamp(0, 1).toDouble();
    final porcentaje = '${(valorFinal * 100).round()} por ciento';
    return Semantics(
      label: etiqueta,
      value: porcentaje,
      excludeSemantics: true,
      child: anillo
          ? TweenAnimationBuilder<double>(
              tween: Tween<double>(begin: 0, end: valorFinal),
              duration: duracion(context, Movimiento.entrada),
              curve: Movimiento.llega,
              builder: (context, avance, _) => SizedBox(
                width: Espacio.s7,
                height: Espacio.s7,
                child: CircularProgressIndicator(
                  value: avance,
                  strokeWidth: Espacio.s1,
                  color: color,
                  backgroundColor: t.surface2,
                ),
              ),
            )
          : TweenAnimationBuilder<double>(
              tween: Tween<double>(begin: 0, end: valorFinal),
              duration: duracion(context, Movimiento.entrada),
              curve: Movimiento.llega,
              builder: (context, avance, _) => LinearProgressIndicator(
                value: avance,
                minHeight: Espacio.s2,
                color: color,
                backgroundColor: t.surface2,
                borderRadius: BorderRadius.circular(Radios.pill),
              ),
            ),
    );
  }
}
