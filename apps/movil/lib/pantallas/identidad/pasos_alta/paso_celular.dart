import 'package:aportaya_diseno/atomos/campo_o_t_p.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/estado_alta.dart';
import '../textos.dart';

/// Paso 2 de 8 — confirmar celular con un código de seis dígitos (CU-01, flujo 1).
/// La validez del código la decide el servidor; acá solo se junta el dato.
class PasoCelular extends ConsumerWidget {
  const PasoCelular({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(altaProvider);
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(TextosIdentidad.codigoEnviado),
          const SizedBox(height: Espacio.s4),
          CampoOTP(
            onCompleto: (_) =>
                ref.read(altaProvider.notifier).confirmarCelular(),
          ),
          const SizedBox(height: Espacio.s5),
          Boton(
            texto: TextosIdentidad.continuar,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: estado.codigoConfirmado
                ? () => ref.read(altaProvider.notifier).siguiente()
                : null,
          ),
        ],
      ),
    );
  }
}
