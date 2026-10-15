import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../dominio/capturas_del_expediente.dart';
import 'trazos_de_cara.dart';

const proporcionCarnetId1 = 85.6 / 54;

/// Radio de esquina del ID-1 (3,18 mm sobre 85,6 mm de ancho).
const _radioRelativoId1 = 3.18 / 85.6;

/// Rectángulo apaisado del carnet dentro del visor, dejando un margen lateral.
Rect marcoDeCarnetId1(Size size) {
  var ancho = size.width * 0.86;
  if (ancho / proporcionCarnetId1 > size.height * 0.8) {
    ancho = size.height * 0.8 * proporcionCarnetId1;
  }
  return Rect.fromCenter(
    center: size.center(Offset.zero),
    width: ancho,
    height: ancho / proporcionCarnetId1,
  );
}

/// El marco de encuadre: el contorno completo de un carnet ID-1 para el documento y
/// el óvalo de rostro para la cara. Un solo trazo continuo, sin esquinas sueltas.
///
/// Sobre el visor de la cámara se pasa [velo] para oscurecer lo que queda afuera.
/// En la tarjeta vacía del carrusel se pasa [esquemaDe] y dentro del contorno se
/// dibuja, apagado, qué va ahí: la foto y los renglones del anverso, la zona de
/// lectura del reverso, o las flechas de giro de un perfil.
class GuiaDeEncuadre extends StatelessWidget {
  const GuiaDeEncuadre({
    super.key,
    required this.esDocumento,
    this.color = Paleta.white,
    this.grosor = 3,
    this.velo,
    this.esquemaDe,
  });
  final bool esDocumento;

  /// Blanco sobre el visor de la cámara; un texto secundario sobre la tarjeta vacía.
  final Color color;
  final double grosor;
  final Color? velo;
  final CaraDelCarril? esquemaDe;

  @override
  Widget build(BuildContext context) {
    return IgnorePointer(
      child: CustomPaint(
        size: Size.infinite,
        painter: _PintorDeGuia(
          esDocumento: esDocumento,
          color: color,
          grosor: grosor,
          velo: velo,
          esquemaDe: esquemaDe,
        ),
      ),
    );
  }
}

class _PintorDeGuia extends CustomPainter {
  _PintorDeGuia({
    required this.esDocumento,
    required this.color,
    required this.grosor,
    required this.velo,
    required this.esquemaDe,
  });
  final bool esDocumento;
  final Color color;
  final double grosor;
  final Color? velo;
  final CaraDelCarril? esquemaDe;

  Path _contorno(Size size) {
    if (esDocumento) {
      final marco = marcoDeCarnetId1(size);
      final radio = Radius.circular(marco.width * _radioRelativoId1);
      return Path()..addRRect(RRect.fromRectAndRadius(marco, radio));
    }
    final alto = size.height * 0.78;
    final ancho = alto * ovaloDeRostro.width / ovaloDeRostro.height;
    return Path()..addOval(
      Rect.fromCenter(
        center: size.center(Offset.zero),
        width: ancho.clamp(0, size.width * 0.8).toDouble(),
        height: alto,
      ),
    );
  }

  @override
  void paint(Canvas canvas, Size size) {
    if (size.isEmpty) return;
    final contorno = _contorno(size);
    final velo = this.velo;
    if (velo != null) {
      canvas.drawPath(
        Path.combine(
          PathOperation.difference,
          Path()..addRect(Offset.zero & size),
          contorno,
        ),
        Paint()..color = velo,
      );
    }
    canvas.drawPath(
      contorno,
      Paint()
        ..color = color
        ..style = PaintingStyle.stroke
        ..strokeWidth = grosor,
    );
    final cara = esquemaDe;
    if (cara != null) _esquema(canvas, size, contorno.getBounds(), cara);
  }

  void _esquema(Canvas canvas, Size size, Rect marco, CaraDelCarril cara) {
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
        canvas.drawRRect(
          RRect.fromRectAndRadius(foto, Radius.circular(2 * u)),
          tenue,
        );
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
      case CaraDelCarril.perfilIzquierdo || CaraDelCarril.perfilDerecho:
        final escala = marco.height / ovaloDeRostro.height;
        final derecha = cara == CaraDelCarril.perfilDerecho;
        final matriz = Matrix4.identity()
          ..translateByDouble(marco.center.dx, marco.center.dy, 0, 1)
          ..scaleByDouble(derecha ? -escala : escala, escala, 1, 1)
          ..translateByDouble(
            -ovaloDeRostro.center.dx,
            -ovaloDeRostro.center.dy,
            0,
            1,
          );
        canvas.drawPath(
          flechasDeGiro().transform(matriz.storage),
          Paint()
            ..color = color
            ..style = PaintingStyle.stroke
            ..strokeCap = StrokeCap.round
            ..strokeJoin = StrokeJoin.round
            ..strokeWidth = grosor,
        );
      case CaraDelCarril.selfie:
        break;
    }
  }

  @override
  bool shouldRepaint(_PintorDeGuia oldDelegate) =>
      oldDelegate.esDocumento != esDocumento ||
      oldDelegate.color != color ||
      oldDelegate.grosor != grosor ||
      oldDelegate.velo != velo ||
      oldDelegate.esquemaDe != esquemaDe;
}
