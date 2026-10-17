import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/widgets.dart';

/// Una persona ilustrada de frente, de busto, en el lienzo normalizado de 300 × 300
/// de `trazos_de_cara.dart`: la cabeza cae dentro de `ovaloDeRostro`. Es la imagen
/// de ayuda de «así va tu cara»; el óvalo solo es el encuadre, nunca la persona.
///
/// Colores fijos de la paleta (es una ilustración, igual en los dos temas).
/// Con [silueta] se pinta toda de un color, para el contraluz. [giro] (-1 a 1)
/// gira la cabeza: los rasgos se corren hacia ese lado y se oculta la oreja de
/// enfrente (-1 = hacia la izquierda de la imagen).
void pintarPersonaIlustrada(
  Canvas canvas, {
  bool conAccesorios = false,
  Color? silueta,
  double giro = 0,
}) {
  if (silueta != null) {
    canvas.saveLayer(
      const Rect.fromLTWH(0, 0, 300, 300),
      Paint()..colorFilter = ColorFilter.mode(silueta, BlendMode.srcIn),
    );
  }
  final piel = Paint()..color = Paleta.o200;
  final sombra = Paint()..color = Paleta.o300;
  final pelo = Paint()..color = Paleta.ink;

  // Camisa y cuello.
  canvas.drawPath(
    Path()
      ..moveTo(28, 300)
      ..cubicTo(34, 246, 92, 222, 150, 222)
      ..cubicTo(208, 222, 266, 246, 272, 300)
      ..close(),
    Paint()..color = Paleta.g600,
  );
  canvas.drawRect(const Rect.fromLTWH(129, 178, 42, 50), sombra);
  canvas.drawPath(
    Path()
      ..moveTo(126, 222)
      ..lineTo(150, 252)
      ..lineTo(174, 222)
      ..close(),
    piel,
  );

  // Orejas y cara con mandíbula.
  if (giro >= 0) canvas.drawOval(const Rect.fromLTWH(91, 118, 20, 34), piel);
  if (giro <= 0) canvas.drawOval(const Rect.fromLTWH(189, 118, 20, 34), piel);
  canvas.drawPath(
    Path()
      ..moveTo(101, 100)
      ..cubicTo(101, 60, 199, 60, 199, 100)
      ..lineTo(199, 140)
      ..cubicTo(199, 182, 174, 206, 150, 206)
      ..cubicTo(126, 206, 101, 182, 101, 140)
      ..close(),
    piel,
  );

  // Pelo.
  canvas.drawPath(
    Path()
      ..moveTo(96, 132)
      ..cubicTo(86, 70, 120, 46, 152, 46)
      ..cubicTo(192, 46, 216, 74, 204, 132)
      ..cubicTo(200, 108, 192, 94, 178, 88)
      ..cubicTo(156, 100, 124, 100, 106, 94)
      ..cubicTo(102, 106, 98, 118, 96, 132)
      ..close(),
    pelo,
  );

  // Rasgos, corridos hacia donde mira.
  canvas.save();
  canvas.translate(giro * 22, 0);
  final trazo = Paint()
    ..color = Paleta.ink
    ..style = PaintingStyle.stroke
    ..strokeCap = StrokeCap.round
    ..strokeWidth = 4;
  canvas.drawLine(const Offset(118, 121), const Offset(138, 118), trazo);
  canvas.drawLine(const Offset(162, 118), const Offset(182, 121), trazo);
  canvas.drawCircle(const Offset(129, 136), 5, pelo);
  canvas.drawCircle(const Offset(171, 136), 5, pelo);
  canvas.drawPath(
    Path()
      ..moveTo(150, 142)
      ..lineTo(145, 162)
      ..lineTo(154, 163),
    trazo
      ..color = Paleta.o400
      ..strokeWidth = 3,
  );
  canvas.drawArc(
    const Rect.fromLTWH(134, 166, 32, 16),
    0.25,
    2.64,
    false,
    trazo
      ..color = Paleta.o700
      ..strokeWidth = 4,
  );

  if (conAccesorios) _pintarAccesorios(canvas);
  canvas.restore();
  if (silueta != null) canvas.restore();
}

/// Lentes oscuros y una gorra con visera.
void _pintarAccesorios(Canvas canvas) {
  final oscuro = Paint()..color = Paleta.ink;
  for (final x in [110.0, 154.0]) {
    canvas.drawRRect(
      RRect.fromLTRBR(x, 124, x + 36, 148, const Radius.circular(10)),
      oscuro,
    );
  }
  canvas.drawRect(const Rect.fromLTWH(144, 130, 12, 4), oscuro);
  final gorra = Paint()..color = Paleta.g700;
  canvas.drawPath(
    Path()
      ..moveTo(92, 100)
      ..cubicTo(90, 50, 210, 50, 208, 100)
      ..close(),
    gorra,
  );
  canvas.drawRRect(
    RRect.fromLTRBR(84, 94, 226, 108, const Radius.circular(7)),
    gorra,
  );
}
