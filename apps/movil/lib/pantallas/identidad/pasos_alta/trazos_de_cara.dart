import 'package:flutter/widgets.dart';

/// Trazos de la guía de rostro en un lienzo normalizado de 300 × 300.
///
/// Un solo óvalo para las tres poses, como las apps bancarias: la persona encaja la
/// cara y gira la cabeza; no se le dibuja una cara (orejas, nariz) que no es la suya.
/// Proporción 0,73 (ancho / alto), la de un rostro adulto con frente y mentón.
const ovaloDeRostro = Rect.fromLTWH(55, 20, 190, 260);

Path contornoDeRostro() => Path()..addOval(ovaloDeRostro);

/// Tres chevrones a la izquierda del óvalo, apuntando hacia afuera: «girá hacia acá».
/// El perfil derecho los recibe espejados.
Path flechasDeGiro() {
  final camino = Path();
  const centroY = 150.0;
  const alto = 16.0;
  for (final x in [34.0, 20.0, 6.0]) {
    camino
      ..moveTo(x + 9, centroY - alto)
      ..lineTo(x, centroY)
      ..lineTo(x + 9, centroY + alto);
  }
  return camino;
}
