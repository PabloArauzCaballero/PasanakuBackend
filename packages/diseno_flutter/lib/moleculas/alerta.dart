import 'package:flutter/material.dart';

import '../atomos/tono.dart';
import '../tokens/tokens.dart';

/// Aviso en la pantalla, con tono. **Persistente y no bloqueante** cuando es una
/// restricción (CU-27): explica, no castiga en silencio.
class Alerta extends StatelessWidget {
  const Alerta({
    super.key,
    required this.titulo,
    this.detalle,
    this.tono = Tono.info,
    this.accion,
  });
  final String titulo;
  final String? detalle;
  final Tono tono;
  final Widget? accion;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final (fondo, frente) = tono.coloresDe(t);
    final texto = Theme.of(context).textTheme;
    final icono = switch (tono) {
      Tono.ok => Icons.check_circle_outline,
      Tono.aviso => Icons.warning_amber_outlined,
      Tono.error => Icons.error_outline,
      _ => Icons.info_outline,
    };
    return Semantics(
      liveRegion: tono == Tono.error,
      child: Container(
        padding: const EdgeInsets.all(Espacio.s4),
        decoration: BoxDecoration(
          color: fondo,
          borderRadius: BorderRadius.circular(Radios.md),
          border: Border.all(
            color: frente.withValues(alpha: 0.3),
            width: Borde.fino,
          ),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icono, color: frente),
            const SizedBox(width: Espacio.s3),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    titulo,
                    style: texto.titleSmall?.copyWith(color: frente),
                  ),
                  if (detalle != null)
                    Text(
                      detalle!,
                      style: texto.bodyMedium?.copyWith(color: t.text),
                    ),
                  if (accion != null)
                    Padding(
                      padding: const EdgeInsets.only(top: Espacio.s2),
                      child: accion,
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
