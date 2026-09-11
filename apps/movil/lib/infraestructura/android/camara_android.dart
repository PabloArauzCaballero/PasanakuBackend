import 'package:flutter/services.dart';

import '../../dominio/puertos/camara.dart';

/// `MediaStore.ACTION_IMAGE_CAPTURE` por `MethodChannel`, sin paquete de pub nuevo.
/// **Sin usuario todavía** (llega en F3, cotejo documental de CU-01): se declara y
/// se cablea ahora para que el shell no cambie cuando F3 lo necesite.
class CamaraAndroid implements Camara {
  CamaraAndroid([MethodChannel? canal])
    : _canal = canal ?? const MethodChannel('bo.aportaya/camara');

  final MethodChannel _canal;

  @override
  Future<String?> capturar({required bool esDocumento}) async {
    try {
      return await _canal.invokeMethod<String>('capturar', {
        'esDocumento': esDocumento,
      });
    } on PlatformException {
      return null;
    } on MissingPluginException {
      return null;
    }
  }
}
