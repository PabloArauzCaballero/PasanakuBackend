/// Lo que el dominio necesita del almacén seguro del dispositivo. La
/// implementación (Keystore en Android, Keychain en iOS) vive en `infraestructura/`.
abstract interface class AlmacenSeguro {
  Future<String?> leer(String clave);
  Future<void> guardar(String clave, String valor);
  Future<void> borrar(String clave);
}
