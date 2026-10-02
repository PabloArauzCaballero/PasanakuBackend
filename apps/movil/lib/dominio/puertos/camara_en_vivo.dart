import 'package:flutter/widgets.dart';

/// El resultado de pedir permiso de cámara. Separado de `denegado` porque Atlas
/// distingue los dos: «denegado» ofrece volver a pedir, «para siempre» manda a
/// Ajustes — el sistema no vuelve a preguntar una vez que la persona tocó «no
/// preguntar de nuevo».
enum EstadoDePermiso { concedido, denegado, denegadoParaSiempre }

/// La cámara en vivo del escáner de identidad: visor con guía de encuadre,
/// cámara trasera para el carnet y delantera para la prueba de vida.
///
/// Es la única excepción de un puerto que devuelve un `Widget`: el visor en vivo no
/// se puede desacoplar del controlador nativo sin una capa que acá no se justifica,
/// y esta app entera es Flutter — no hay un lado "puro" al que protejer de
/// `CameraPreview`. El resto de la interfaz (permiso, captura, liberar) sigue siendo
/// Dart puro, igual que los demás puertos de F2.
abstract interface class CamaraEnVivo {
  Future<EstadoDePermiso> pedirPermiso();

  /// Prepara el sensor pedido. Se llama después de tener el permiso.
  Future<void> iniciar({required bool frontal});

  /// El visor en vivo, ya inicializado. Solo se pide después de [iniciar].
  Widget vista();

  /// La ruta del archivo capturado.
  Future<String> tomarFoto();

  /// Libera el sensor. Se llama siempre al salir de la pantalla de cámara, haya
  /// foto o no — una cámara que queda abierta es batería y es el candado de otra
  /// app que la quiera usar.
  Future<void> liberar();
}
