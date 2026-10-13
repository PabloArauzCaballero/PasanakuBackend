import 'package:flutter/material.dart';

import 'movimiento.dart';

/// Entrada breve de vacíos/errores; no mueve el layout ni anima con reduce motion.
class EntradaDeEstado extends StatelessWidget {
  const EntradaDeEstado({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) => TweenAnimationBuilder<double>(
    tween: Tween<double>(begin: 0, end: 1),
    duration: duracion(context, Movimiento.entrada),
    curve: Curves.easeOut,
    builder: (context, opacidad, child) =>
        Opacity(opacity: opacidad, child: child),
    child: child,
  );
}
