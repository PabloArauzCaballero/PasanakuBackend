import 'package:flutter/material.dart';

import '../atomos/avatar.dart';
import '../tokens/tokens.dart';

/// Publicidad rotulada, **sin cierre**, formato único: dos líneas y un monograma en
/// marco fijo (D-7). Solo en la portada, al pie.
class BannerDePauta extends StatelessWidget {
  const BannerDePauta({
    super.key,
    required this.anunciante,
    required this.titulo,
    required this.detalle,
    required this.onSobreEsteAviso,
    this.onTap,
  });
  final String anunciante;
  final String titulo;
  final String detalle;
  final VoidCallback onSobreEsteAviso;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    assert(
      titulo.length <= 46 && detalle.length <= 60,
      'formato único: título ≤ 46, detalle ≤ 60',
    );
    return Semantics(
      label: 'Publicidad de $anunciante: $titulo. $detalle',
      child: Container(
        padding: const EdgeInsets.all(Espacio.s3),
        decoration: BoxDecoration(
          color: t.surface,
          borderRadius: BorderRadius.circular(Radios.lg),
          border: Border.all(color: t.border, width: Borde.fino),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text(
                  'Publicidad · $anunciante',
                  style: texto.labelSmall?.copyWith(color: t.text3),
                ),
                const Spacer(),
                TextButton(
                  onPressed: onSobreEsteAviso,
                  child: const Text('Sobre este aviso'),
                ),
              ],
            ),
            InkWell(
              onTap: onTap,
              child: Row(
                children: [
                  Avatar(nombre: anunciante, tamano: Tactil.minimo),
                  const SizedBox(width: Espacio.s3),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          titulo,
                          style: texto.titleSmall?.copyWith(color: t.text),
                        ),
                        Text(
                          detalle,
                          style: texto.bodySmall?.copyWith(color: t.text2),
                        ),
                      ],
                    ),
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
