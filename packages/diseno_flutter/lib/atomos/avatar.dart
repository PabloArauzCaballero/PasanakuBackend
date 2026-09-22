import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Avatar con iniciales; 24/32/40/56.
class Avatar extends StatelessWidget {
  const Avatar({
    super.key,
    required this.nombre,
    this.tamano = Espacio.s7 - Espacio.s2,
    this.imagen,
  });

  final String nombre;
  final double tamano;
  final ImageProvider? imagen;

  String get _iniciales {
    final partes = nombre
        .trim()
        .split(RegExp(r'\s+'))
        .where((p) => p.isNotEmpty)
        .toList();
    if (partes.isEmpty) return '?';
    return partes.take(2).map((p) => p[0].toUpperCase()).join();
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      label: nombre,
      image: imagen != null,
      excludeSemantics: true,
      child: CircleAvatar(
        radius: tamano / 2,
        backgroundColor: t.brand,
        foregroundColor: t.sobreVerdeSolido,
        backgroundImage: imagen,
        child: imagen == null
            ? Text(
                _iniciales,
                style: TextStyle(
                  fontFamily: Fuente.display,
                  fontWeight: FontWeight.w600,
                  fontSize: tamano * 0.4,
                ),
              )
            : null,
      ),
    );
  }
}
