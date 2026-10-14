import 'dart:io';

import 'package:flutter/material.dart';

import '../dominio/capturas_del_expediente.dart';

String etiquetaHeroDeCaptura(Captura captura) => 'captura:${captura.ruta}';

int anchoDeCacheDeCaptura(BuildContext context) =>
    (MediaQuery.sizeOf(context).width * MediaQuery.devicePixelRatioOf(context))
        .round()
        .clamp(1, 4096)
        .toInt();

Widget imagenDeCaptura(
  BuildContext context,
  Captura captura, {
  required BoxFit fit,
  double? width,
  double? height,
}) {
  final t = Theme.of(context);
  return Image.file(
    File(captura.ruta),
    fit: fit,
    width: width,
    height: height,
    cacheWidth: anchoDeCacheDeCaptura(context),
    gaplessPlayback: true,
    errorBuilder: (context, error, stack) => ColoredBox(
      key: const ValueKey('hueco-de-captura'),
      color: t.colorScheme.surfaceContainerHighest,
      child: const SizedBox.expand(),
    ),
  );
}

Future<void> mostrarCapturaAmpliada(BuildContext context, Captura captura) =>
    Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (context) => _VisorAmpliado(captura: captura),
      ),
    );

class _VisorAmpliado extends StatelessWidget {
  const _VisorAmpliado({required this.captura});
  final Captura captura;

  @override
  Widget build(BuildContext context) {
    final ancho = MediaQuery.sizeOf(context).width;
    final alto = MediaQuery.sizeOf(context).height;
    return Scaffold(
      backgroundColor: Theme.of(context).colorScheme.scrim,
      body: Stack(
        children: [
          Center(
            child: Hero(
              tag: etiquetaHeroDeCaptura(captura),
              child: InteractiveViewer(
                minScale: 1,
                maxScale: 5,
                child: imagenDeCaptura(
                  context,
                  captura,
                  fit: BoxFit.contain,
                  width: ancho,
                  height: alto,
                ),
              ),
            ),
          ),
          Positioned(
            top: MediaQuery.paddingOf(context).top + 8,
            right: 8,
            child: IconButton.filledTonal(
              tooltip: 'Cerrar imagen ampliada',
              onPressed: () => Navigator.of(context).pop(),
              icon: const Icon(Icons.close_rounded),
            ),
          ),
        ],
      ),
    );
  }
}
