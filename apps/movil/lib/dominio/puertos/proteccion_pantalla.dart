/// Bloquear captura y grabación de pantalla en las vistas con saldo o datos
/// personales (regla 3 del shell). `go_router` lo activa y desactiva por ruta al
/// entrar y salir — nunca queda prendido para toda la app.
abstract interface class ProteccionPantalla {
  Future<void> activar();
  Future<void> desactivar();
}
