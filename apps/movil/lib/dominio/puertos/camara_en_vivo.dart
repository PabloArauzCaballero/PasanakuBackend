import 'package:flutter/widgets.dart';
import '../../pantallas/identidad/dominio/lectura_de_rostro.dart';

export '../../pantallas/identidad/dominio/lectura_de_rostro.dart';

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

  /// Prepara el sensor pedido. Se llama después de tener el permiso. Con
  /// [conDeteccion], el video queda en el formato que lee el detector de rostro.
  Future<void> iniciar({required bool frontal, bool conDeteccion = false});

  /// El visor en vivo, ya inicializado. Solo se pide después de [iniciar].
  Widget vista();

  /// La ruta del archivo capturado.
  Future<String> tomarFoto();

  /// Empieza a leer el rostro en cada cuadro del video (prueba de vida). Llama a
  /// [alLeer] con `null` si en el cuadro no hay nadie. Devuelve `false` si en este
  /// dispositivo no hay detector (emulador sin servicios, web): ahí la pantalla
  /// ofrece «Tomar foto ahora».
  Future<bool> escucharRostros(void Function(LecturaDeRostro?) alLeer);

  /// Deja de leer el video. Se llama antes de [tomarFoto] y al salir.
  Future<void> dejarDeEscuchar();

  /// Libera el sensor. Se llama siempre al salir de la pantalla de cámara, haya
  /// foto o no — una cámara que queda abierta es batería y es el candado de otra
  /// app que la quiera usar.
  Future<void> liberar();
}
