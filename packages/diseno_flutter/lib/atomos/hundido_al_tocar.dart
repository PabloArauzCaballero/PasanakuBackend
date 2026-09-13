import 'package:flutter/physics.dart';
import 'package:flutter/material.dart';

/// Hunde lo que envuelve mientras el dedo está encima, y al soltar lo devuelve con un
/// **resorte**: pasa apenas de su tamaño y se asienta, como los botones de iOS.
///
/// La versión anterior achicaba un 3 % con una curva lineal y no se notaba — que es
/// peor que no tenerlo, porque el botón igual se siente muerto. Un 4,5 % con rebote se
/// ve y se siente en la mano, sin llegar a juguete.
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
  late final AnimationController _control = AnimationController.unbounded(
    vsync: this,
  );

  static const _hundido = 0.045;
  static const _resorte = SpringDescription(
    mass: 1,
    stiffness: 520,
    damping: 17,
  );

  void _apretar() {
    if (!widget.activo) return;
    _control.animateTo(
      1,
      duration: const Duration(milliseconds: 110),
      curve: Curves.easeOut,
    );
  }

  void _soltar() {
    if (!widget.activo) return;
    _control.animateWith(SpringSimulation(_resorte, _control.value, 0, -3));
  }

  @override
  void dispose() {
    _control.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (MediaQuery.disableAnimationsOf(context)) return widget.child;
    return Listener(
      onPointerDown: (_) => _apretar(),
      onPointerUp: (_) => _soltar(),
      onPointerCancel: (_) => _soltar(),
      child: AnimatedBuilder(
        animation: _control,
        child: widget.child,
        builder: (context, hijo) =>
            Transform.scale(scale: 1 - _hundido * _control.value, child: hijo),
      ),
    );
  }
}
