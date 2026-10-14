import 'package:flutter/material.dart';

import '../atomos/movimiento.dart';
import '../tokens/tokens.dart';

/// Confirmación sobria de cuenta creada: marca que crece y tilde que se dibuja.
class CelebracionDeExito extends StatefulWidget {
  const CelebracionDeExito({super.key, required this.onCompleto});

  final VoidCallback onCompleto;

  @override
  State<CelebracionDeExito> createState() => _CelebracionDeExitoState();
}

class _CelebracionDeExitoState extends State<CelebracionDeExito>
    with SingleTickerProviderStateMixin {
  var _iniciada = false;
  var _notificada = false;
  late final AnimationController _control = AnimationController(
    vsync: this,
    duration: Movimiento.celebracionDeAlta,
  )..addStatusListener(_alCompletar);
  late final Animation<double> _escala = CurvedAnimation(
    parent: _control,
    curve: Curves.easeOutBack,
  );
  late final Animation<double> _trazo = CurvedAnimation(
    parent: _control,
    curve: const Interval(0.28, 1, curve: Movimiento.llega),
  );

  void _alCompletar(AnimationStatus estado) {
    if (estado != AnimationStatus.completed || _notificada) return;
    _notificada = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) widget.onCompleto();
    });
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_iniciada) return;
    _iniciada = true;
    if (reducirMovimiento(context)) {
      _control.value = 1;
    } else {
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
    final t = Tokens.of(context);
    return Semantics(
      liveRegion: true,
      label: 'Cuenta creada',
      child: Center(
        child: AnimatedBuilder(
          animation: _control,
          builder: (context, _) => Transform.scale(
            scale: _escala.value.clamp(0.0, 1.12),
            child: Container(
              width: 96,
              height: 96,
              alignment: Alignment.center,
              decoration: BoxDecoration(color: t.brand, shape: BoxShape.circle),
              child: CustomPaint(
                size: const Size(48, 48),
                painter: _TildeDeExito(_trazo.value, t.brandTexto),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _TildeDeExito extends CustomPainter {
  const _TildeDeExito(this.progreso, this.color);

  final double progreso;
  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final trazo = Path()
      ..moveTo(size.width * 0.16, size.height * 0.53)
      ..lineTo(size.width * 0.41, size.height * 0.77)
      ..lineTo(size.width * 0.86, size.height * 0.25);
    final medida = trazo.computeMetrics().first;
    final visible = medida.extractPath(0, medida.length * progreso);
    canvas.drawPath(
      visible,
      Paint()
        ..color = color
        ..style = PaintingStyle.stroke
        ..strokeWidth = 5
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round,
    );
  }

  @override
  bool shouldRepaint(_TildeDeExito oldDelegate) =>
      progreso != oldDelegate.progreso || color != oldDelegate.color;
}
