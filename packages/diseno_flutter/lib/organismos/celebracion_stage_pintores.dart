part of 'celebracion_stage.dart';

/// Estampilla superior inclinada (-6 grados).
class _EstampillaDeLogro extends StatelessWidget {
  const _EstampillaDeLogro({required this.tier});

  final InsigniaTier tier;

  @override
  Widget build(BuildContext context) {
    return Positioned(
      top: -14,
      child: Transform.rotate(
        angle: -0.1,
        child: Container(
          padding: const EdgeInsets.symmetric(
            horizontal: Espacio.s4,
            vertical: Espacio.s2,
          ),
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: [
                tier.colorPrimario,
                tier.colorPrimario.withValues(alpha: 0.85),
              ],
            ),
            borderRadius: BorderRadius.circular(Radios.pill),
            boxShadow: [
              BoxShadow(
                color: tier.colorPrimario.withValues(alpha: 0.5),
                blurRadius: 12,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Text(
            '¡NUEVO LOGRO!',
            style: Tipo.etiqueta.copyWith(
              color: Paleta.white,
              fontWeight: FontWeight.w900,
              letterSpacing: 1.5,
            ),
          ),
        ),
      ),
    );
  }
}

class _PintorDeRayos extends CustomPainter {
  const _PintorDeRayos({required this.angulo, required this.color});

  final double angulo;
  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final centro = Offset(size.width / 2, size.height / 2);
    final radio = math.max(size.width, size.height) * 0.95;
    const rayos = 16;
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.fill;

    canvas.save();
    canvas.translate(centro.dx, centro.dy);
    canvas.rotate(angulo);
    const paso = math.pi / rayos;

    for (var i = 0; i < rayos; i++) {
      final theta = (i * 2 * math.pi) / rayos;
      final path = Path()
        ..moveTo(0, 0)
        ..lineTo(radio * math.cos(theta), radio * math.sin(theta))
        ..lineTo(
          radio * math.cos(theta + paso * 0.6),
          radio * math.sin(theta + paso * 0.6),
        )
        ..close();
      canvas.drawPath(path, paint);
    }
    canvas.restore();
  }

  @override
  bool shouldRepaint(_PintorDeRayos old) =>
      old.angulo != angulo || old.color != color;
}

class _PintorDeParticulas extends CustomPainter {
  const _PintorDeParticulas({required this.progreso, required this.color});

  final double progreso;
  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final centro = Offset(size.width / 2, size.height / 2);
    final paint = Paint()..style = PaintingStyle.fill;

    const cantidad = 24;
    for (var i = 0; i < cantidad; i++) {
      final fase = (progreso + i / cantidad) % 1.0;
      final angulo = (i * 137.5) * (math.pi / 180);
      final distancia = 90 + fase * 130;
      final x = centro.dx + math.cos(angulo) * distancia;
      final y = centro.dy + math.sin(angulo) * distancia;
      final radio = (1.0 - fase) * 3.5;
      final opacidad = (math.sin(fase * math.pi)).clamp(0.0, 1.0);

      paint.color = color.withValues(alpha: opacidad * 0.85);
      canvas.drawCircle(Offset(x, y), radio, paint);
    }
  }

  @override
  bool shouldRepaint(_PintorDeParticulas old) => old.progreso != progreso;
}
