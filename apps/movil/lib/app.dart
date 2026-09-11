import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/verificacion_contrato.dart';
import 'navegacion/rutas.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';

class AppAportaYa extends ConsumerWidget {
  AppAportaYa({super.key, String inicial = '/billetera/inicio'})
    : _enrutador = crearEnrutador(inicial: inicial);

  final GoRouter _enrutador;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // Se dispara al construir el árbol, sin bloquear el primer frame: si el
    // gateway está caído al iniciar, el arranque no se cuelga esperándolo.
    ref.watch(verificacionContratoProvider);
    return MaterialApp.router(
      title: 'AportaYa',
      theme: temaDesde(Tokens.claro, Brightness.light),
      darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
      themeMode: ThemeMode.system,
      routerConfig: _enrutador,
      debugShowCheckedModeBanner: false,
      builder: (context, child) => _AvisoDeContrato(child: child),
    );
  }
}

/// Una franja angosta arriba, no un diálogo que tapa la pantalla: el aviso de
/// «actualizá la app» no puede impedir seguir usando lo que ya funciona.
class _AvisoDeContrato extends ConsumerWidget {
  const _AvisoDeContrato({required this.child});
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
