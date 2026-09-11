import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/casilla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_sesion.dart';
import 'textos.dart';

/// CU-04, paso 3: ofrecer confiar en el dispositivo por 30 días (evita pedir MFA
/// de nuevo). El listado de dispositivos ya registrados (`GET
/// /sesion/dispositivos`) queda pendiente del cliente Dart de identidad — hueco.
class PantallaDeDispositivos extends ConsumerWidget {
  const PantallaDeDispositivos({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(sesionIdentidadProvider);
    final notifier = ref.read(sesionIdentidadProvider.notifier);
    return Scaffold(
      appBar: AppBar(title: const Text(TextosIdentidad.tusDispositivos)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Casilla(
                etiqueta: TextosIdentidad.dispositivoDeConfianza,
                valor: estado.confiarEnDispositivo,
                onChanged: notifier.elegirConfianza,
              ),
              const SizedBox(height: Espacio.s5),
              Boton(
                texto: TextosIdentidad.continuar,
                variante: BotonVariante.primario,
                expandido: true,
                onPressed: () => context.go('/billetera/inicio'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
