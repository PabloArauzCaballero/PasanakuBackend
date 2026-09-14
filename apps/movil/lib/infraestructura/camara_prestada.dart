import 'dart:io';

import 'package:path_provider/path_provider.dart';

import '../dominio/foto_del_puente.dart';
import '../dominio/puertos/camara.dart';

/// **La cámara del Mac, prestada al simulador.**
///
/// El simulador de iOS no tiene cámara y no puede usar la del Mac: no expone ningún
/// `AVCaptureDevice`, así que `image_picker` con `ImageSource.camera` no devuelve
/// nada. Es de Apple, no del código. Sin cámara no hay forma de recorrer el alta
/// —documento y prueba de vida— sin un teléfono físico.
///
/// Este adaptador le pide la foto a `scripts/camara_del_mac.py`, que corre en la
/// máquina y saca un cuadro con la cámara de verdad. **La foto es real**: lo único
/// distinto es de dónde sale. Todo lo que viene después —subida, MinIO, expediente,
/// decisión en el backoffice— es exactamente el mismo camino.
///
/// Solo existe cuando se compila con `--dart-define=CAMARA_DEV=<url>`. Sin esa
/// bandera, `camaraDeLaPlataforma()` devuelve la cámara del sistema y esta clase no
/// se instancia nunca.
class CamaraPrestada implements Camara {
  const CamaraPrestada(this.url, this._respaldo);

  /// De dónde se pide la foto. La pone `--dart-define=CAMARA_DEV`.
  final String url;

  /// Si el puente no está levantado, se cae a la cámara del sistema: en un teléfono
  /// de verdad esta clase no tiene por qué estorbar.
  final Camara _respaldo;

  @override
  Future<String?> capturar({required bool esDocumento}) async {
    final bytes = await pedirFotoAlPuente(url);
    if (bytes == null) {
      // El puente no está corriendo, o macOS no dio el permiso de cámara. No se
      // rompe el alta por eso: se intenta la cámara del sistema y, si tampoco, la
      // pantalla ya ofrece elegir de las fotos o escribir a mano.
      return _respaldo.capturar(esDocumento: esDocumento);
    }
    final carpeta = await getTemporaryDirectory();
    final destino = File(
      '${carpeta.path}/foto-${DateTime.now().millisecondsSinceEpoch}.jpg',
    );
    await destino.writeAsBytes(bytes);
    return destino.path;
  }

  /// La fototeca es la del simulador: eso sí funciona sin puente.
  @override
  Future<String?> elegirDeLasFotos() => _respaldo.elegirDeLasFotos();
}
