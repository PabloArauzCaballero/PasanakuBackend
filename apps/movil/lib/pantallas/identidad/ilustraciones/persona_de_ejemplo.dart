import 'dart:ui' as ui;

import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/widgets.dart';

/// La persona de los ejemplos: un muñeco genérico con volumen (pelo, orejas, cuello,
/// remera con cuello en V y rasgos simples), dibujado en el lienzo normalizado de
/// 300 × 300 de `trazos_de_cara.dart`. No representa a nadie real.
///
/// La usan la escena del selfie y la foto chica del carnet, así las dos muestran la
/// misma persona.
void pintarPersona(Canvas canvas, {bool conAccesorios = false}) {
  _pintarTorso(canvas);
  _pintarCuello(canvas);
  _pintarOrejas(canvas);
  _pintarCabeza(canvas);
  _pintarRasgos(canvas);
  _pintarPelo(canvas);
  if (conAccesorios) _pintarAccesorios(canvas);
}

/// El óvalo de la cabeza, que también recortan el contraluz y la gorra.
const cabezaDePersona = Rect.fromLTWH(100, 62, 100, 130);

void _pintarTorso(Canvas canvas) {
  final torso = Path()
    ..moveTo(30, 300)
    ..cubicTo(34, 238, 86, 208, 150, 208)
    ..cubicTo(214, 208, 266, 238, 270, 300)
    ..close();
  canvas.drawPath(
    torso,
    Paint()
      ..shader = ui.Gradient.linear(
        const Offset(150, 208),
        const Offset(150, 300),
        [Paleta.g500, Paleta.g700],
      ),
  );
  // Luz de costado sobre el hombro izquierdo.
  canvas.save();
  canvas.clipPath(torso);
  canvas.drawOval(
    const Rect.fromLTWH(48, 214, 90, 70),
    Paint()
      ..color = Paleta.g400.withValues(alpha: 0.45)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 14),
  );
  canvas.restore();
  // El escote en V deja ver la piel, y el cuello de la remera lo bordea.
  canvas.drawPath(
    Path()
      ..moveTo(130, 207)
      ..lineTo(150, 238)
      ..lineTo(170, 207)
      ..close(),
    Paint()..color = Paleta.o200,
  );
  final solapa = Paint()..color = Paleta.g100;
  canvas.drawPath(
    Path()
      ..moveTo(120, 206)
      ..lineTo(130, 206)
      ..lineTo(150, 238)
      ..lineTo(146, 246)
      ..close(),
    solapa,
  );
  canvas.drawPath(
    Path()
      ..moveTo(180, 206)
      ..lineTo(170, 206)
      ..lineTo(150, 238)
      ..lineTo(154, 246)
      ..close(),
    solapa,
  );
}

void _pintarCuello(Canvas canvas) {
  const cuello = Rect.fromLTWH(131, 166, 38, 46);
  canvas.drawRRect(
    RRect.fromRectAndRadius(cuello, const Radius.circular(10)),
    Paint()
      ..shader = ui.Gradient.linear(
        cuello.topCenter,
        cuello.bottomCenter,
        [Paleta.o300, Paleta.o200],
        const [0.35, 1],
      ),
  );
  // Sombra del mentón sobre el cuello; la cabeza se pinta encima.
  canvas.save();
  canvas.clipRect(cuello);
  canvas.drawOval(
    cabezaDePersona.translate(0, 10),
    Paint()
      ..color = Paleta.o600.withValues(alpha: 0.35)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 4),
  );
  canvas.restore();
}

void _pintarOrejas(Canvas canvas) {
  final oreja = Paint()..color = Paleta.o300;
  final interior = Paint()..color = Paleta.o400.withValues(alpha: 0.6);
  for (final x in [100.0, 200.0]) {
    canvas.drawOval(
      Rect.fromCenter(center: Offset(x, 132), width: 18, height: 30),
      oreja,
    );
    canvas.drawOval(
      Rect.fromCenter(center: Offset(x, 133), width: 8, height: 16),
      interior,
    );
  }
}

void _pintarCabeza(Canvas canvas) {
  canvas.drawOval(
    cabezaDePersona,
    Paint()
      ..shader = ui.Gradient.radial(
        const Offset(138, 112),
        78,
        [Paleta.o100, Paleta.o200, Paleta.o300],
        const [0, 0.55, 1],
      ),
  );
}

