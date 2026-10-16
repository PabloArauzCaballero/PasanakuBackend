import 'package:flutter/material.dart';

import '../atomos/movimiento.dart';

/// Los hijos entran uno detrás de otro, subiendo apenas mientras aparecen.
///
/// **Un solo momento orquestado, al abrir la pantalla.** No es una animación por
/// elemento repartida por toda la app —eso cansa y se nota fabricado—: es la pantalla
/// armándose una vez, de arriba hacia abajo, que es el orden en que se lee.
///
/// El escalón es corto a propósito: 45 ms entre hijos. Lo suficiente para que el ojo
/// perciba orden, muy poco para que nadie espere. Y el primer hijo arranca en el
/// primer frame, así que la pantalla nunca está vacía. Con «reducir movimiento» del
/// sistema, todo aparece de una.
class AparicionEscalonada extends StatefulWidget {
  const AparicionEscalonada({
    super.key,
    required this.children,
    this.escalon = const Duration(milliseconds: 45),
    this.duracionPorHijo = const Duration(milliseconds: 280),
    this.demoraInicial = Duration.zero,
  });

  final List<Widget> children;
  final Duration escalon;
  final Duration duracionPorHijo;
  final Duration demoraInicial;

  @override
  State<AparicionEscalonada> createState() => _AparicionEscalonadaState();
}

class _AparicionEscalonadaState extends State<AparicionEscalonada>
    with SingleTickerProviderStateMixin {
  late final AnimationController _control = AnimationController(
    vsync: this,
    duration:
        widget.demoraInicial +
        widget.duracionPorHijo +
        widget.escalon * (widget.children.length - 1).clamp(0, 20),
  );

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (reducirMovimiento(context)) {
      _control.value = 1;
    } else if (!_control.isAnimating && _control.value == 0) {
      _control.forward();
    }
  }

  @override
  void dispose() {
    _control.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final total = _control.duration!.inMilliseconds;
    final porHijo = widget.duracionPorHijo.inMilliseconds / total;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      mainAxisSize: MainAxisSize.min,
      children: [
        for (var i = 0; i < widget.children.length; i++)
          _Hijo(
            control: _control,
            desde:
                (widget.demoraInicial.inMilliseconds +
                    widget.escalon.inMilliseconds * i) /
                total,
            largo: porHijo,
            child: widget.children[i],
          ),
      ],
    );
  }
}

class _Hijo extends StatelessWidget {
  const _Hijo({
    required this.control,
    required this.desde,
    required this.largo,
    required this.child,
  });

  final Animation<double> control;
  final double desde;
  final double largo;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final curva = CurvedAnimation(
      parent: control,
      curve: Interval(
        desde,
        (desde + largo).clamp(0.0, 1.0),
        curve: Curves.easeOutCubic,
      ),
    );
    return AnimatedBuilder(
      animation: curva,
      child: child,
      builder: (context, hijo) => IgnorePointer(
        ignoring: curva.value < 0.01,
        child: FadeTransition(
          opacity: curva,
          child: SlideTransition(
            // Sube, no baja: el contenido llega a su lugar en vez de caer sobre él.
            position: Tween(
              begin: const Offset(0, 0.06),
              end: Offset.zero,
            ).animate(curva),
            child: hijo,
          ),
        ),
      ),
    );
  }
}
