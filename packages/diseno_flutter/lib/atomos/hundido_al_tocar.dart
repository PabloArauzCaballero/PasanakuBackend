import 'package:flutter/physics.dart';
import 'package:flutter/material.dart';

import 'movimiento.dart';

/// Hunde lo que envuelve mientras el dedo está encima, y al soltar lo devuelve con un
/// **resorte**: pasa apenas de su tamaño y se asienta, como los botones de iOS.
///
/// La escala llega a 0,97 en 120 ms y vuelve con el resorte compartido: respuesta
/// breve, perceptible y consistente entre botones y filas.
///
/// `Listener` y no `GestureDetector`: no compite en la arena de gestos, así que el
/// botón de adentro sigue recibiendo su toque tal cual. Respeta «reducir movimiento».
class HundidoAlTocar extends StatefulWidget {
  const HundidoAlTocar({super.key, required this.child, this.activo = true});

  final Widget child;
  final bool activo;

  @override
  State<HundidoAlTocar> createState() => _HundidoAlTocarState();
}

class _HundidoAlTocarState extends State<HundidoAlTocar>
    with SingleTickerProviderStateMixin {
  // Sin límites: el resorte tiene que poder pasarse por debajo de cero, que es el
  // instante en que el botón queda un poco más grande que su tamaño antes de asentarse.
  late final AnimationController _control;

  static const _escalaPresion = 0.97;

  @override
  void initState() {
    super.initState();
    // Even with reduced motion there is no AnimatedBuilder; initialize the
    // ticker while the State is active rather than lazily from dispose().
    _control = AnimationController.unbounded(vsync: this);
  }

  void _apretar() {
    if (!widget.activo) return;
    _control.animateTo(
      1,
      duration: duracion(context, Movimiento.micro),
      curve: Curves.easeOut,
    );
  }

  void _soltar() {
    if (!widget.activo) return;
    _control.animateWith(
      SpringSimulation(Movimiento.resorte, _control.value, 0, -3),
    );
  }

  @override
  void dispose() {
    _control.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (reducirMovimiento(context)) return widget.child;
    return Listener(
      onPointerDown: (_) => _apretar(),
      onPointerUp: (_) => _soltar(),
      onPointerCancel: (_) => _soltar(),
      child: AnimatedBuilder(
        animation: _control,
        child: widget.child,
        builder: (context, hijo) => Transform.scale(
          scale: 1 - (1 - _escalaPresion) * _control.value,
          child: hijo,
        ),
      ),
    );
  }
}
