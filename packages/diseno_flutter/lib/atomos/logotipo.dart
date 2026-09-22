import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'marca.dart';
import 'palabra_de_marca.dart';

/// **El logotipo entero**: el isotipo y, debajo, la palabra «AportaYa».
///
/// [Marca] sola es el isotipo, y alcanza para un sello de 72 px o un ícono en una
/// cabecera. En grande no: tres trazos abiertos, sin la palabra, se leen como una
/// forma abstracta y no como el logo de nadie. Cuando la marca es la protagonista
/// —la apertura, el zoom de entrada— va el logotipo completo.
///
/// Va apilado y no en fila como `docs/Views/AportaYa-logo-horizontal.svg`: un teléfono
/// es alto y angosto, y el encuadre horizontal deja la marca chica justo cuando se la
/// quiere ver grande.
class Logotipo extends StatelessWidget {
  const Logotipo({
    super.key,
    this.tamano = 96,
    this.colorDelTrazo,
    this.colorDeLaPalabra,
    this.avance = 1,
    this.opacidadDeLaPalabra = 1,
    this.volumen = false,
  });

  /// El lado del isotipo. La palabra se dimensiona en proporción a esto.
  final double tamano;

  /// El color de los tres trazos exteriores. Por defecto, el verde de marca; sobre
  /// relleno verde sólido se le pasa [Tokens.sobreVerdeSolido].
  final Color? colorDelTrazo;

  /// El color de «Aporta», si va distinto del trazo.
  final Color? colorDeLaPalabra;

  /// Cuánto del trazo está dibujado, de 0 a 1 — lo usa la apertura.
  final double avance;

  /// Para que la palabra entre después que el isotipo, sin mover nada de lugar.
  final double opacidadDeLaPalabra;

  /// Degradado en los trazos del isotipo. Ver [PintorDeMarca.volumen].
  final bool volumen;

  /// El ancho de la palabra respecto del isotipo. En el SVG el isotipo mide 134 de
  /// ancho y «AportaYa» 390; apilada, esa relación la deja desbordada, así que se
  /// recorta a poco más del doble.
  static const _anchoDeLaPalabra = 1.9;

  @override
  Widget build(BuildContext context) => Semantics(
    label: 'AportaYa',
    image: true,
    child: ExcludeSemantics(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Marca(
            tamano: tamano,
            colorDelTrazo: colorDelTrazo,
            avance: avance,
            volumen: volumen,
          ),
          SizedBox(height: tamano * 0.14),
          Opacity(
            opacity: opacidadDeLaPalabra.clamp(0.0, 1.0),
            child: PalabraDeMarca(
              ancho: tamano * _anchoDeLaPalabra,
              color: colorDeLaPalabra ?? colorDelTrazo,
            ),
          ),
        ],
      ),
    ),
  );
}
