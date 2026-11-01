import 'dart:math' as math;
import 'dart:ui' as ui;

import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../dominio/pautas_de_fotos.dart';
import 'escena_de_rostro.dart';
import 'persona_de_ejemplo.dart';

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

  RRect _contorno(Rect carnet) => RRect.fromRectAndRadius(
    carnet,
    Radius.circular(carnet.width * 3.18 / 85.6),
  );

  /// Una franja de luz quemada y el punto de la lámpara que la provoca.
  void _pintarReflejo(Canvas canvas, Rect carnet) {
    canvas.save();
    canvas.clipRRect(_contorno(carnet));
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
  void _pintarCarnet(Canvas canvas, Rect carnet) {
    final u = carnet.width / 100;
    final contorno = _contorno(carnet);
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
    _pintarZonaDeLectura(canvas, carnet, u);
    canvas.restore();
    _pintarFoto(canvas, carnet, u);
    _pintarDatos(canvas, carnet, u);
    _pintarChip(canvas, carnet, u);
    _pintarHolograma(canvas, carnet, u);
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
  void _pintarDatos(Canvas canvas, Rect carnet, double u) {
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

  void _pintarChip(Canvas canvas, Rect carnet, double u) {
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
  void _pintarHolograma(Canvas canvas, Rect carnet, double u) {
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
  void _pintarZonaDeLectura(Canvas canvas, Rect carnet, double u) {
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

  @override
  bool shouldRepaint(_PintorDePauta oldDelegate) =>
      oldDelegate.escena != escena;
}
