/// Capturar una imagen para el cotejo documental (CU-01) o el QR de recarga.
/// **F2 la declara sin usuario todavía** — el primero es F3 — porque los siete
/// puertos se fijan antes de la Ola F2 y un puerto nuevo en T5 sería un micro-PR al
/// shell que los tres carriles de pantallas tendrían que rebasar (ficha `F2`).
abstract interface class Camara {
  /// La ruta del archivo capturado, o `null` si la persona canceló.
  Future<String?> capturar({required bool esDocumento});

  /// Una foto que **ya existe** en el teléfono, en vez de sacar una nueva.
  ///
  /// No es solo una comodidad: mucha gente ya tiene la foto de su carnet, y hay
  /// teléfonos con la cámara rota. Y es la única vía en el simulador de iOS, que no
  /// tiene cámara ni puede usar la del Mac —limitación de Apple, no del código—,
  /// así que sin esto el flujo entero no se puede probar sin un teléfono real.
  Future<String?> elegirDeLasFotos();
}
