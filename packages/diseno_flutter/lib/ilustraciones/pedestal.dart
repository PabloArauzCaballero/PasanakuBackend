import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Apoya un dibujo sobre un orbe con sombra, dentro de un aro tenue.
///
/// Un dibujo plano contra el fondo de la app se lee como un ícono grande; sobre un
/// orbe con sombra se lee como un objeto que está ahí. Son tres capas y ninguna pide
/// atención sola: el aro de marca abre el espacio, el orbe claro levanta el dibujo, y
/// la sombra dice a qué altura está.
class Pedestal extends StatelessWidget {
  const Pedestal({super.key, required this.hijo, required this.tamano});

  /// El lado del dibujo. El orbe y el aro salen de acá, en proporción.
  final double tamano;
  final Widget hijo;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return SizedBox.square(
      dimension: tamano * 1.3,
      child: Stack(
        alignment: Alignment.center,
        children: [
          _Circulo(lado: tamano * 1.3, color: t.brandBg.withValues(alpha: 0.5)),
          _Circulo(lado: tamano * 1.06, color: t.surface, sombra: t.sombra3),
          hijo,
        ],
      ),
    );
  }
}

class _Circulo extends StatelessWidget {
  const _Circulo({required this.lado, required this.color, this.sombra});

  final double lado;
  final Color color;
  final BoxShadow? sombra;

  @override
  Widget build(BuildContext context) => SizedBox.square(
    dimension: lado,
    child: DecoratedBox(
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: color,
        boxShadow: sombra == null ? null : [sombra!],
      ),
    ),
  );
}
