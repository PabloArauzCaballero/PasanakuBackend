import 'package:flutter/material.dart';

import '../atomos/avatar.dart';
import '../atomos/chip_estado.dart';
import '../atomos/progreso.dart';
import '../atomos/tono.dart';
import '../moleculas/tarjeta.dart';
import '../tokens/tokens.dart';

/// Un grupo en una línea: avatar, nombre, rol en ese grupo, avance y estado.
class ItemPasanaku extends StatelessWidget {
  const ItemPasanaku({
    super.key,
    required this.nombre,
    required this.rol,
    required this.avance,
    required this.estado,
    this.onTap,
    this.tono = Tono.ok,
  });
  final String nombre;

  /// «Organizás este grupo» / «Organiza Rosa A.» — el rol es del vínculo, no de la persona (D-13).
  final String rol;
  final double avance;
  final String estado;
  final Tono tono;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Tarjeta(
      onTap: onTap,
      etiqueta: 'Grupo $nombre, $estado',
      child: Row(
        children: [
          Avatar(nombre: nombre),
          const SizedBox(width: Espacio.s3),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(nombre, style: texto.titleMedium?.copyWith(color: t.text)),
                Text(rol, style: texto.bodySmall?.copyWith(color: t.text3)),
                const SizedBox(height: Espacio.s2),
                Progreso(valor: avance, etiqueta: 'Avance del ciclo'),
              ],
            ),
          ),
          const SizedBox(width: Espacio.s3),
          ChipEstado(texto: estado, tono: tono),
        ],
      ),
    );
  }
}
