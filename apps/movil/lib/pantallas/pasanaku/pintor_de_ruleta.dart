import 'dart:math' as math;

import 'package:aportaya_diseno/atomos/colores_de_turno.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// El disco que gira: un gajo por persona que **todavía no salió**, con luz propia (más
/// claro hacia el borde), sus clavijas y el nombre. El que acaba de salir brilla con
/// [resalte] y después se va: con [salida] de 0 a 1 se angosta hasta desaparecer y los
/// demás crecen a ocupar su lugar. En el resumen final ([puestos]) cada gajo lleva su
/// puesto.
class PintorDeRuleta extends CustomPainter {
  PintorDeRuleta({
    required this.nombres,
    required this.colores,
    required this.ganador,
    required this.resalte,
    this.salida = 0,
    this.puestos = const {},
  });

  final List<String> nombres;

  /// Índice de color de cada gajo: el de la persona, no el de su lugar en la ruleta.
  final List<int> colores;
  final int? ganador;
  final double resalte;
  final double salida;

  /// Gajo → puesto (1 = primero), para el resumen final.
  final Map<int, int> puestos;

  static Color _luz(Color base, double delta) {
    final hsl = HSLColor.fromColor(base);
    return hsl.withLightness((hsl.lightness + delta).clamp(0.0, 1.0)).toColor();
  }

  double _peso(int i) => i == ganador ? 1 - salida : 1;

  @override
  void paint(Canvas canvas, Size size) {
    final n = nombres.length;
    if (n == 0) return;
    final centro = size.center(Offset.zero);
    final r = size.width / 2;
    final rect = Rect.fromCircle(center: centro, radius: r);
    final total = List.generate(n, _peso).fold<double>(0, (a, b) => a + b);
    if (total <= 0) return;
    // El gajo 0 empieza en la aguja (arriba) y siguen en el sentido del reloj.
    final inicios = <double>[];
    var a = -math.pi / 2;
    for (var i = 0; i < n; i++) {
      inicios.add(a);
      a += 2 * math.pi * _peso(i) / total;
    }
    for (var i = 0; i < n; i++) {
      final barrido = 2 * math.pi * _peso(i) / total;
      if (barrido <= 0) continue;
      final base = ColoresDeTurno.de(colores[i]);
      canvas.drawArc(
        rect,
        inicios[i],
        barrido,
        true,
        Paint()
          ..shader = RadialGradient(
            colors: [_luz(base, -0.14), base, _luz(base, 0.1)],
            stops: const [0.15, 0.7, 1],
          ).createShader(rect),
      );
      final opacidad = i == ganador ? 1 - salida : 1.0;
      _rotulo(
        canvas,
        centro,
        r,
        inicios[i] + barrido / 2,
        barrido,
        i,
        opacidad,
      );
    }
    final linea = Paint()
      ..color = Paleta.white.withValues(alpha: 0.6)
      ..strokeWidth = r * 0.008;
    final clavija = Paint()..color = Paleta.crema;
    for (var i = 0; i < n && n > 1; i++) {
      final dir = Offset(math.cos(inicios[i]), math.sin(inicios[i]));
      canvas.drawLine(centro, centro + dir * r, linea);
      canvas.drawCircle(centro + dir * (r * 0.95), r * 0.022, clavija);
    }
    final g = ganador;
    if (g != null && g < n && resalte > 0 && salida < 1) {
      final borde = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = r * 0.03
        ..color = Paleta.white.withValues(alpha: resalte * (1 - salida))
        ..maskFilter = MaskFilter.blur(BlurStyle.solid, r * 0.02 * resalte);
      final barrido = 2 * math.pi * _peso(g) / total;
      canvas.drawArc(
        rect.deflate(r * 0.015),
        inicios[g],
        barrido,
        n > 1,
        borde,
      );
    }
  }

  void _rotulo(
    Canvas canvas,
    Offset centro,
    double r,
    double angulo,
    double barrido,
    int i,
    double opacidad,
  ) {
    if (opacidad <= 0.02) return;
    // El tamaño sigue al ancho del gajo: con doce personas no se pisan los nombres.
    final tamano = math.min(r * 0.085, r * barrido * 0.42);
    final puesto = puestos[i];
    final nombre = nombres[i].split(' ').first;
    final texto = TextPainter(
      text: TextSpan(
        text: puesto != null ? '$puesto.º $nombre' : nombre,
        style: Tipo.etiqueta.copyWith(
          fontSize: tamano,
          fontWeight: FontWeight.w700,
          color: Paleta.white.withValues(alpha: opacidad),
          shadows: [
            Shadow(
              color: Paleta.ink.withValues(alpha: 0.55 * opacidad),
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
      viejo.colores != colores ||
      viejo.ganador != ganador ||
      viejo.resalte != resalte ||
      viejo.salida != salida ||
      viejo.puestos != puestos;
}
