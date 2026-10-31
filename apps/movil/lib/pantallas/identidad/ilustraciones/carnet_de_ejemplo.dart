import 'dart:math' as math;
import 'dart:ui' as ui;

import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'detalles_del_carnet.dart';
import 'persona_de_ejemplo.dart';

/// El carnet de ejemplo de las pautas: genérico, sin copiar ningún documento real.
/// Se dibuja en [carnet] (proporción ID-1); `u` es la centésima parte de su ancho.

/// El contorno del carnet, con el radio de esquina de una tarjeta ID-1 (3,18 mm).
RRect contornoDeCarnet(Rect carnet) => RRect.fromRectAndRadius(
  carnet,
  Radius.circular(carnet.width * 3.18 / 85.6),
);

void pintarCarnet(Canvas canvas, Rect carnet) {
  final u = carnet.width / 100;
  final contorno = contornoDeCarnet(carnet);
  // Sombra sobre la mesa: el carnet está apoyado, no flotando.
  canvas.drawRRect(
    contorno.shift(Offset(0, 1.6 * u)),
    Paint()
      ..color = Paleta.ink.withValues(alpha: 0.6)
      ..maskFilter = ui.MaskFilter.blur(BlurStyle.normal, 2.4 * u),
  );
  canvas.drawRRect(
    contorno,
    Paint()
      ..shader = ui.Gradient.linear(
        carnet.topLeft,
        carnet.bottomRight,
        [Paleta.white, Paleta.cloud, Paleta.g100],
        const [0, 0.5, 1],
      ),
  );
  canvas.save();
  canvas.clipRRect(contorno);
  _pintarFondoDeSeguridad(canvas, carnet, u);
  _pintarEncabezado(canvas, carnet, u);
  pintarZonaDeLecturaDelCarnet(canvas, carnet, u);
  canvas.restore();
  _pintarFoto(canvas, carnet, u);
  pintarDatosDelCarnet(canvas, carnet, u);
  pintarChipDelCarnet(canvas, carnet, u);
  pintarHologramaDelCarnet(canvas, carnet, u);
  // Canto del plástico: un filo de luz y uno de sombra.
  canvas.drawRRect(
    contorno,
    Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.5 * u
      ..shader = ui.Gradient.linear(carnet.topLeft, carnet.bottomRight, [
        Paleta.white,
        Paleta.muted.withValues(alpha: 0.5),
      ]),
  );
}

/// Ondas finas y una roseta, como el fondo de seguridad de un documento.
void _pintarFondoDeSeguridad(Canvas canvas, Rect carnet, double u) {
  final linea = Paint()
    ..style = PaintingStyle.stroke
    ..strokeWidth = 0.3 * u
    ..color = Paleta.g300.withValues(alpha: 0.45);
  for (var i = 0; i < 9; i++) {
    final base = carnet.top + 15 * u + i * 4.5 * u;
    final onda = Path()..moveTo(carnet.left, base);
    for (var x = 0.0; x <= 100; x += 2) {
      onda.lineTo(
        carnet.left + x * u,
        base + math.sin((x / 14 + i * 0.6) * math.pi) * 1.4 * u,
      );
    }
    canvas.drawPath(onda, linea);
  }
  final centro = Offset(carnet.left + 68 * u, carnet.top + 36 * u);
  for (var r = 4.0; r <= 22; r += 3) {
    canvas.drawCircle(centro, r * u, linea);
  }
}

void _pintarEncabezado(Canvas canvas, Rect carnet, double u) {
  final banda = Rect.fromLTWH(carnet.left, carnet.top, carnet.width, 11 * u);
  canvas.drawRect(
    banda,
    Paint()
      ..shader = ui.Gradient.linear(banda.centerLeft, banda.centerRight, [
        Paleta.g500,
        Paleta.g700,
      ]),
  );
  final escudo = Offset(carnet.left + 7 * u, carnet.top + 5.5 * u);
  canvas.drawCircle(escudo, 3.4 * u, Paint()..color = Paleta.o400);
  canvas.drawCircle(
    escudo,
    2.2 * u,
    Paint()
      ..color = Paleta.white
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.5 * u,
  );
  final texto = Paint()..color = Paleta.white.withValues(alpha: 0.95);
  canvas.drawRRect(
    RRect.fromLTRBR(
      carnet.left + 13 * u,
      carnet.top + 2.8 * u,
      carnet.left + 50 * u,
      carnet.top + 5 * u,
      Radius.circular(u),
    ),
    texto,
  );
  canvas.drawRRect(
    RRect.fromLTRBR(
      carnet.left + 13 * u,
      carnet.top + 6.6 * u,
      carnet.left + 36 * u,
      carnet.top + 8.2 * u,
      Radius.circular(u),
    ),
    Paint()..color = Paleta.white.withValues(alpha: 0.6),
  );
}

void _pintarFoto(Canvas canvas, Rect carnet, double u) {
  final foto = Rect.fromLTWH(
    carnet.left + 5 * u,
    carnet.top + 15 * u,
    24 * u,
    30 * u,
  );
  final marco = RRect.fromRectAndRadius(foto, Radius.circular(1.5 * u));
  canvas.save();
  canvas.clipRRect(marco);
  canvas.drawRect(
    foto,
    Paint()
      ..shader = ui.Gradient.linear(foto.topCenter, foto.bottomCenter, [
        Paleta.infobg,
        Paleta.line,
      ]),
  );
  // La misma persona del selfie, achicada al tamaño de la foto.
  final escala = foto.height / 270;
  canvas.translate(foto.center.dx - 150 * escala, foto.bottom - 300 * escala);
  canvas.scale(escala);
  pintarPersona(canvas);
  canvas.restore();
  canvas.drawRRect(
    marco,
    Paint()
      ..color = Paleta.muted.withValues(alpha: 0.5)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.3 * u,
  );
}

/// Cada dato con su rótulo chico encima, y la firma debajo.
