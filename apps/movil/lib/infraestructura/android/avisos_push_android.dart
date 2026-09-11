import 'dart:async';

import 'package:flutter/services.dart';

import '../../dominio/puertos/avisos_push.dart';

/// **Hueco declarado** (ver informe del carril): el registro FCM de verdad pide el
/// SDK de Firebase (plugin de Gradle + `google-services.json` del proyecto), que no
/// existe en este andamiaje y que `pubspec.yaml`/`android/build.gradle.kts` a nivel
/// de proyecto no están habilitados a cambiar libremente desde un carril de frontend
/// sin antes decidir el proveedor (`notificaciones-consentimiento`, ADR-035). El
/// canal ya está declarado y listo para conectarse cuando eso pase: hoy responde
/// `null` — la app funciona igual, porque `/notificaciones/bandeja` es la fuente de
/// verdad y el push es solo un aviso de que hay algo nuevo.
class AvisosPushAndroid implements AvisosPush {
  AvisosPushAndroid([MethodChannel? canal])
    : _canal = canal ?? const MethodChannel('bo.aportaya/avisos_push');

  final MethodChannel _canal;
  final _toques = StreamController<String>.broadcast();

  @override
  Future<String?> token() async {
    try {
      return await _canal.invokeMethod<String>('token');
    } on PlatformException {
      return null;
    } on MissingPluginException {
      return null;
    }
  }

  @override
  Stream<String> get toques => _toques.stream;
}
