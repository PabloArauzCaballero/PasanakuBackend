/// Registrar el dispositivo para avisos push y enterarse de qué pantalla abrir al
/// tocar uno. **La bandeja de `/notificaciones/bandeja` es la fuente de verdad**
/// (ADR-035): el push es un aviso de que hay algo nuevo, nunca el único lugar donde
/// vive. La implementación (FCM en Android, APNs en iOS) vive en `infraestructura/`.
abstract interface class AvisosPush {
  /// El identificador que el backend usa para mandar avisos a este dispositivo.
  /// `null` si el dispositivo no pudo registrarse (sin Google Play Services, por
  /// ejemplo): la app sigue funcionando, solo que sin push — la bandeja alcanza.
  Future<String?> token();

  /// Avisos entrantes con la app abierta, con la ruta a la que hay que navegar.
  Stream<String> get toques;
}
