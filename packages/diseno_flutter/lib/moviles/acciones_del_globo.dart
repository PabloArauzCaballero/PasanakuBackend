import 'package:flutter/material.dart';

import '../atomos/boton.dart';
import '../atomos/boton_tamano.dart';
import '../atomos/boton_variante.dart';
import '../tokens/tokens.dart';

/// Los controles del globo del tutorial: atrás, reintentar, siguiente y salir.
///
/// Vive en su propio archivo porque el globo se pasaba de las 200 líneas, y porque los
/// botones son lo que más cambia cuando se ajusta el recorrido.
class AccionesDelGlobo extends StatelessWidget {
  const AccionesDelGlobo({
    super.key,
    required this.indice,
    required this.textoAvanzar,
    required this.textoSalir,
    required this.alAvanzar,
    required this.alRetroceder,
    required this.alSalir,
    this.alReintentar,
  });

  final int indice;
  final String textoAvanzar;
  final String textoSalir;
  final VoidCallback alAvanzar;
  final VoidCallback alRetroceder;
  final VoidCallback alSalir;
  final VoidCallback? alReintentar;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      Row(
        children: [
          if (indice > 0) ...[
            Expanded(
              child: Boton(
                texto: 'Atrás',
                variante: BotonVariante.secundario,
                tamano: BotonTamano.base,
                onPressed: alRetroceder,
              ),
            ),
            const SizedBox(width: Espacio.s2),
          ],
          if (alReintentar != null) ...[
            Expanded(
              child: Boton(
                texto: 'Reintentar',
                variante: BotonVariante.secundario,
                onPressed: alReintentar,
              ),
            ),
            const SizedBox(width: Espacio.s2),
          ],
          Expanded(
            child: Boton(
              texto: textoAvanzar,
              variante: BotonVariante.primario,
              onPressed: alAvanzar,
            ),
          ),
        ],
      ),
      const SizedBox(height: Espacio.s2),
      Boton(
        texto: textoSalir,
        variante: BotonVariante.fantasma,
        tamano: BotonTamano.sm,
        onPressed: alSalir,
      ),
    ],
  );
}
