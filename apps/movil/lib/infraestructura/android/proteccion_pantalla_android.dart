import 'package:flutter/services.dart';

import '../../dominio/puertos/proteccion_pantalla.dart';

/// `FLAG_SECURE` es de la API de Android y no pide ningún paquete de pub: el canal
/// lo atiende `MainActivity.kt` llamando a `window.addFlags` / `clearFlags`.
class ProteccionPantallaAndroid implements ProteccionPantalla {
  ProteccionPantallaAndroid([MethodChannel? canal])
    : _canal = canal ?? const MethodChannel('bo.aportaya/proteccion_pantalla');

  final MethodChannel _canal;

  @override
  Future<void> activar() async {
    try {
      await _canal.invokeMethod<void>('activar');
    } on MissingPluginException {
      // No hay plataforma detrás (prueba de widget): sin efecto, no falla.
    }
  }

  @override
  Future<void> desactivar() async {
    try {
      await _canal.invokeMethod<void>('desactivar');
    } on MissingPluginException {
      // idem
    }
  }
}
