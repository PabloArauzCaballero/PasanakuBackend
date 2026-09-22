import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// La acción principal de una pantalla, anclada abajo y siempre en el mismo lugar.
///
/// Un botón al final de un formulario largo obliga a desplazarse para descubrir que
/// existe, y en un teléfono chico queda debajo del teclado. Acá vive fuera del scroll,
/// sobre la superficie, respetando el área segura del teléfono.
class BarraDeAccion extends StatelessWidget {
  const BarraDeAccion({super.key, required this.hijo, this.nota});

  final Widget hijo;

  /// Una línea de contexto encima del botón («Se debita de tu saldo disponible»).
  final String? nota;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Container(
      decoration: BoxDecoration(
        color: t.surface,
        border: Border(
          top: BorderSide(color: t.border, width: Borde.fino),
        ),
      ),
      child: SafeArea(
        top: false,
        minimum: const EdgeInsets.fromLTRB(
          Espacio.s4,
          Espacio.s3,
          Espacio.s4,
          Espacio.s3,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (nota != null) ...[
              Text(
                nota!,
                textAlign: TextAlign.center,
                style: Tipo.ayuda.copyWith(color: t.text2),
              ),
              const SizedBox(height: Espacio.s2),
            ],
            SizedBox(width: double.infinity, child: hijo),
          ],
        ),
      ),
    );
  }
}
