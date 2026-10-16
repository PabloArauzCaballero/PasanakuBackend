import 'dart:ui' as ui;

import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/widgets.dart';

import '../dominio/pautas_de_fotos.dart';
import '../pasos_alta/trazos_de_cara.dart';

/// Ejemplo de foto de rostro: un busto neutro, sin rasgos, dentro del mismo óvalo
/// que muestra la cámara. Se dibuja en el lienzo normalizado de 300 × 300 de
/// `trazos_de_cara.dart` y se escala al alto de la escena.
void pintarEscenaDeRostro(Canvas canvas, Size size, EscenaDePauta escena) {
  final contraluz = escena == EscenaDePauta.rostroContraluz;
  final fondo = Offset.zero & size;
  canvas.drawRect(fondo, Paint()..color = Paleta.line);
  canvas.save();
  final escala = size.height / 300;
  canvas.translate(size.width / 2 - 150 * escala, 0);
  canvas.scale(escala);
  if (contraluz) {
    // Una ventana encendida detrás: el fondo quema y la persona queda oscura.
    canvas.drawCircle(
      const Offset(150, 120),
      190,
      Paint()
        ..shader = ui.Gradient.radial(
          const Offset(150, 120),
          190,
          [Paleta.white, Paleta.white, Paleta.line],
          const [0, 0.45, 1],
        ),
    );
  }
  canvas.save();
  if (escena == EscenaDePauta.rostroLadeado) {
    // Cabeza inclinada y corrida hacia afuera del óvalo.
    canvas.translate(70, 10);
    canvas.rotate(0.3);
  }
  final persona = Paint()..color = contraluz ? Paleta.slate : Paleta.muted;
  canvas.drawPath(_busto(), persona);
  canvas.drawOval(_cabeza, persona);
  if (escena == EscenaDePauta.rostroConAccesorios) _pintarAccesorios(canvas);
  canvas.restore();
  // El mismo velo que la cámara pone afuera del óvalo.
  canvas.drawPath(
    Path.combine(
      PathOperation.difference,
      Path()..addRect(const Rect.fromLTWH(-300, 0, 900, 300)),
      contornoDeRostro(),
    ),
    Paint()..color = Paleta.ink.withValues(alpha: 0.35),
  );
  canvas.drawPath(
    contornoDeRostro(),
    Paint()
      ..color = Paleta.white
      ..style = PaintingStyle.stroke
      ..strokeWidth = 6,
  );
  canvas.restore();
}

const _cabeza = Rect.fromLTWH(100, 62, 100, 130);

Path _busto() => Path()
  ..addRect(const Rect.fromLTWH(132, 170, 36, 40))
  ..moveTo(40, 300)
  ..cubicTo(44, 240, 92, 212, 150, 212)
  ..cubicTo(208, 212, 256, 240, 260, 300)
  ..close();

/// Lentes oscuros y una gorra con visera.
void _pintarAccesorios(Canvas canvas) {
  final oscuro = Paint()..color = Paleta.ink;
  for (final x in [110.0, 153.0]) {
    canvas.drawRRect(
      RRect.fromLTRBR(x, 112, x + 37, 134, const Radius.circular(9)),
      oscuro,
    );
  }
  canvas.drawRect(const Rect.fromLTWH(145, 118, 10, 4), oscuro);
  final gorra = Paint()..color = Paleta.g700;
  canvas.save();
  canvas.clipRect(const Rect.fromLTWH(0, 0, 300, 96));
  canvas.drawOval(_cabeza.inflate(5), gorra);
  canvas.restore();
  canvas.drawRRect(
    RRect.fromLTRBR(92, 88, 222, 100, const Radius.circular(6)),
    gorra,
  );
}
