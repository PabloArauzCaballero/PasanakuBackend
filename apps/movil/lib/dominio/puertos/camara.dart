/// Capturar una imagen para el cotejo documental (CU-01) o el QR de recarga.
/// **F2 la declara sin usuario todavía** — el primero es F3 — porque los siete
/// puertos se fijan antes de la Ola F2 y un puerto nuevo en T5 sería un micro-PR al
/// shell que los tres carriles de pantallas tendrían que rebasar (ficha `F2`).
abstract interface class Camara {
  /// La ruta del archivo capturado, o `null` si la persona canceló.
  Future<String?> capturar({required bool esDocumento});
}
