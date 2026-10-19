import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/kyc_del_titular.dart';
import 'pantalla_completar_kyc.dart';

/// Resuelve el id desde la sesión; nunca se confía en un usuarioId de la navegación.
class PantallaCompletarKycDeSesion extends ConsumerWidget {
  const PantallaCompletarKycDeSesion({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final usuarioId = ref.watch(usuarioIdDeLaSesionProvider);
    if (usuarioId.isLoading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    if (usuarioId.hasError) {
      return Scaffold(
        body: Center(
          child: TextButton(
            onPressed: () => ref.invalidate(usuarioIdDeLaSesionProvider),
            child: const Text('No pudimos leer tu sesión. Reintentar'),
          ),
        ),
      );
    }
    final id = usuarioId.value;
    if (id == null) {
      return const Scaffold(
        body: Center(
          child: Text('Iniciá sesión para completar tu verificación.'),
        ),
      );
    }
    return PantallaCompletarKyc(usuarioId: id);
  }
}
