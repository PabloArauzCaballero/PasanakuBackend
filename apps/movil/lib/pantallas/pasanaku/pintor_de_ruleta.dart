import 'dart:math' as math;

import 'package:aportaya_diseno/atomos/colores_de_turno.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// El disco que gira: un gajo por integrante con luz propia (más claro hacia el borde),
/// sus clavijas y el nombre. Los gajos ya sorteados se apagan y llevan su puesto; el que
/// acaba de salir brilla con [resalte].
class PintorDeRuleta extends CustomPainter {
  PintorDeRuleta({
    required this.nombres,
    required this.puestos,
    required this.ganador,
    required this.resalte,
  });

  final List<String> nombres;

  /// Gajo → puesto que ya le salió (1 = primero).
  final Map<int, int> puestos;
  final int? ganador;
  final double resalte;

  static Color _luz(Color base, double delta) {
    final hsl = HSLColor.fromColor(base);
    return hsl.withLightness((hsl.lightness + delta).clamp(0.0, 1.0)).toColor();
  }

  @override
  void paint(Canvas canvas, Size size) {
    final n = nombres.length;
    if (n == 0) return;
    final centro = size.center(Offset.zero);
    final r = size.width / 2;
    final paso = 2 * math.pi / n;
    final rect = Rect.fromCircle(center: centro, radius: r);
    for (var i = 0; i < n; i++) {
      // El gajo 0 empieza en la aguja (arriba) y siguen en el sentido del reloj.
      final inicio = -math.pi / 2 + i * paso;
      final base = ColoresDeTurno.de(i);
      final apagado = puestos.containsKey(i) && ganador != i;
      canvas.drawArc(
        rect,
        inicio,
        paso,
        true,
        Paint()
          ..shader = RadialGradient(
            colors: [_luz(base, -0.14), base, _luz(base, 0.1)],
            stops: const [0.15, 0.7, 1],
          ).createShader(rect),
      );
      if (apagado) {
        canvas.drawArc(
          rect,
          inicio,
          paso,
          true,
          Paint()..color = Paleta.ink.withValues(alpha: 0.5),
        );
      }
      _rotulo(canvas, centro, r, inicio + paso / 2, paso, i, apagado);
    }
    final linea = Paint()
      ..color = Paleta.white.withValues(alpha: 0.6)
      ..strokeWidth = r * 0.008;
    final clavija = Paint()..color = Paleta.crema;
    for (var i = 0; i < n; i++) {
      final a = -math.pi / 2 + i * paso;
      final dir = Offset(math.cos(a), math.sin(a));
      canvas.drawLine(centro, centro + dir * r, linea);
      canvas.drawCircle(centro + dir * (r * 0.95), r * 0.022, clavija);
    }
    final g = ganador;
    if (g != null && resalte > 0) {
      final inicio = -math.pi / 2 + g * paso;
      final borde = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = r * 0.03
        ..color = Paleta.white.withValues(alpha: resalte)
        ..maskFilter = MaskFilter.blur(BlurStyle.solid, r * 0.02 * resalte);
      canvas.drawArc(rect.deflate(r * 0.015), inicio, paso, true, borde);
    }
  }

  void _rotulo(
    Canvas canvas,
    Offset centro,
    double r,
    double angulo,
    double paso,
    int i,
    bool apagado,
  ) {
    // El tamaño sigue al ancho del gajo: con doce personas no se pisan los nombres.
    final tamano = math.min(r * 0.085, r * paso * 0.42);
    final puesto = puestos[i];
    final texto = TextPainter(
      text: TextSpan(
        text: puesto != null
            ? '$puesto.º ${nombres[i].split(' ').first}'
            : nombres[i].split(' ').first,
        style: Tipo.etiqueta.copyWith(
          fontSize: tamano,
          fontWeight: FontWeight.w700,
          color: Paleta.white.withValues(alpha: apagado ? 0.75 : 1),
          shadows: [
            Shadow(
              color: Paleta.ink.withValues(alpha: 0.55),
              blurRadius: r * 0.02,
            ),
          ],
        ),
      ),
      textDirection: TextDirection.ltr,
      maxLines: 1,
      ellipsis: '…',
    )..layout(maxWidth: r * 0.56);
    canvas.save();
    canvas.translate(centro.dx, centro.dy);
    canvas.rotate(angulo);
    texto.paint(canvas, Offset(r * 0.3, -texto.height / 2));
    canvas.restore();
  }

  @override
  bool shouldRepaint(PintorDeRuleta viejo) =>
      viejo.nombres != nombres ||
      viejo.puestos != puestos ||
      viejo.ganador != ganador ||
      viejo.resalte != resalte;
}
