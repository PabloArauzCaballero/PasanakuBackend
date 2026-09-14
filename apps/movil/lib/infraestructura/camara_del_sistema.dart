import 'package:image_picker/image_picker.dart';

import '../dominio/puertos/camara.dart';

/// **La cámara de verdad**, con el permiso de verdad.
///
/// Antes de esto, el adaptador hablaba por `MethodChannel` con un plugin nativo que
/// nunca existió: `capturar` devolvía `null` siempre y la pantalla lo trataba como
/// «no se pudo». No había forma de sacar una foto.
///
/// Abre la cámara del sistema, que es la que pide el permiso —el diálogo de iOS con
/// el texto de `NSCameraUsageDescription`— y la que la gente ya sabe usar. La trasera
/// para el documento y la frontal para la prueba de vida: fotografiar un carnet con
/// la cámara frontal no sale bien nunca.
///
/// Devuelve `null` cuando se cancela **y también cuando se deniega el permiso**: el
/// puerto promete eso, y la pantalla ya tiene el camino alternativo listo. Un permiso
/// denegado no es una excepción que mate el alta; es alguien que dijo que no.
class CamaraDelSistema implements Camara {
  CamaraDelSistema([ImagePicker? selector])
    : _selector = selector ?? ImagePicker();

  final ImagePicker _selector;

  /// Suficiente para leer un carnet y liviano para subir por datos móviles. Una foto
  /// de 12 megapíxeles son cuatro megas: se sube lento, se cae más y no se lee mejor.
  static const _ladoMaximo = 1600.0;
  static const _calidad = 88;

  @override
  Future<String?> capturar({required bool esDocumento}) async {
    try {
      final foto = await _selector.pickImage(
        source: ImageSource.camera,
        preferredCameraDevice: esDocumento
            ? CameraDevice.rear
            : CameraDevice.front,
        maxWidth: _ladoMaximo,
        maxHeight: _ladoMaximo,
        imageQuality: _calidad,
      );
      return foto?.path;
    } on Object {
      // Permiso denegado, cámara ocupada o simulador sin cámara. El puerto promete
      // `null`, y arriba eso ya significa «ofrecé reintentar o escribir a mano».
      return null;
    }
  }

  @override
  Future<String?> elegirDeLasFotos() async {
    try {
      final foto = await _selector.pickImage(
        source: ImageSource.gallery,
        maxWidth: _ladoMaximo,
        maxHeight: _ladoMaximo,
        imageQuality: _calidad,
      );
      return foto?.path;
    } on Object {
      return null;
    }
  }
}
