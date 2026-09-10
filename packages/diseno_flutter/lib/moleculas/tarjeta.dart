import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Superficie base de toda tarjeta: radio, borde y sombra de los tokens.
class Tarjeta extends StatelessWidget {
  const Tarjeta({
    super.key,
    required this.child,
    this.relleno = true,
    this.onTap,
    this.etiqueta,
  });
  final Widget child;
  final bool relleno;
  final VoidCallback? onTap;
  final String? etiqueta;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final cuerpo = Container(
      width: double.infinity,
      padding: relleno ? const EdgeInsets.all(Espacio.s4) : EdgeInsets.zero,
      decoration: BoxDecoration(
        color: t.surface,
        borderRadius: BorderRadius.circular(Radios.lg),
        border: Border.all(color: t.border, width: Borde.fino),
        boxShadow: [t.sombra1],
      ),
      child: child,
    );
    if (onTap == null) return cuerpo;
    return Semantics(
      button: true,
      label: etiqueta,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(Radios.lg),
        child: cuerpo,
      ),
    );
  }
}
