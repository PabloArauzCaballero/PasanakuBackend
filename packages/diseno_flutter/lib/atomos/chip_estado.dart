import 'package:flutter/material.dart';

import '../atomos/tono.dart';
import '../tokens/tokens.dart';

/// Estado con nombre y punto: «Al día», «Vencido», «En revisión».
class ChipEstado extends StatelessWidget {
  const ChipEstado({
    super.key,
    required this.texto,
    this.tono = Tono.neutro,
    this.icono,
  });

  final String texto;
  final Tono tono;
  final IconData? icono;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final (fondo, frente) = tono.coloresDe(t);
    return Semantics(
      label: 'Estado: $texto',
      excludeSemantics: true,
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: Espacio.s3,
          vertical: Espacio.s1,
        ),
        decoration: BoxDecoration(
          color: fondo,
          borderRadius: BorderRadius.circular(Radios.pill),
          // Un color de estado en una superficie chica lleva relleno y borde (D-12).
          border: Border.all(
            color: frente.withValues(alpha: 0.35),
            width: Borde.fino,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icono ?? Icons.circle,
              size: icono == null ? Espacio.s2 : Espacio.s4,
              color: frente,
            ),
            const SizedBox(width: Espacio.s2),
            Flexible(
              child: Text(
                texto,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.labelMedium?.copyWith(
                  color: frente,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
