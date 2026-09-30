import 'package:aportaya_diseno/atomos/campo_o_t_p.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_sesion.dart';
import 'textos.dart';
import '../../navegacion/retorno_de_invitacion.dart';

/// CU-04, paso 2: el segundo factor. Seis dígitos, el mismo átomo del celular en
/// el alta — es a propósito, es el mismo patrón (`CampoOTP`).
///
/// Antes esta pantalla avanzaba con cualquier código: guardaba los seis dígitos en
/// memoria y navegaba. Ahora los manda al servidor y **solo avanza si el servidor los
/// acepta**; si no, el error se muestra acá y el campo queda listo para reintentar.
class PantallaDeMfa extends ConsumerWidget {
  const PantallaDeMfa({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(sesionIdentidadProvider);
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const CabeceraDeSeccion(titulo: TextosIdentidad.tituloMfa),
              const SizedBox(height: Espacio.s5),
              CampoOTP(
                error: estado.error,
                onCompleto: (codigo) async {
                  final ok = await ref
                      .read(sesionIdentidadProvider.notifier)
                      .completarMfa(codigo);
                  if (!ok || !context.mounted) return;
                  final retorno = retornoDeInvitacion(
                    GoRouterState.of(context).uri.queryParameters['volver'],
                  );
                  context.go(retorno ?? '/billetera/inicio');
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
