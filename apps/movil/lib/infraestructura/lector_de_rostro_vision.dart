import 'package:camera/camera.dart';
import 'package:flutter/services.dart';

import '../dominio/puertos/camara_en_vivo.dart';

/// Lee el rostro de un cuadro con Vision de Apple (`AppDelegate.swift`,
/// `LectorDeRostroVision`), en el teléfono: la imagen no sale del dispositivo. Es el
/// lector de iOS: ML Kit quedó solo en Android porque sus pods no tienen slice arm64
/// de simulador (`terceros/README.md`).
class LectorDeRostroVision {
  LectorDeRostroVision(this._camara);

  static const canal = MethodChannel('bo.aportaya/lector_de_rostro');

  final CameraDescription _camara;

  /// `null` si el cuadro no es BGRA de un plano, o no hay cara.
  Future<LecturaDeRostro?> leer(CameraImage cuadro) async {
    if (cuadro.planes.length != 1) return null;
    final plano = cuadro.planes.first;
    final caras = await canal.invokeListMethod<Object?>('leer', {
      'bytes': plano.bytes,
      'ancho': cuadro.width,
      'alto': cuadro.height,
      'bytesPorFila': plano.bytesPerRow,
      'rotacion': _camara.sensorOrientation,
    });
    return lecturaDesdeVision(caras);
  }

  Future<void> cerrar() async {}
}

/// Traduce lo que devuelve Vision a [LecturaDeRostro]. La cara que manda es la más
/// ancha, como con ML Kit. El cuadro de la cámara delantera de iOS llega espejado
/// (`camera_avfoundation`: `isVideoMirrored = true`), así que el `yaw` se invierte
/// para que el giro positivo sea hacia la izquierda de la persona, igual que en
/// Android. Supuesto a confirmar en un iPhone real.
LecturaDeRostro? lecturaDesdeVision(List<Object?>? caras) {
  final validas = [
    for (final c in caras ?? const <Object?>[])
      if (c is Map && c['ancho'] is num) c,
  ];
  if (validas.isEmpty) return null;
  final cara = validas.reduce(
    (a, b) => (a['ancho'] as num) >= (b['ancho'] as num) ? a : b,
  );
  double valor(String clave) => (cara[clave] as num?)?.toDouble() ?? 0;
  return LecturaDeRostro(
    rostros: validas.length,
    centroX: valor('x'),
    centroY: valor('y'),
    ancho: valor('ancho'),
    giro: -valor('yaw'),
    inclinacion: valor('pitch'),
  );
}
