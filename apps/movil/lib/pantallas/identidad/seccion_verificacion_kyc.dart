import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/kyc_del_titular.dart';

/// Estado propio y acción de completar evidencia faltante, dentro de Perfil.
class SeccionVerificacionKyc extends ConsumerWidget {
  const SeccionVerificacionKyc({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(kycDeLaSesionProvider);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'Verificación de identidad (KYC)',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            estado.when(
              loading: () => const Text('Consultando tu verificación…'),
              error: (_, _) => TextButton(
                onPressed: () => ref.invalidate(kycDeLaSesionProvider),
                child: const Text('No pudimos consultar el estado. Reintentar'),
              ),
              data: (expediente) {
                if (expediente == null) {
                  return const Text(
                    'Iniciá sesión para consultar tu verificación.',
                  );
                }
                final faltantes = pendientesDeKyc(expediente);
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text(etiquetaEstadoKyc(expediente.estado)),
                    if (faltantes.isNotEmpty &&
                        kycAdmiteCorreccion(expediente)) ...[
                      const SizedBox(height: 6),
                      Text(
                        'Para completar tu expediente falta: ${faltantes.join(', ')}.',
                      ),
                      const SizedBox(height: 8),
                      OutlinedButton.icon(
                        onPressed: () =>
                            context.push('/identidad/perfil/verificacion'),
                        icon: const Icon(Icons.fact_check_outlined),
                        label: const Text('Completar expediente'),
                      ),
                    ] else if (kycAdmiteCorreccion(expediente))
                      const Text(
                        'Tu expediente está completo. La aprobación sigue siendo manual.',
                      ),
                    if (expediente.estado ==
                        EstadoDeVerificacionEstadoEnum.RECHAZADA)
                      const Text(
                        'Revisá el motivo informado y contactá soporte para conocer los próximos pasos.',
                      ),
                  ],
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}
