import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'dart:ui' as ui;

/// Los detalles del anverso del carnet de ejemplo: datos con rótulo, firma, chip,
/// holograma y zona de lectura mecánica. `u` es la centésima parte del ancho.

void pintarDatosDelCarnet(Canvas canvas, Rect carnet, double u) {
  final x = carnet.left + 34 * u;
  final rotulo = Paint()..color = Paleta.muted.withValues(alpha: 0.75);
  final valor = Paint()..color = Paleta.slate;
  const datos = [(15.5, 10.0, 34.0), (22.5, 14.0, 42.0), (29.5, 8.0, 22.0)];
  for (final (y, largoRotulo, largoValor) in datos) {
    canvas.drawRRect(
      RRect.fromLTRBR(
        x,
        carnet.top + y * u,
        x + largoRotulo * u,
        carnet.top + (y + 1.3) * u,
        Radius.circular(u),
      ),
      rotulo,
    );
    canvas.drawRRect(
      RRect.fromLTRBR(
        x,
        carnet.top + (y + 2.2) * u,
        x + largoValor * u,
        carnet.top + (y + 4.6) * u,
        Radius.circular(u),
      ),
      valor,
    );
  }
  final firma = Path()
    ..moveTo(x, carnet.top + 42 * u)
    ..cubicTo(
      x + 4 * u,
      carnet.top + 36 * u,
      x + 6 * u,
      carnet.top + 45 * u,
      x + 10 * u,
      carnet.top + 40 * u,
    )
    ..cubicTo(
      x + 13 * u,
      carnet.top + 36 * u,
      x + 15 * u,
      carnet.top + 44 * u,
      x + 20 * u,
      carnet.top + 40.5 * u,
    )
    ..quadraticBezierTo(
      x + 23 * u,
      carnet.top + 39 * u,
      x + 26 * u,
      carnet.top + 41 * u,
    );
  canvas.drawPath(
    firma,
    Paint()
      ..color = Paleta.info
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.6 * u
      ..strokeCap = StrokeCap.round,
  );
}

void pintarChipDelCarnet(Canvas canvas, Rect carnet, double u) {
  final chip = Rect.fromLTWH(
    carnet.left + 82 * u,
    carnet.top + 15 * u,
    11 * u,
    8.5 * u,
  );
  canvas.drawRRect(
    RRect.fromRectAndRadius(chip, Radius.circular(1.6 * u)),
    Paint()
      ..shader = ui.Gradient.linear(
        chip.topLeft,
        chip.bottomRight,
        [Paleta.o200, Paleta.o400, Paleta.o300],
        const [0, 0.5, 1],
      ),
  );
  final contacto = Paint()
    ..color = Paleta.o700.withValues(alpha: 0.5)
    ..style = PaintingStyle.stroke
    ..strokeWidth = 0.3 * u;
  canvas.drawLine(chip.centerLeft, chip.centerRight, contacto);
  for (final f in [0.33, 0.66]) {
    final cx = chip.left + chip.width * f;
    canvas.drawLine(Offset(cx, chip.top), Offset(cx, chip.bottom), contacto);
  }
  canvas.drawRRect(
    RRect.fromRectAndRadius(chip.deflate(2.6 * u), Radius.circular(0.8 * u)),
    contacto,
  );
}

/// Un sello iridiscente: cambia de color como los hologramas de verdad.
void pintarHologramaDelCarnet(Canvas canvas, Rect carnet, double u) {
  final centro = Offset(carnet.left + 86 * u, carnet.top + 40 * u);
  final radio = 5.5 * u;
  canvas.drawCircle(
    centro,
    radio,
    Paint()
      ..shader = ui.Gradient.sweep(
        centro,
        [
          Paleta.info.withValues(alpha: 0.45),
          Paleta.pii.withValues(alpha: 0.4),
          Paleta.g300.withValues(alpha: 0.5),
          Paleta.o300.withValues(alpha: 0.5),
          Paleta.info.withValues(alpha: 0.45),
        ],
        const [0, 0.25, 0.5, 0.75, 1],
      ),
  );
  canvas.drawCircle(
    centro,
    radio * 0.6,
    Paint()
      ..color = Paleta.white.withValues(alpha: 0.7)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.35 * u,
  );
}

/// Dos renglones de caracteres monoespaciados, con los «<» de relleno más tenues.
void pintarZonaDeLecturaDelCarnet(Canvas canvas, Rect carnet, double u) {
  final zona = Rect.fromLTRB(
    carnet.left,
    carnet.bottom - 12 * u,
    carnet.right,
    carnet.bottom,
  );
  canvas.drawRect(zona, Paint()..color = Paleta.white.withValues(alpha: 0.7));
  final caracter = Paint()..color = Paleta.slate.withValues(alpha: 0.8);
  final relleno = Paint()..color = Paleta.muted.withValues(alpha: 0.35);
  for (final (fila, y) in [2.5, 6.8].indexed) {
    for (var k = 0; k < 37; k++) {
      final esRelleno = (k + fila * 3) % 9 > 6 || k > 30 - fila * 4;
      canvas.drawRect(
        Rect.fromLTWH(
          zona.left + 5 * u + k * 2.45 * u,
          zona.top + y * u,
          1.6 * u,
          2.4 * u,
        ),
        esRelleno ? relleno : caracter,
      );
    }
  }
}
