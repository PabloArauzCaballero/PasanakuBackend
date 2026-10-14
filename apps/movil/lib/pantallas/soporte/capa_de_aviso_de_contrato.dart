import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/verificacion_contrato.dart';

/// Aviso angosto y no bloqueante cuando el servidor exige actualizar la app.
class CapaDeAvisoDeContrato extends ConsumerWidget {
  const CapaDeAvisoDeContrato({super.key, required this.child});

  final Widget? child;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(verificacionContratoProvider).asData?.value;
    if (estado != EstadoContrato.desactualizado || child == null) {
      return child ?? const SizedBox.shrink();
    }
    return Column(
      children: [
        Material(
          color: Theme.of(context).colorScheme.errorContainer,
          child: SafeArea(
            bottom: false,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: Text(
                'Hay una versión nueva de AportaYa. Actualizala para seguir '
                'operando sin problemas.',
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ),
          ),
        ),
        Expanded(child: child!),
      ],
    );
  }
}
