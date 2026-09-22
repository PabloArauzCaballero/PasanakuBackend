/// Estar «conectado a wifi» no es tener internet: el adaptador sondea el gateway.
abstract interface class Conectividad {
  Stream<bool> get cambios;
  Future<bool> hayConexion();
}
