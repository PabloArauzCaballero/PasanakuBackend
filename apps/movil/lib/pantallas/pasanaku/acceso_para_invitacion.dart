import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

/// Entrada al flujo de identidad sin mostrar datos del grupo antes de validar.
class AccesoParaInvitacion extends StatelessWidget {
  const AccesoParaInvitacion({super.key, required this.codigo});

  final String codigo;

  @override
  Widget build(BuildContext context) {
    final retorno = Uri.encodeComponent('/pasanaku/unirse/$codigo');
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Ingresá con el teléfono que recibió la invitación para ver el grupo.',
        ),
        const SizedBox(height: Espacio.s4),
        ElevatedButton(
          onPressed: () => context.go('/ingreso?volver=$retorno'),
          child: const Text('Ingresar'),
        ),
        TextButton(
          onPressed: () => context.go('/registro?volver=$retorno'),
          child: const Text('Crear cuenta'),
        ),
      ],
    );
  }
}
