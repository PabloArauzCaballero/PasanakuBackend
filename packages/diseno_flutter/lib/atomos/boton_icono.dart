import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'hundido_al_tocar.dart';

/// Botón de ícono redondo (44 dp visibles sobre una superficie elevada, 48 dp
/// táctiles) para cabeceras y barras: el lenguaje de la app bancaria (ADR-053).
class BotonIcono extends StatelessWidget {
  const BotonIcono({
    super.key,
    required this.icono,
    required this.etiqueta,
    required this.onPressed,
  });

  final IconData icono;

  /// Lo que dice el lector: un botón de ícono sin etiqueta no existe para nadie.
  final String etiqueta;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return HundidoAlTocar(
      activo: onPressed != null,
      child: IconButton(
        onPressed: onPressed,
        icon: Icon(icono, size: Espacio.s5 - Espacio.s1 / 2),
        tooltip: etiqueta,
        color: t.text,
        disabledColor: t.text3,
        style: IconButton.styleFrom(
          backgroundColor: t.surface,
          shape: CircleBorder(
            side: Theme.of(context).brightness == Brightness.dark
                ? BorderSide(color: t.border, width: Borde.fino)
                : BorderSide.none,
          ),
          shadowColor: t.sombra1.color,
          elevation: 1,
        ),
        constraints: const BoxConstraints.tightFor(
          width: Tactil.minimo,
          height: Tactil.minimo,
        ),
      ),
    );
  }
}
