import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/moviles/mostrar_snackbar.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../proveedores/compartir.dart';
import 'dominio/enlace_para_compartir.dart';
import 'textos_invitacion.dart';

/// Enviar la invitación por WhatsApp, SMS u otra app (2026-10-18). El canal elegido en
/// el formulario va primero; los otros siguen a mano. Si la app no abre, se avisa que
/// se puede copiar el enlace.
class BotonesDeCompartir extends ConsumerWidget {
  const BotonesDeCompartir({
    super.key,
    required this.enlace,
    required this.telefono,
    required this.canal,
  });

  final String enlace;
  final String telefono;
  final String canal;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final compartir = ref.read(compartirProvider);
    final texto = mensajeDeInvitacion(enlace);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (final (indice, opcion) in ordenDeCanales(canal).indexed) ...[
          if (indice > 0) const SizedBox(height: Espacio.s2),
          Boton(
            key: ValueKey('compartir-$opcion'),
            texto: switch (opcion) {
              'WHATSAPP' => TextosInvitacion.enviarPorWhatsApp,
              'SMS' => TextosInvitacion.enviarPorSms,
              _ => TextosInvitacion.compartirConOtraApp,
            },
            icono: switch (opcion) {
              'WHATSAPP' => Icons.chat_rounded,
              'SMS' => Icons.sms_rounded,
              _ => Icons.ios_share_rounded,
            },
            variante: indice == 0
                ? BotonVariante.primario
                : BotonVariante.secundario,
            expandido: true,
            onPressed: () async {
              final abierto = await switch (opcion) {
                'WHATSAPP' => compartir.porWhatsApp(
                  telefono: telefono,
                  texto: texto,
                ),
                'SMS' => compartir.porSms(telefono: telefono, texto: texto),
                _ => compartir.conOtraApp(texto: texto),
              };
              if (abierto || !context.mounted) return;
              mostrarSnackbar(context, TextosInvitacion.noSePudoAbrir);
            },
          ),
        ],
      ],
    );
  }
}

/// El canal elegido primero; después los otros dos.
List<String> ordenDeCanales(String elegido) {
  const todos = ['WHATSAPP', 'SMS', 'OTRA'];
  final primero = switch (elegido) {
    'WHATSAPP' => 'WHATSAPP',
    'SMS' => 'SMS',
    _ => 'OTRA',
  };
  return [primero, ...todos.where((c) => c != primero)];
}
