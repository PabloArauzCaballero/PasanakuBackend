import 'package:aportaya_diseno/atomos/campo_o_t_p.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/estado_alta.dart';
import '../textos.dart';
import '../textos_del_alta.dart';
import 'bloque_de_formulario.dart';
import 'tarjeta_de_confianza.dart';

/// Paso de confirmación del correo con un código de seis dígitos (CU-01, flujo 1).
/// La validez del código la decide el servidor; acá solo se junta el dato.
class PasoCorreo extends ConsumerWidget {
  const PasoCorreo({super.key});

  /// El destino enmascarado mientras llega la respuesta del servidor.
  static String _aDonde(DatosPersonales datos) {
    if (datos.canalVerificacion == 'CORREO') {
      final partes = datos.correo.split('@');
      final nombre = partes.first;
      final visible = nombre.length <= 2 ? nombre : nombre.substring(0, 2);
      final dominio = partes.length > 1 ? '@${partes.last}' : '';
      return '${TextosIdentidad.codigoPorCorreo} $visible•••$dominio';
    }
    final telefono = datos.telefono;
    final ultimos = telefono.length >= 2
        ? telefono.substring(telefono.length - 2)
        : telefono;
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
          BloqueDeFormulario(
            icono: Icons.mail_outline_rounded,
            titulo: 'Confirmar correo',
            hijos: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    estado.destinoVerificacion == null
                        ? _aDonde(estado.datos)
                        : '${TextosIdentidad.codigoPorCorreo} ${estado.destinoVerificacion}',
                  ),
                  const SizedBox(height: Espacio.s2),
                  Text(
                    TextosIdentidad.codigoNoLlega,
                    style: Tipo.ayuda.copyWith(color: Tokens.of(context).text3),
                  ),
                  const SizedBox(height: Espacio.s4),
                  CampoOTP(
                    ejemplo: TextosDelAlta.ejemploCodigo,
                    error: estado.errorCodigo?.isEmpty == true
                        ? null
                        : estado.errorCodigo,
                    onCompleto: (codigo) => ref
                        .read(altaProvider.notifier)
                        .confirmarCodigoCorreo(codigo),
                  ),
                  const SizedBox(height: Espacio.s5),
                  Boton(
                    texto: estado.verificandoCodigo
                        ? TextosIdentidad.verificandoCodigo
                        : TextosIdentidad.continuar,
                    variante: BotonVariante.primario,
                    expandido: true,
                    cargando: estado.verificandoCodigo,
                    onPressed: estado.codigoConfirmado
                        ? () => ref.read(altaProvider.notifier).siguiente()
                        : null,
                  ),
                  const SizedBox(height: Espacio.s3),
                  Boton(
                    texto: estado.enviandoCodigo
                        ? TextosIdentidad.enviandoCodigo
                        : TextosIdentidad.reenviarCodigo,
                    variante: BotonVariante.fantasma,
                    expandido: true,
                    cargando: estado.enviandoCodigo,
                    onPressed: estado.verificandoCodigo
                        ? null
                        : () => ref
                              .read(altaProvider.notifier)
                              .solicitarCodigoCorreo(nuevo: true),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: Espacio.s4),
          const TarjetaDeConfianza(),
        ],
      ),
    );
  }
}
