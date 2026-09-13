import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Los colores con los que se distingue **a una persona de otra** dentro de una rueda:
/// el turno 1 no es el turno 2, y en una lista de diez hay que poder señalar el propio
/// de un vistazo.
///
/// Salen todos de la paleta de AportaYa —los dos verdes vivos, los cuatro naranjas y
/// los dos semánticos que no son rojo—, en un orden pensado para que dos vecinos nunca
/// queden del mismo tono. Nunca pintan un botón ni un fondo: solo personas y turnos,
/// porque el naranja de acción tiene que seguir queriendo decir «tocá acá».
///
/// El color **no es la única señal**: el turno propio además lleva un punto, y el que
/// cobra ahora lleva su etiqueta. Quien no distingue estos tonos no se queda afuera.
abstract final class ColoresDeTurno {
  static const _rueda = [
    Paleta.g500,
    Paleta.o500,
    Paleta.info,
    Paleta.g300,
    Paleta.o700,
    Paleta.g700,
    Paleta.warn,
    Paleta.g400,
    Paleta.o400,
    Paleta.g600,
  ];

  /// El color del turno `indice` (base 0). Se repite cada diez: una rueda de veinte
  /// vuelve a empezar, y a esa altura el número del turno ya es la señal que manda.
  static Color de(int indice) => _rueda[indice % _rueda.length];

  /// Cuántos colores distintos hay antes de repetir.
  static int get cuantos => _rueda.length;
}
