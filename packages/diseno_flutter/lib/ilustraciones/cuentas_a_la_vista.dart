import 'package:flutter/material.dart';

import '../atomos/colores_de_turno.dart';
import '../tokens/tokens.dart';
import 'lienzo.dart';

/// **Tu rueda a la vista**: la lista de quién puso y quién cobró, abierta.
///
/// Una hoja con tres filas —persona, monto, tilde— y una lupa encima. Lo que se
/// dibuja no es «datos»: es el cuaderno del pasanaku, que siempre existió, ahora
/// legible por todos en vez de guardado por uno.
class CuentasALaVista extends CustomPainter {
  const CuentasALaVista(this.t);
  final Tokens t;

  @override
  void paint(Canvas canvas, Size size) {
    final l = Lienzo(canvas, size, t)..escenario();
    final hoja = Rect.fromCenter(
      center: l.centro - Offset(0, l.p(0.02)),
      width: l.p(0.60),
      height: l.p(0.56),
    );
    final radio = Radius.circular(l.p(0.055));

    canvas
      ..drawRRect(
        RRect.fromRectAndRadius(hoja, radio),
        l.relleno(t.surface),
      )
      ..drawRRect(
        RRect.fromRectAndRadius(hoja, radio),
        l.trazo(t.brand, l.p(0.022)),
      );

    // Tres filas: quién, cuánto, y el tilde de que ya está.
    final alto = hoja.height / 4;
    for (var i = 0; i < 3; i++) {
      final y = hoja.top + alto * (i + 1);
      final x = hoja.left + l.p(0.07);
      l.persona(Offset(x, y), l.p(0.075), ColoresDeTurno.de(i));
      canvas.drawLine(
        Offset(x + l.p(0.06), y),
        Offset(hoja.right - l.p(0.16), y),
        l.trazo(t.border, l.p(0.022)),
      );
      _tilde(canvas, l, Offset(hoja.right - l.p(0.085), y), l.p(0.035));
    }

    _lupa(canvas, l, l.centro + Offset(l.p(0.20), l.p(0.20)), l.p(0.105));
  }

  void _tilde(Canvas canvas, Lienzo l, Offset centro, double r) {
    final camino = Path()
      ..moveTo(centro.dx - r, centro.dy)
      ..lineTo(centro.dx - r * 0.25, centro.dy + r * 0.7)
      ..lineTo(centro.dx + r, centro.dy - r * 0.8);
    canvas.drawPath(camino, l.trazo(t.ok, l.p(0.028)));
  }

  /// La lupa apoyada sobre la hoja: lo que hay dentro se puede mirar de cerca.
  void _lupa(Canvas canvas, Lienzo l, Offset centro, double radio) {
    final grosor = l.p(0.032);
    canvas
      ..drawCircle(centro, radio, l.relleno(t.surface))
      ..drawCircle(centro, radio, l.trazo(t.accent, grosor))
      ..drawLine(
        centro + Offset(radio * 0.72, radio * 0.72),
        centro + Offset(radio * 1.55, radio * 1.55),
        l.trazo(t.accent, grosor),
      );
  }

  @override
  bool shouldRepaint(CuentasALaVista otro) => otro.t != t;
}
