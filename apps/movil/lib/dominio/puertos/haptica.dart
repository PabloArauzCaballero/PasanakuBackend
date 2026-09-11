/// Retroalimentación táctil corta, para confirmar un toque de dinero o marcar un
/// error de validación sin depender solo de la vista (accesibilidad).
abstract interface class Haptica {
  Future<void> exito();
  Future<void> error();
  Future<void> toqueLigero();
}
