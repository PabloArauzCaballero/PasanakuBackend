/// Lo que el sistema de diseño necesita saber de un error para pintarlo: el mensaje ya
/// traducido, la traza para soporte y si fue la red. La app implementa esto con sus
/// `ErrorDeApi` y `ErrorDeRed`; el paquete no conoce ni códigos ni HTTP.
abstract interface class ErrorPresentable {
  String get mensaje;
  String? get trazaId;
  bool get sinConexion;
}
