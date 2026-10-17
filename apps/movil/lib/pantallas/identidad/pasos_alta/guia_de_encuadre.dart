import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../dominio/capturas_del_expediente.dart';
import 'esquema_de_captura.dart';
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
/// dibuja qué va ahí (ver `esquema_de_captura.dart`).
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
    final cara = esquemaDe;
    if (cara != null) {
      pintarEsquemaDeCaptura(
        canvas,
        contorno.getBounds(),
        cara,
        color: color,
        grosor: grosor,
      );
    }
    canvas.drawPath(
      contorno,
      Paint()
        ..color = color
        ..style = PaintingStyle.stroke
        ..strokeWidth = grosor,
    );
  }

  @override
  bool shouldRepaint(_PintorDeGuia oldDelegate) =>
      oldDelegate.esDocumento != esDocumento ||
      oldDelegate.color != color ||
      oldDelegate.grosor != grosor ||
      oldDelegate.velo != velo ||
      oldDelegate.esquemaDe != esquemaDe;
}
