import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Identificador de soporte asociado a un error API, sin datos de la persona.
class ReferenciaDeError extends StatelessWidget {
  const ReferenciaDeError({super.key, required this.trazaId});

  final String? trazaId;

  @override
  Widget build(BuildContext context) {
    final referencia = trazaId?.trim();
    if (referencia == null || referencia.isEmpty) {
      return const SizedBox.shrink();
    }
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.only(top: Espacio.s2),
      child: Text(
        'Referencia: $referencia',
        style: Theme.of(context).textTheme.bodySmall?.copyWith(color: t.text3),
      ),
    );
  }
}
