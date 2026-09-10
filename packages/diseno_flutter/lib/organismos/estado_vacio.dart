import 'package:flutter/material.dart';

import '../organismos/motivo_vacio.dart';
import '../tokens/tokens.dart';

/// Dice por qué no hay nada y qué hacer. Nunca «no hay datos».
class EstadoVacio extends StatelessWidget {
  const EstadoVacio({
    super.key,
    required this.mensaje,
    this.motivo = MotivoVacio.sinDatos,
    this.accion,
  });

  final String mensaje;
  final MotivoVacio motivo;
  final Widget? accion;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final icono = switch (motivo) {
      MotivoVacio.sinDatos => Icons.inbox_outlined,
      MotivoVacio.porFiltro => Icons.filter_alt_off_outlined,
      MotivoVacio.porPermiso => Icons.lock_outline,
    };
    return Padding(
      padding: const EdgeInsets.all(Espacio.s5),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icono, size: Espacio.s7, color: t.text3),
          const SizedBox(height: Espacio.s3),
          Text(
            mensaje,
            textAlign: TextAlign.center,
            style: TextStyle(color: t.text2),
          ),
          if (accion != null) ...[const SizedBox(height: Espacio.s4), accion!],
        ],
      ),
    );
  }
}
