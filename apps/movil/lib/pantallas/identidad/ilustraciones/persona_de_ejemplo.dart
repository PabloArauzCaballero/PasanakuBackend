import 'dart:ui' as ui;

import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/widgets.dart';

import 'accesorios_de_persona.dart';
import 'rasgos_de_persona.dart';

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
  pintarRasgosDePersona(canvas);
  pintarPeloDePersona(canvas);
  if (conAccesorios) pintarAccesoriosDePersona(canvas);
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
