import 'dart:ui' as ui;

import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/widgets.dart';

import '../dominio/pautas_de_fotos.dart';
import '../pasos_alta/trazos_de_cara.dart';
import 'persona_ilustrada.dart';

/// Ejemplo de foto de rostro: una persona ilustrada dentro del mismo óvalo y velo
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
  pintarPersonaIlustrada(
    canvas,
    conAccesorios: escena == EscenaDePauta.rostroConAccesorios,
    silueta: contraluz ? Paleta.slate : null,
  );
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
