import 'package:aportaya_diseno/atomos/campo_o_t_p.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_sesion.dart';
import 'textos.dart';

/// CU-04, paso 2: el segundo factor. Seis dígitos, el mismo átomo del celular en
/// el alta — es a propósito, es el mismo patrón (`CampoOTP`).
class PantallaDeMfa extends ConsumerWidget {
  const PantallaDeMfa({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(title: const Text(TextosIdentidad.tituloMfa)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: CampoOTP(
            onCompleto: (codigo) {
              ref.read(sesionIdentidadProvider.notifier).completarMfa(codigo);
              context.go('/identidad/dispositivos');
            },
          ),
        ),
      ),
    );
  }
}
