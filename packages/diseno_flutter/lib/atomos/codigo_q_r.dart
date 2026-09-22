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
