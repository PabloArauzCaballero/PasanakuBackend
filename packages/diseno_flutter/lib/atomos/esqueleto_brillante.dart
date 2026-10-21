import 'package:flutter/material.dart';

import 'movimiento.dart';
import '../tokens/tokens.dart';

/// Destello tenue sobre placeholders; se detiene cuando el sistema reduce movimiento.
class EsqueletoBrillante extends StatefulWidget {
  const EsqueletoBrillante({super.key, required this.child});

  final Widget child;

  @override
  State<EsqueletoBrillante> createState() => _EsqueletoBrillanteState();
}

class _EsqueletoBrillanteState extends State<EsqueletoBrillante>
    with SingleTickerProviderStateMixin {
  late final AnimationController _control = AnimationController(
    vsync: this,
    duration: Movimiento.brillo,
  );

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (reducirMovimiento(context)) {
      _control.stop();
      _control.value = 0;
    } else if (!_control.isAnimating) {
      _control.repeat();
    }
  }

  @override
  void dispose() {
    _control.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (reducirMovimiento(context)) return widget.child;
    final t = Tokens.of(context);
    return RepaintBoundary(
      child: AnimatedBuilder(
        animation: _control,
        child: widget.child,
        builder: (context, child) => ShaderMask(
          blendMode: BlendMode.srcATop,
          shaderCallback: (bounds) {
            final avance = _control.value * 2 - 1;
            return LinearGradient(
              begin: Alignment(avance - 0.7, 0),
              end: Alignment(avance + 0.7, 0),
              colors: [t.surface2, t.surface, t.surface2],
              stops: const [0.2, 0.5, 0.8],
            ).createShader(bounds);
          },
          child: child,
        ),
      ),
    );
  }
}
