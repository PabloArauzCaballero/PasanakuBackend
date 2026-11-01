import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'superficie_viva.dart';

/// Acción principal flotante: círculo de 56 dp en verde de marca (ADR-053). Una por
/// pantalla, si hay. Lleva la piel del botón primario (`SuperficieViva`): profundidad,
/// resorte y brillo.
class BotonFlotante extends StatelessWidget {
  const BotonFlotante({
    super.key,
    required this.icono,
    required this.etiqueta,
    required this.onPressed,
  });

  final IconData icono;
  final String etiqueta;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return SuperficieViva(
      color: t.verdeSolido,
      colorDelTexto: t.sobreVerdeSolido,
      radio: Radios.pill,
      brilla: true,
      child: FloatingActionButton(
        onPressed: onPressed,
        tooltip: etiqueta,
        elevation: 0,
        focusElevation: 0,
        hoverElevation: 0,
        highlightElevation: 0,
        backgroundColor: Colors.transparent,
        foregroundColor: t.sobreVerdeSolido,
        shape: const CircleBorder(),
        child: Icon(icono),
      ),
    );
  }
}
