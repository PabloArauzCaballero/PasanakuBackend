/// Lectura normalizada del detector, independiente de resolución y plataforma.
class LecturaDeRostro {
  const LecturaDeRostro({
    required this.rostros,
    required this.centroX,
    required this.centroY,
    required this.ancho,
    required this.giro,
    this.inclinacion = 0,
    this.ojosAbiertos,
  });

  final int rostros;
  final double centroX;
  final double centroY;
  final double ancho;
  final double giro;
  final double inclinacion;
  final double? ojosAbiertos;
}