void _pintarRasgos(Canvas canvas) {
  final trazo = Paint()
    ..style = PaintingStyle.stroke
    ..strokeCap = StrokeCap.round;
  // Cejas.
  for (final s in [-1.0, 1.0]) {
    canvas.drawPath(
      Path()
        ..moveTo(150 + s * 9, 113)
        ..quadraticBezierTo(150 + s * 20, 106, 150 + s * 31, 112),
      Paint.from(trazo)
        ..color = Paleta.slate
        ..strokeWidth = 4,
    );
  }
  // Ojos con un brillo, para que miren.
  for (final x in [131.0, 169.0]) {
    canvas.drawOval(
      Rect.fromCenter(center: Offset(x, 129), width: 9, height: 11),
      Paint()..color = Paleta.ink,
    );
    canvas.drawCircle(
      Offset(x + 1.8, 126.5),
      1.8,
      Paint()..color = Paleta.white,
    );
  }
  // Mejillas.
  for (final x in [124.0, 176.0]) {
    canvas.drawCircle(
      Offset(x, 152),
      9,
      Paint()
        ..color = Paleta.o400.withValues(alpha: 0.3)
        ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 4),
    );
  }
  // Nariz.
  canvas.drawPath(
    Path()
      ..moveTo(151, 132)
      ..quadraticBezierTo(146, 147, 145, 152)
      ..quadraticBezierTo(150, 156, 155, 153),
    Paint.from(trazo)
      ..color = Paleta.o600.withValues(alpha: 0.6)
      ..strokeWidth = 2.6,
  );
  // Sonrisa.
  canvas.drawPath(
    Path()
      ..moveTo(137, 166)
      ..quadraticBezierTo(150, 176, 163, 166),
    Paint.from(trazo)
      ..color = Paleta.o700
      ..strokeWidth = 3.4,
  );
}

void _pintarPelo(Canvas canvas) {
  final pelo = Path()
    ..moveTo(96, 140)
    ..cubicTo(86, 74, 118, 48, 150, 50)
    ..cubicTo(184, 48, 214, 74, 204, 140)
    ..cubicTo(200, 116, 194, 100, 182, 92)
    ..cubicTo(166, 102, 136, 96, 122, 88)
    ..cubicTo(108, 98, 100, 114, 96, 140)
    ..close();
  canvas.drawPath(
    pelo,
    Paint()
      ..shader = ui.Gradient.linear(
        const Offset(120, 50),
        const Offset(180, 140),
        [Paleta.slate, Paleta.ink],
      ),
  );
  // Un brillo sobre el pelo.
  canvas.drawPath(
    Path()
      ..moveTo(118, 66)
      ..quadraticBezierTo(140, 56, 164, 60),
    Paint()
      ..color = Paleta.muted.withValues(alpha: 0.7)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 4
      ..strokeCap = StrokeCap.round,
  );
}

/// Lentes oscuros con reflejo y una gorra con visera.
void _pintarAccesorios(Canvas canvas) {
  final gorra = Path()
    ..addArc(cabezaDePersona.inflate(6), 3.14159, 3.14159)
    ..close();
  canvas.save();
  canvas.clipRect(const Rect.fromLTWH(0, 0, 300, 100));
  canvas.drawPath(
    gorra,
    Paint()
      ..shader = ui.Gradient.linear(
        const Offset(150, 56),
        const Offset(150, 100),
        [Paleta.g500, Paleta.g800],
      ),
  );
  canvas.restore();
  canvas.drawCircle(const Offset(150, 58), 4, Paint()..color = Paleta.g800);
  canvas.drawRRect(
    RRect.fromLTRBR(88, 90, 226, 102, const Radius.circular(6)),
    Paint()
      ..shader = ui.Gradient.linear(
        const Offset(150, 90),
        const Offset(150, 102),
        [Paleta.g600, Paleta.g900],
      ),
  );
  for (final x in [111.0, 154.0]) {
    final lente = RRect.fromLTRBR(
      x,
      117,
      x + 35,
      140,
      const Radius.circular(10),
    );
    canvas.drawRRect(
      lente,
      Paint()
        ..shader = ui.Gradient.linear(Offset(x, 117), Offset(x + 35, 140), [
          Paleta.slate,
          Paleta.ink,
        ]),
    );
    canvas.drawLine(
      Offset(x + 8, 122),
      Offset(x + 15, 122),
      Paint()
        ..color = Paleta.white.withValues(alpha: 0.6)
        ..strokeWidth = 2.5
        ..strokeCap = StrokeCap.round,
    );
  }
  canvas.drawRect(
    const Rect.fromLTWH(146, 124, 8, 4),
    Paint()..color = Paleta.ink,
  );
}
