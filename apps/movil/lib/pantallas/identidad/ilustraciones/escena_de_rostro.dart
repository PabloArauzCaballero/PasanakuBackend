import 'dart:ui' as ui;

import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/widgets.dart';

import '../dominio/pautas_de_fotos.dart';
import '../pasos_alta/trazos_de_cara.dart';
import 'persona_de_ejemplo.dart';

/// Ejemplo de foto de rostro: la persona de ejemplo dentro del mismo óvalo que
/// muestra la cámara. Se dibuja en el lienzo normalizado de 300 × 300 de
/// `trazos_de_cara.dart` y se escala al alto de la escena.
void pintarEscenaDeRostro(Canvas canvas, Size size, EscenaDePauta escena) {
  final contraluz = escena == EscenaDePauta.rostroContraluz;
  final fondo = Offset.zero & size;
  // Una pared lisa, más clara arriba, como la luz de una habitación.
  canvas.drawRect(
    fondo,
    Paint()
      ..shader = ui.Gradient.linear(fondo.topCenter, fondo.bottomCenter, [
        Paleta.cloud,
        Paleta.line,
      ]),
  );
  canvas.save();
  final escala = size.height / 300;
  canvas.translate(size.width / 2 - 150 * escala, 0);
  canvas.scale(escala);
  if (contraluz) {
    // Una ventana encendida detrás: el fondo quema y la persona queda oscura.
    canvas.drawCircle(
      const Offset(150, 120),
      200,
      Paint()
        ..shader = ui.Gradient.radial(
          const Offset(150, 120),
          200,
          [Paleta.white, Paleta.white, Paleta.cloud, Paleta.line],
          const [0, 0.4, 0.7, 1],
        ),
    );
  }
  // Sombra suave de la persona sobre la pared.
  canvas.drawOval(
    const Rect.fromLTWH(70, 190, 160, 120),
    Paint()
      ..color = Paleta.ink.withValues(alpha: contraluz ? 0 : 0.12)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 18),
  );
  canvas.save();
  if (escena == EscenaDePauta.rostroLadeado) {
    // Cabeza inclinada y corrida hacia afuera del óvalo.
    canvas.translate(70, 10);
    canvas.rotate(0.3);
  }
  if (contraluz) {
    // La persona entera queda en sombra, con un borde de luz alrededor.
    canvas.saveLayer(const Rect.fromLTWH(0, 0, 300, 300), Paint());
    pintarPersona(canvas);
    canvas.drawRect(
      const Rect.fromLTWH(0, 0, 300, 300),
      Paint()
        ..color = Paleta.ink.withValues(alpha: 0.78)
        ..blendMode = BlendMode.srcATop,
    );
    canvas.restore();
    // La luz viene de atrás: el borde solo brilla arriba, no en el mentón.
    canvas.clipRect(const Rect.fromLTWH(0, 0, 300, 140));
    canvas.drawOval(
      cabezaDePersona.inflate(2),
      Paint()
        ..color = Paleta.white.withValues(alpha: 0.55)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 3
        ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 2),
    );
  } else {
    pintarPersona(
      canvas,
      conAccesorios: escena == EscenaDePauta.rostroConAccesorios,
    );
  }
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
