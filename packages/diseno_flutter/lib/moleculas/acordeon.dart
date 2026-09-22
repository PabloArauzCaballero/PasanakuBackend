import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Sección plegable (reglamento, condiciones de un vale).
class Acordeon extends StatelessWidget {
  const Acordeon({
    super.key,
    required this.titulo,
    required this.child,
    this.abierto = false,
  });
  final String titulo;
  final Widget child;
  final bool abierto;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Theme(
      data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
      child: ExpansionTile(
        title: Text(
          titulo,
          style: Theme.of(
            context,
          ).textTheme.titleSmall?.copyWith(color: t.text),
        ),
        initiallyExpanded: abierto,
        iconColor: t.brandTexto,
        collapsedIconColor: t.text3,
        tilePadding: EdgeInsets.zero,
        childrenPadding: const EdgeInsets.only(bottom: Espacio.s3),
        children: [child],
      ),
    );
  }
}
