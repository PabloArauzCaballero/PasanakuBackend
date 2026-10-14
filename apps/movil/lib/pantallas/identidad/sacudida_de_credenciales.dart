import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:flutter/material.dart';

/// Desplazamiento breve del formulario cuando el servidor rechaza credenciales.
class SacudidaDeCredenciales extends StatefulWidget {
  const SacudidaDeCredenciales({super.key, required this.child});

  final Widget child;

  @override
  SacudidaDeCredencialesState createState() => SacudidaDeCredencialesState();
}

class SacudidaDeCredencialesState extends State<SacudidaDeCredenciales>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controlador = AnimationController(
    vsync: this,
    duration: Movimiento.sacudidaDeCredencial,
  );
  late final Animation<double> _desplazamiento = TweenSequence<double>([
    TweenSequenceItem(tween: Tween(begin: 0, end: 6), weight: 1),
    TweenSequenceItem(tween: Tween(begin: 6, end: -6), weight: 1),
    TweenSequenceItem(tween: Tween(begin: -6, end: 6), weight: 1),
    TweenSequenceItem(tween: Tween(begin: 6, end: -6), weight: 1),
    TweenSequenceItem(tween: Tween(begin: -6, end: 6), weight: 1),
    TweenSequenceItem(tween: Tween(begin: 6, end: 0), weight: 1),
  ]).animate(_controlador);

  void ejecutar() {
    if (!reducirMovimiento(context)) _controlador.forward(from: 0);
  }

  @override
  void dispose() {
    _controlador.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
    key: const ValueKey('sacudida-de-credencial'),
    animation: _desplazamiento,
    child: widget.child,
    builder: (context, child) => Transform.translate(
      offset: Offset(_desplazamiento.value, 0),
      child: child,
    ),
  );
}
