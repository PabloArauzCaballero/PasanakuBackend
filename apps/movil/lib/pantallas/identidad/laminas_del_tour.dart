import 'package:aportaya_diseno/ilustraciones/arte_en_custodia.dart';
import 'package:aportaya_diseno/ilustraciones/arte_fondo_de_garantia.dart';
import 'package:aportaya_diseno/ilustraciones/arte_sin_cuaderno.dart';
import 'package:aportaya_diseno/ilustraciones/arte_sorteo_a_la_vista.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// Una lámina del tour: el dibujo, el título en dos líneas —la segunda destacada— y
/// el párrafo.
typedef Lamina = ({
  CustomPainter Function(Tokens) arte,
  String etiqueta,
  String titulo,
  String destacado,
  String texto,
});

/// Las cuatro láminas de la maqueta (`const TOUR`), en su orden: qué es esto, dónde
/// está la plata, cómo se decide el turno, y qué pasa si alguien no pone.
const List<Lamina> laminasDelTour = [
  (
    arte: ArteSinCuaderno.new,
    etiqueta: 'Tres personas y el aporte en común',
    titulo: TextosIdentidad.tour1Titulo,
    destacado: TextosIdentidad.tour1Destacado,
    texto: TextosIdentidad.tour1Texto,
  ),
  (
    arte: ArteEnCustodia.new,
    etiqueta: 'Un extracto con comprobante y el escudo de la custodia',
    titulo: TextosIdentidad.tour2Titulo,
    destacado: TextosIdentidad.tour2Destacado,
    texto: TextosIdentidad.tour2Texto,
  ),
  (
    arte: ArteSorteoALaVista.new,
    etiqueta: 'La rueda del sorteo y su marca de inicio',
    titulo: TextosIdentidad.tour3Titulo,
    destacado: TextosIdentidad.tour3Destacado,
    texto: TextosIdentidad.tour3Texto,
  ),
  (
    arte: ArteFondoDeGarantia.new,
    etiqueta: 'Un paraguas cubriendo la moneda de la cuota',
    titulo: TextosIdentidad.tour4Titulo,
    destacado: TextosIdentidad.tour4Destacado,
    texto: TextosIdentidad.tour4Texto,
  ),
];
