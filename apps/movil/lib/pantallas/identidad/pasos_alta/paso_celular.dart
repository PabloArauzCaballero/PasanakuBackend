import 'package:aportaya_diseno/atomos/campo_o_t_p.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/estado_alta.dart';
import '../textos.dart';

/// Paso 2 de 8 — confirmar el contacto con un código de seis dígitos (CU-01, flujo 1).
/// La validez del código la decide el servidor; acá solo se junta el dato.
///
/// **Dice a dónde se mandó.** Un «te enviamos un código» sin decir adónde deja a
/// alguien mirando el teléfono cuando el código le llegó al correo, o al revés. Y el
/// destino va enmascarado: la pantalla la puede estar mirando alguien más.
class PasoCelular extends ConsumerWidget {
  const PasoCelular({super.key});

  /// El destino, enmascarado: `+591 7••• ••90` o `ma•••@correo.com`.
  static String _aDonde(DatosPersonales d) {
    if (d.canalVerificacion == 'CORREO') {
      final partes = d.correo.split('@');
      final nombre = partes.first;
      final visible = nombre.length <= 2 ? nombre : nombre.substring(0, 2);
      final dominio = partes.length > 1 ? '@${partes.last}' : '';
      return '${TextosIdentidad.codigoPorCorreo} $visible•••$dominio';
    }
    final tel = d.telefono;
    final ultimos = tel.length >= 2 ? tel.substring(tel.length - 2) : tel;
    return '${TextosIdentidad.codigoPorSms} +591 •••••• $ultimos';
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(altaProvider);
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(_aDonde(estado.datos)),
          const SizedBox(height: Espacio.s2),
          Text(
            TextosIdentidad.codigoNoLlega,
            style: Tipo.ayuda.copyWith(color: Tokens.of(context).text3),
          ),
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
