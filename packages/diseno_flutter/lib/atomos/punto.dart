import 'package:flutter/material.dart';

import '../atomos/tono.dart';
import '../tokens/tokens.dart';

/// El punto de «hay algo nuevo», con cuenta opcional.
class Punto extends StatelessWidget {
  const Punto({super.key, this.cuenta, this.tono = Tono.error});
  final int? cuenta;
  final Tono tono;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final color = tono == Tono.error ? t.err : tono.coloresDe(t).$2;
    return Semantics(
      label: cuenta == null ? 'Hay novedades' : '$cuenta sin leer',
      child: Container(
        constraints: const BoxConstraints(
          minWidth: Espacio.s3,
          minHeight: Espacio.s3,
        ),
        padding: cuenta == null
            ? EdgeInsets.zero
            : const EdgeInsets.symmetric(horizontal: Espacio.s1),
        decoration: BoxDecoration(
          color: color,
          borderRadius: BorderRadius.circular(Radios.pill),
        ),
        child: cuenta == null
            ? null
            : Text(
                '$cuenta',
                style: Theme.of(
                  context,
                ).textTheme.labelSmall?.copyWith(color: t.sobreRojoSolido),
              ),
      ),
    );
  }
}
