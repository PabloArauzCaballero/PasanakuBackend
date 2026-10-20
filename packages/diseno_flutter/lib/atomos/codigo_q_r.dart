import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../tokens/tokens.dart';

/// El mismo dibujo para depósito, invitación y vale: cambia el contenido, no la pieza.
class CodigoQR extends StatelessWidget {
  const CodigoQR({
    super.key,
    required this.contenido,
    required this.etiqueta,
    this.tamano = Espacio.s7 * 4,
  });
  final String contenido;
  final String etiqueta;
  final double tamano;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      label: etiqueta,
      image: true,
      excludeSemantics: true,
      child: Container(
        padding: const EdgeInsets.all(Espacio.s3),
        decoration: BoxDecoration(
          color: Paleta.white,
          borderRadius: BorderRadius.circular(Radios.lg),
          border: Border.all(color: t.border, width: Borde.fino),
        ),
        child: QrImageView(
          data: contenido,
          size: tamano,
          eyeStyle: const QrEyeStyle(
            eyeShape: QrEyeShape.square,
            color: Paleta.ink,
          ),
          dataModuleStyle: const QrDataModuleStyle(
            dataModuleShape: QrDataModuleShape.square,
            color: Paleta.ink,
          ),
        ),
      ),
    );
  }
}

/// El mismo QR como imagen PNG, para mandarlo por WhatsApp (2026-10-20). Fondo blanco y
/// margen ancho: WhatsApp recomprime la foto, y un QR con fondo transparente queda negro
/// sobre negro. Corrección de errores M para que se siga leyendo aunque pierda calidad.
/// Con [titulo], el texto va centrado debajo del código.
Future<Uint8List> codigoQrComoPng(String contenido, {String? titulo}) async {
  const lado = 1080.0;
  const margen = 96.0;
  final conTitulo = titulo != null && titulo.trim().isNotEmpty;
  final alto = lado + (conTitulo ? 120.0 : 0.0);

  final grabador = ui.PictureRecorder();
  final lienzo = Canvas(grabador);
  lienzo.drawRect(
    Rect.fromLTWH(0, 0, lado, alto),
    Paint()..color = Paleta.white,
  );
  final qr = QrPainter(
    data: contenido,
    version: QrVersions.auto,
    errorCorrectionLevel: QrErrorCorrectLevel.M,
    gapless: true,
    eyeStyle: const QrEyeStyle(eyeShape: QrEyeShape.square, color: Paleta.ink),
    dataModuleStyle: const QrDataModuleStyle(
      dataModuleShape: QrDataModuleShape.square,
      color: Paleta.ink,
    ),
  );
  lienzo.save();
  lienzo.translate(margen, margen);
  qr.paint(lienzo, const Size.square(lado - 2 * margen));
  lienzo.restore();

  if (conTitulo) {
    final texto = TextPainter(
      text: TextSpan(
        text: titulo.trim(),
        style: Tipo.titulo2.copyWith(color: Paleta.ink),
      ),
      // La imagen mide lo que tres pantallas de 360: el título se ve como en el teléfono.
      textScaler: const TextScaler.linear(lado / 360),
      textAlign: TextAlign.center,
      textDirection: TextDirection.ltr,
      maxLines: 2,
      ellipsis: '…',
    )..layout(maxWidth: lado - 2 * margen);
    texto.paint(lienzo, Offset((lado - texto.width) / 2, lado - margen / 2));
  }

  final imagen = await grabador.endRecording().toImage(
    lado.toInt(),
    alto.toInt(),
  );
  final bytes = await imagen.toByteData(format: ui.ImageByteFormat.png);
  imagen.dispose();
  if (bytes == null) throw StateError('No se pudo dibujar el QR.');
  return bytes.buffer.asUint8List();
}
