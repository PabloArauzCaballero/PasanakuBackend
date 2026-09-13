import 'dart:ui' as ui;

import 'package:flutter/material.dart';

import '../atomos/colores_de_turno.dart';
import '../tokens/tokens.dart';
import 'lienzo.dart';

/// **Tu rueda a la vista**: la lista de quién puso y quién cobró, abierta.
///
/// Una hoja apoyada sobre el escenario con tres filas —persona, monto, tilde— y una
/// lupa de vidrio encima. Lo que se dibuja no es «datos»: es el cuaderno del pasanaku,
/// que siempre existió, ahora legible por todos en vez de guardado por uno.
class CuentasALaVista extends CustomPainter {
  const CuentasALaVista(this.t);
  final Tokens t;

  @override
  void paint(Canvas canvas, Size size) {
    final l = Lienzo(canvas, size, t)..escenario();
    final hoja = Rect.fromCenter(
      center: l.centro - Offset(l.p(0.015), l.p(0.03)),
      width: l.p(0.585),
      height: l.p(0.545),
    );
    final radio = Radius.circular(l.p(0.055));
    final forma = RRect.fromRectAndRadius(hoja, radio);

    l.sombra(Path()..addRRect(forma), desplazamiento: 0.030, difuso: 0.036);
    canvas
      ..drawRRect(
        forma,
        Paint()
          ..shader = ui.Gradient.linear(
            hoja.topCenter,
            hoja.bottomCenter,
            [t.surface, Color.lerp(t.surface, t.brandBg, 0.35)!],
          ),
      )
      ..drawRRect(forma, l.trazo(t.brand, l.p(0.020)));

    // La pestaña de arriba: convierte un rectángulo en un cuaderno.
    final pestana = RRect.fromRectAndRadius(
      Rect.fromCenter(
        center: Offset(hoja.center.dx, hoja.top),
        width: hoja.width * 0.34,
        height: l.p(0.045),
      ),
      Radius.circular(l.p(0.022)),
    );
    canvas.drawRRect(pestana, l.relleno(t.brand));

    final alto = (hoja.height - l.p(0.05)) / 3.4;
    for (var i = 0; i < 3; i++) {
      final y = hoja.top + l.p(0.085) + alto * (i + 0.55);
      final x = hoja.left + l.p(0.072);
      l.persona(Offset(x, y), l.p(0.072), ColoresDeTurno.de(i));
      // El monto, como un renglón escrito: dos tramos de distinto largo.
      canvas
        ..drawLine(
          Offset(x + l.p(0.062), y - l.p(0.018)),
          Offset(hoja.right - l.p(0.175), y - l.p(0.018)),
          l.trazo(t.text3.withValues(alpha: 0.55), l.p(0.019)),
        )
        ..drawLine(
          Offset(x + l.p(0.062), y + l.p(0.026)),
          Offset(hoja.right - l.p(0.255), y + l.p(0.026)),
          l.trazo(t.border, l.p(0.015)),
        );
      _tilde(canvas, l, Offset(hoja.right - l.p(0.082), y), l.p(0.036));
    }

    _lupa(canvas, l, hoja.bottomRight + Offset(l.p(0.01), -l.p(0.02)), l.p(0.115));
  }

  void _tilde(Canvas canvas, Lienzo l, Offset centro, double r) {
    canvas.drawCircle(centro, r * 1.15, l.relleno(t.okBg));
    canvas.drawPath(
      Path()
        ..moveTo(centro.dx - r * 0.52, centro.dy)
        ..lineTo(centro.dx - r * 0.10, centro.dy + r * 0.42)
        ..lineTo(centro.dx + r * 0.56, centro.dy - r * 0.44),
      l.trazo(t.okTexto, l.p(0.024)),
    );
  }

  /// La lupa apoyada sobre la hoja, con el vidrio hecho de luz: lo que hay dentro se
  /// puede mirar de cerca.
  void _lupa(Canvas canvas, Lienzo l, Offset centro, double radio) {
    final grosor = l.p(0.036);
    final mango = Path()
      ..moveTo(centro.dx + radio * 0.70, centro.dy + radio * 0.70)
      ..lineTo(centro.dx + radio * 1.62, centro.dy + radio * 1.62);
    l.sombra(
      Path()..addOval(Rect.fromCircle(center: centro, radius: radio)),
      desplazamiento: 0.024,
      difuso: 0.030,
    );
    canvas
      ..drawPath(mango, l.trazo(Color.lerp(t.accent, Paleta.g900, 0.25)!, grosor * 1.15))
      ..drawCircle(
        centro,
        radio,
        Paint()
          ..shader = ui.Gradient.linear(
            Offset(centro.dx - radio, centro.dy - radio),
            Offset(centro.dx + radio, centro.dy + radio),
            [
              t.surface.withValues(alpha: 0.95),
              t.brandBg.withValues(alpha: 0.75),
            ],
          ),
      )
      ..drawCircle(centro, radio, l.trazo(t.accent, grosor));
    l.brillo(centro - Offset(radio * 0.38, radio * 0.40), radio * 0.62);
  }

  @override
  bool shouldRepaint(CuentasALaVista otro) => otro.t != t;
}
