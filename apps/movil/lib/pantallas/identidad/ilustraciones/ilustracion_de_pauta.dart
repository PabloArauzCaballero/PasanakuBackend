import 'dart:ui' as ui;

import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../dominio/pautas_de_fotos.dart';
import 'carnet_de_ejemplo.dart';
import 'escena_de_rostro.dart';

/// Ilustración vectorial original: no contiene ni depende de fotos personales, y se
/// ve nítida a cualquier densidad de pantalla.
///
/// Es una «foto de ejemplo», así que usa colores fijos de la paleta, iguales en tema
/// claro y oscuro, como el visor de la cámara. El carnet tiene la proporción ID-1
/// real (85,6 × 54 mm) y cada defecto se dibuja como se ve en una foto: oscuridad,
/// una franja de reflejo, desenfoque de verdad y perspectiva de verdad. El carnet es
/// genérico: no copia el diseño de ningún documento real.
class IlustracionDePauta extends StatelessWidget {
  const IlustracionDePauta({super.key, required this.escena});

  final EscenaDePauta escena;

  @override
  Widget build(BuildContext context) => AspectRatio(
    aspectRatio: 2,
    child: ExcludeSemantics(
      child: CustomPaint(painter: _PintorDePauta(escena)),
    ),
  );
}

class _PintorDePauta extends CustomPainter {
  const _PintorDePauta(this.escena);

  final EscenaDePauta escena;

  @override
  void paint(Canvas canvas, Size size) {
    canvas.save();
    canvas.clipRRect(
      RRect.fromRectAndRadius(
        Offset.zero & size,
        const Radius.circular(Radios.md),
      ),
    );
    if (escena.name.startsWith('carnet')) {
      _pintarEscenaDeCarnet(canvas, size);
    } else {
      pintarEscenaDeRostro(canvas, size, escena);
    }
    canvas.restore();
  }

  void _pintarEscenaDeCarnet(Canvas canvas, Size size) {
    // La mesa oscura y lisa que pide «Antes de escanear», con la luz al centro.
    final mesa = Offset.zero & size;
    canvas.drawRect(
      mesa,
      Paint()
        ..shader = ui.Gradient.radial(mesa.center, size.longestSide * 0.6, [
          Paleta.slate,
          Paleta.g900,
        ]),
    );
    var ancho = size.width * 0.62;
    if (ancho / _proporcionId1 > size.height * 0.78) {
      ancho = size.height * 0.78 * _proporcionId1;
    }
    final carnet = Rect.fromCenter(
      center: Offset.zero,
      width: ancho,
      height: ancho / _proporcionId1,
    );
    canvas.save();
    canvas.translate(size.width / 2, size.height / 2);
    switch (escena) {
      case EscenaDePauta.carnetBorroso:
        canvas.saveLayer(
          carnet.inflate(ancho * 0.1),
          Paint()
            ..imageFilter = ui.ImageFilter.blur(
              sigmaX: ancho * 0.022,
              sigmaY: ancho * 0.022,
            ),
        );
        pintarCarnet(canvas, carnet);
        canvas.restore();
      case EscenaDePauta.carnetInclinado:
        // Fotografiado de costado: el borde lejano se achica.
        canvas.transform(
          (Matrix4.identity()
                ..setEntry(3, 2, 0.004)
                ..rotateX(0.7)
                ..rotateZ(-0.1))
              .storage,
        );
        pintarCarnet(canvas, carnet);
      default:
        pintarCarnet(canvas, carnet);
    }
    if (escena == EscenaDePauta.carnetReflejo) _pintarReflejo(canvas, carnet);
    canvas.restore();
    if (escena == EscenaDePauta.carnetOscuro) {
      canvas.drawRect(
        mesa,
        Paint()..color = Paleta.ink.withValues(alpha: 0.78),
      );
    }
  }

  static const _proporcionId1 = 85.6 / 54;

  /// Una franja de luz quemada y el punto de la lámpara que la provoca.
  void _pintarReflejo(Canvas canvas, Rect carnet) {
    canvas.save();
    canvas.clipRRect(contornoDeCarnet(carnet));
    canvas.drawRect(
      carnet,
      Paint()
        ..shader = ui.Gradient.linear(
          Offset(carnet.left + carnet.width * 0.25, carnet.top),
          Offset(carnet.left + carnet.width * 0.65, carnet.bottom),
          [
            Paleta.white.withValues(alpha: 0),
            Paleta.white.withValues(alpha: 0.95),
            Paleta.white.withValues(alpha: 0.95),
            Paleta.white.withValues(alpha: 0),
          ],
          const [0.3, 0.45, 0.55, 0.7],
        ),
    );
    final foco = Offset(
      carnet.left + carnet.width * 0.5,
      carnet.top + carnet.height * 0.42,
    );
    canvas.drawCircle(
      foco,
      carnet.width * 0.16,
      Paint()
        ..shader = ui.Gradient.radial(foco, carnet.width * 0.16, [
          Paleta.white,
          Paleta.white.withValues(alpha: 0),
        ]),
    );
    canvas.restore();
  }

  /// Anverso genérico: encabezado, foto de la persona, chip, datos con su rótulo,
  /// firma, holograma y la zona de lectura mecánica al pie.

  @override
  bool shouldRepaint(_PintorDePauta oldDelegate) =>
      oldDelegate.escena != escena;
}
