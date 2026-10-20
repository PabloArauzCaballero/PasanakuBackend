import 'package:flutter/services.dart';

import '../../dominio/puertos/compartir.dart';

/// Habla con `MainActivity.kt` por `MethodChannel`, con intents del sistema y sin
/// paquete de pub nuevo. Sin el canal (iOS, web o una prueba) devuelve `false` y la
/// pantalla ofrece copiar el enlace.
class CompartirAndroid implements Compartir {
  CompartirAndroid([MethodChannel? canal])
    : _canal = canal ?? const MethodChannel('bo.aportaya/compartir');

  final MethodChannel _canal;

  Future<bool> _invocar(String metodo, Map<String, String> argumentos) async {
    try {
      return await _canal.invokeMethod<bool>(metodo, argumentos) ?? false;
    } on PlatformException {
      return false;
    } on MissingPluginException {
      return false;
    }
  }

  @override
  Future<bool> porWhatsApp({required String telefono, required String texto}) =>
      _invocar('whatsapp', {'telefono': telefono, 'texto': texto});

  @override
  Future<bool> porSms({required String telefono, required String texto}) =>
      _invocar('sms', {'telefono': telefono, 'texto': texto});

  @override
  Future<bool> conOtraApp({required String texto}) =>
      _invocar('otra', {'texto': texto});

  @override
  Future<bool> imagen({
    required String ruta,
    required String texto,
    bool porWhatsApp = true,
  }) => _invocar('imagen', {
    'ruta': ruta,
    'texto': texto,
    'whatsapp': porWhatsApp ? 'si' : 'no',
  });
}
