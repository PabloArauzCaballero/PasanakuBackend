/// Llevar a la persona a los ajustes de la app en el teléfono, que es el único lugar
/// donde puede reactivar un permiso que ya negó (el sistema no vuelve a preguntar).
abstract interface class AjustesDelSistema {
  /// `true` si se pudo abrir; `false` si el sistema no lo permitió. Nunca lanza.
  Future<bool> abrir();
}
