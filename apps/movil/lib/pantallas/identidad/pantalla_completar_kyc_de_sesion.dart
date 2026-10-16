import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../proveedores/sesion.dart';
import 'pantalla_completar_kyc.dart';

/// Resuelve el id desde la sesión; nunca se confía en un usuarioId de la navegación.
class PantallaCompletarKycDeSesion extends ConsumerWidget {
  const PantallaCompletarKycDeSesion({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    ref.watch(generacionDeSesionProvider);
    final usuarioId = ref.read(sesionProvider).usuarioIdActual;
    if (usuarioId == null) {
      return const Scaffold(
        body: Center(
          child: Text('Iniciá sesión para completar tu verificación.'),
        ),
      );
    }
    return PantallaCompletarKyc(usuarioId: usuarioId);
  }
}
