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
  /// ofrece «Tomar foto» a mano.
  Future<bool> escucharRostros(void Function(LecturaDeRostro?) alLeer);

  /// Deja de leer el video. Se llama antes de [tomarFoto] y al salir.
  Future<void> dejarDeEscuchar();

  /// Libera el sensor. Se llama siempre al salir de la pantalla de cámara, haya
  /// foto o no — una cámara que queda abierta es batería y es el candado de otra
  /// app que la quiera usar.
  Future<void> liberar();
}

/// Lo que el detector vio en un cuadro, ya en la orientación de la pantalla y en
/// fracciones del cuadro (0 a 1), para que el juez no dependa de la resolución.
///
/// [giro] es en grados y **positivo hacia la izquierda de la persona**, sea cual sea
/// la plataforma: iOS entrega el video de la cámara delantera espejado y Android no,
/// y el adaptador lo corrige antes de que llegue acá.
class LecturaDeRostro {
  const LecturaDeRostro({
    required this.rostros,
    required this.centroX,
    required this.centroY,
    required this.ancho,
    required this.giro,
    this.inclinacion = 0,
    this.ojosAbiertos,
  });

  /// Cuántas caras hay en el cuadro; las demás medidas son de la más grande.
  final int rostros;
  final double centroX;
  final double centroY;

  /// Ancho de la cara sobre el ancho del cuadro.
  final double ancho;
  final double giro;

  /// Cabeza hacia arriba o abajo, en grados.
  final double inclinacion;

  /// La menor de las dos probabilidades de ojo abierto, si el detector la dio.
  final double? ojosAbiertos;
}
