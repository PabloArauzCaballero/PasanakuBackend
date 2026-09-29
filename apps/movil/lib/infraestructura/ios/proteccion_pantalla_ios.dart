import 'package:flutter/services.dart';

import '../../dominio/puertos/proteccion_pantalla.dart';

/// El mismo canal que Android (`bo.aportaya/proteccion_pantalla`), atendido en
/// iOS por `AppDelegate.swift`: la capa de la ventana cuelga del lienzo seguro de
/// un `UITextField`, y `isSecureTextEntry` tapa el contenido en capturas y
/// grabaciones mientras la ruta lo pide.
///
/// Grado `degradado`, no `soportado` (`capacidades.dart`): iOS no tiene un
/// `FLAG_SECURE`; lo que se usa es un comportamiento de UIKit que Apple no
/// documenta como API de privacidad y podría cambiar en una versión futura.
///
/// Antes de esto era un no-op y `app.dart` bloqueaba el arranque en todo iPhone
/// release (H2.S2.M3): la pantalla «AportaYa no puede abrir en este dispositivo».
class ProteccionPantallaIos implements ProteccionPantalla {
  ProteccionPantallaIos([MethodChannel? canal])
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
