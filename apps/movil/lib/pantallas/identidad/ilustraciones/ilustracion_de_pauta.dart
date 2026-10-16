import 'dart:ui' as ui;

import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../dominio/pautas_de_fotos.dart';
import 'escena_de_rostro.dart';

/// Ilustración geométrica original: no contiene ni depende de fotos personales.
///
/// Es una «foto de ejemplo», así que usa colores fijos de la paleta, iguales en tema
/// claro y oscuro, como el visor de la cámara. El carnet tiene la proporción ID-1
/// real (85,6 × 54 mm) y cada defecto se dibuja como se ve en una foto: oscuridad,
/// una franja de reflejo, desenfoque de verdad y perspectiva de verdad.
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
    // La mesa oscura y lisa que pide «Antes de escanear».
    canvas.drawRect(Offset.zero & size, Paint()..color = Paleta.slate);
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
        _pintarCarnet(canvas, carnet);
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
        _pintarCarnet(canvas, carnet);
      default:
        _pintarCarnet(canvas, carnet);
    }
    if (escena == EscenaDePauta.carnetReflejo) {
      canvas.save();
      canvas.clipRRect(_contorno(carnet));
      canvas.drawRect(
        carnet,
        Paint()
          ..shader = ui.Gradient.linear(
            Offset(carnet.left + ancho * 0.25, carnet.top),
            Offset(carnet.left + ancho * 0.65, carnet.bottom),
            [
              Paleta.white.withValues(alpha: 0),
              Paleta.white.withValues(alpha: 0.95),
              Paleta.white.withValues(alpha: 0.95),
              Paleta.white.withValues(alpha: 0),
            ],
            const [0.3, 0.45, 0.55, 0.7],
          ),
      );
      canvas.restore();
    }
    canvas.restore();
    if (escena == EscenaDePauta.carnetOscuro) {
      canvas.drawRect(
        Offset.zero & size,
        Paint()..color = Paleta.ink.withValues(alpha: 0.78),
      );
    }
  }

  static const _proporcionId1 = 85.6 / 54;

  RRect _contorno(Rect carnet) => RRect.fromRectAndRadius(
    carnet,
    Radius.circular(carnet.width * 3.18 / 85.6),
  );

  /// Anverso esquemático: franja de encabezado, foto con busto y renglones de datos.
  void _pintarCarnet(Canvas canvas, Rect carnet) {
    final u = carnet.width / 100;
    final contorno = _contorno(carnet);
    canvas.drawRRect(contorno, Paint()..color = Paleta.cloud);
    canvas.save();
    canvas.clipRRect(contorno);
    canvas.drawRect(
      Rect.fromLTWH(carnet.left, carnet.top, carnet.width, 11 * u),
      Paint()..color = Paleta.g200,
    );
    canvas.restore();
    final foto = Rect.fromLTWH(
      carnet.left + 6 * u,
      carnet.top + 17 * u,
      25 * u,
      32 * u,
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(foto, Radius.circular(1.5 * u)),
      Paint()..color = Paleta.line,
    );
    final busto = Paint()..color = Paleta.muted;
    canvas.drawOval(
      Rect.fromCenter(
        center: Offset(foto.center.dx, foto.top + 12 * u),
        width: 10 * u,
        height: 12.5 * u,
      ),
      busto,
    );
    canvas.save();
    canvas.clipRect(foto);
    canvas.drawOval(
      Rect.fromCenter(
        center: Offset(foto.center.dx, foto.bottom + 2 * u),
        width: 21 * u,
        height: 17 * u,
      ),
      busto,
    );
    canvas.restore();
    final x = foto.right + 6 * u;
    final renglones = [(18.0, 34.0), (25.0, 52.0), (35.0, 26.0), (42.0, 44.0)];
    for (final (i, (y, largo)) in renglones.indexed) {
      canvas.drawRRect(
        RRect.fromLTRBR(
          x,
          carnet.top + y * u,
          x + largo * u,
          carnet.top + (y + 3) * u,
          Radius.circular(1.5 * u),
        ),
        Paint()..color = i.isEven ? Paleta.muted : Paleta.slate,
      );
    }
    canvas.drawRRect(
      RRect.fromLTRBR(
        carnet.left + 6 * u,
        carnet.bottom - 9 * u,
        carnet.right - 6 * u,
        carnet.bottom - 6 * u,
        Radius.circular(1.5 * u),
      ),
      Paint()..color = Paleta.line,
    );
  }

  @override
  bool shouldRepaint(_PintorDePauta oldDelegate) =>
      oldDelegate.escena != escena;
}
