/// Verde = estructura, naranja = acción: un solo [BotonVariante.primario] por pantalla.
enum BotonVariante {
  primario,
  secundario,
  fantasma,
  peligro,
  enlace,

  /// Fantasma para usar **sobre el verde sólido** (tarjeta de saldo): texto y borde claros.
  sobreVerde,
}
