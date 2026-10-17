import 'package:flutter/material.dart';

import '../dominio/capturas_del_expediente.dart';
import '../ilustraciones/persona_ilustrada.dart';
import 'trazos_de_cara.dart';

/// Lo que va dentro del contorno de la tarjeta vacía del carrusel: el anverso con
/// la foto de la persona y sus renglones, el reverso con las líneas de lectura, y
/// para el rostro la persona ilustrada dentro del óvalo (con flechas de giro en los
/// perfiles). [marco] es el contorno ya calculado: el carnet o el óvalo.
void pintarEsquemaDeCaptura(
  Canvas canvas,
  Rect marco,
  CaraDelCarril cara, {
  required Color color,
  required double grosor,
}) {
  final tenue = Paint()..color = color.withValues(alpha: 0.22);
  RRect barra(double x, double y, double ancho, double alto) =>
      RRect.fromLTRBR(x, y, x + ancho, y + alto, Radius.circular(alto / 2));
  final u = marco.width / 100;
  switch (cara) {
    case CaraDelCarril.anverso:
      // Foto a la izquierda, renglones de datos a la derecha.
      final foto = Rect.fromLTWH(
        marco.left + 7 * u,
        marco.top + marco.height * 0.24,
        24 * u,
        marco.height * 0.56,
      );
      final recorte = RRect.fromRectAndRadius(foto, Radius.circular(2 * u));
      canvas.drawRRect(recorte, tenue);
      canvas.save();
      canvas.clipRRect(recorte);
      final escala = foto.height / 250;
      canvas.translate(foto.center.dx - 150 * escala, foto.top - 30 * escala);
      canvas.scale(escala);
      pintarPersonaIlustrada(canvas);
      canvas.restore();
      for (final (i, largo) in [(0, 46.0), (1, 38.0), (2, 52.0), (3, 30.0)]) {
        canvas.drawRRect(
          barra(foto.right + 7 * u, foto.top + i * 9 * u, largo * u, 3 * u),
          tenue,
        );
      }
    case CaraDelCarril.reverso:
      // Tres líneas de lectura mecánica abajo, de borde a borde.
      for (var i = 0; i < 3; i++) {
        canvas.drawRRect(
          barra(
            marco.left + 7 * u,
            marco.bottom - (10 + i * 7) * u,
            86 * u,
            3 * u,
          ),
          tenue,
        );
      }
    case CaraDelCarril.selfie ||
        CaraDelCarril.perfilIzquierdo ||
        CaraDelCarril.perfilDerecho:
      // El lienzo normalizado de 300 × 300 llevado al óvalo de la tarjeta.
      final escala = marco.height / ovaloDeRostro.height;
      final matriz = Matrix4.identity()
        ..translateByDouble(marco.center.dx, marco.center.dy, 0, 1)
        ..scaleByDouble(escala, escala, 1, 1)
        ..translateByDouble(
          -ovaloDeRostro.center.dx,
          -ovaloDeRostro.center.dy,
          0,
          1,
        );
      canvas.save();
      canvas.clipPath(Path()..addOval(marco));
      canvas.transform(matriz.storage);
      pintarPersonaIlustrada(
        canvas,
        giro: switch (cara) {
          CaraDelCarril.perfilIzquierdo => -1,
          CaraDelCarril.perfilDerecho => 1,
          _ => 0,
        },
      );
      canvas.restore();
      if (cara == CaraDelCarril.selfie) return;
      final derecha = cara == CaraDelCarril.perfilDerecho;
      final espejo = Matrix4.identity()
        ..translateByDouble(marco.center.dx, marco.center.dy, 0, 1)
        ..scaleByDouble(derecha ? -escala : escala, escala, 1, 1)
        ..translateByDouble(
          -ovaloDeRostro.center.dx,
          -ovaloDeRostro.center.dy,
          0,
          1,
        );
      canvas.drawPath(
        flechasDeGiro().transform(espejo.storage),
        Paint()
          ..color = color
          ..style = PaintingStyle.stroke
          ..strokeCap = StrokeCap.round
          ..strokeJoin = StrokeJoin.round
          ..strokeWidth = grosor,
      );
  }
}
