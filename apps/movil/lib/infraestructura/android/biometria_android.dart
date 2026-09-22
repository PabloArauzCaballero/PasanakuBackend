import 'package:flutter/services.dart';

import '../../dominio/puertos/biometria.dart';

/// Habla con `BiometricPrompt` nativo por `MethodChannel` — sin agregar un paquete
/// de pub nuevo (`pubspec.yaml` está congelado para este carril). El canal lo
/// atiende `MainActivity.kt` con `androidx.biometric`, agregado en
/// `android/app/build.gradle.kts` (ese archivo no está en la lista de「no tocás」).
class BiometriaAndroid implements Biometria {
  BiometriaAndroid([MethodChannel? canal])
    : _canal = canal ?? const MethodChannel('bo.aportaya/biometria');

  final MethodChannel _canal;

  @override
  Future<bool> disponible() async {
    try {
      return await _canal.invokeMethod<bool>('disponible') ?? false;
    } on PlatformException {
      return false;
    } on MissingPluginException {
      // Sin el `MethodChannel` nativo registrado (por ejemplo, en una prueba de
      // widget sin plataforma): se declara no disponible, nunca se rompe la app.
      return false;
    }
  }

  @override
  Future<bool> confirmar(String motivo) async {
    try {
      return await _canal.invokeMethod<bool>('confirmar', {'motivo': motivo}) ??
          false;
    } on PlatformException {
      return false;
    } on MissingPluginException {
      return false;
    }
  }
}
