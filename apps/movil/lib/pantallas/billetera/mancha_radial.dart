import 'package:flutter/material.dart';

/// Un círculo con un degradado radial que se apaga hacia afuera — las dos manchas
/// decorativas de `TarjetaDeSaldo`, fieles a `.saldo::before`/`::after` de la maqueta
/// (`docs/Views/AportaYa-Maqueta.html`). `IgnorePointer` porque son puramente visuales.
class ManchaRadial extends StatelessWidget {
  const ManchaRadial({
    super.key,
    required this.diametro,
    required this.colorCentro,
  });
  final double diametro;
  final Color colorCentro;

  @override
  Widget build(BuildContext context) {
    return IgnorePointer(
      child: Container(
        width: diametro,
        height: diametro,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          gradient: RadialGradient(
            colors: [colorCentro, colorCentro.withValues(alpha: 0)],
            stops: const [0, 0.68],
          ),
        ),
      ),
    );
  }
}
