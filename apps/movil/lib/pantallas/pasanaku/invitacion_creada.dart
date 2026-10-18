import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/fecha.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/moviles/mostrar_snackbar.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/cliente.dart' show baseDelGateway;
import 'botones_de_compartir.dart';
import 'dominio/enlace_para_compartir.dart';

import 'textos_invitacion.dart';

/// El resultado de CU-69: la invitación quedó creada y esta pantalla entrega el enlace.
///
/// El enlace lleva el secreto de la invitación: se muestra y se copia, **nunca** se
/// escribe en un log ni en la ruta de navegación (regla 90.2.9). Por eso no hay un
/// `print` ni un `debugPrint` en este archivo.
///
/// **Compartir sin dependencias nuevas (2026-10-18).** AportaYa no tiene SMS ni WhatsApp
/// conectados del lado del servidor: el enlace sale del teléfono de quien invita. Los
/// botones abren WhatsApp o el SMS con el número y el mensaje ya escritos, o la hoja de
/// compartir del sistema, por intents nativos (`CompartirMensaje.kt`), sin `share_plus`.
/// El enlace que se comparte es https (la app web), porque `aportaya://` no es tocable
/// en un chat. Si la app elegida no abre, se ofrece copiarlo.
class InvitacionCreada extends ConsumerStatefulWidget {
  const InvitacionCreada({
    super.key,
    required this.resultado,
    required this.telefono,
    required this.alInvitarOtra,
    this.canal = 'ENLACE',
    this.appWeb,
  });

  final SalidaInvitacion resultado;

  /// El celular al que se invitó: lo escribió quien invita, no viene del servidor.
  final String telefono;
  final VoidCallback alInvitarOtra;

  /// El canal elegido en el formulario (`ENLACE`, `SMS`, `WHATSAPP`): su botón va primero.
  final String canal;

  /// La dirección de la app web; sin ella se deriva de la API (`enlace_para_compartir`).
  final String? appWeb;

  @override
  ConsumerState<InvitacionCreada> createState() => _InvitacionCreadaState();
}

class _InvitacionCreadaState extends ConsumerState<InvitacionCreada> {
  bool _copiado = false;

  Future<void> _copiar(String enlace) async {
    await Clipboard.setData(ClipboardData(text: enlace));
    if (!mounted) return;
    setState(() => _copiado = true);
    mostrarSnackbar(context, TextosInvitacion.enlaceCopiado);
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final delServidor = widget.resultado.enlace;
    final enlace = delServidor == null
        ? null
        : enlaceParaCompartir(
            delServidor,
            baseWeb: widget.appWeb ?? baseDeLaAppWeb(api: baseDelGateway),
          );
    final vence = widget.resultado.expiraEn;
    return ListView(
      padding: const EdgeInsets.all(Espacio.s4),
      children: [
        Tarjeta(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(Espacio.s2),
                    decoration: BoxDecoration(
                      color: t.okBg,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(Icons.check_rounded, color: t.okTexto),
                  ),
                  const SizedBox(width: Espacio.s3),
                  Expanded(
                    child: Text(
                      TextosInvitacion.invitacionCreadaTitulo,
                      style: Tipo.titulo3.copyWith(color: t.text),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: Espacio.s3),
              Text(
                TextosInvitacion.invitacionCreadaPara(widget.telefono),
                style: Tipo.cuerpo.copyWith(color: t.text2),
              ),
              if (vence != null) ...[
                const SizedBox(height: Espacio.s1),
                Text(
                  TextosInvitacion.invitacionVence(
                    Fecha.formatear(vence.toUtc().toIso8601String()),
                  ),
                  style: Tipo.cuerpoChico.copyWith(color: t.text3),
                ),
              ],
            ],
          ),
        ),
        if (enlace != null) ...[
          const SizedBox(height: Espacio.s4),
          Text(
            TextosInvitacion.enlaceDeInvitacion,
            style: Tipo.etiqueta.copyWith(color: t.text2),
          ),
          const SizedBox(height: Espacio.s2),
          Container(
            padding: const EdgeInsets.all(Espacio.s3),
            decoration: BoxDecoration(
              color: t.surface2,
              borderRadius: BorderRadius.circular(Radios.md),
              border: Border.all(color: t.border, width: Borde.fino),
            ),
            child: SelectableText(
              enlace,
              maxLines: 3,
              style: Tipo.cuerpoChico.copyWith(color: t.text),
            ),
          ),
          const SizedBox(height: Espacio.s4),
          BotonesDeCompartir(
            enlace: enlace,
            telefono: widget.telefono,
            canal: widget.canal,
          ),
          const SizedBox(height: Espacio.s2),
          Boton(
            texto: _copiado
                ? TextosInvitacion.copiado
                : TextosInvitacion.copiarEnlace,
            icono: _copiado ? Icons.check_rounded : Icons.copy_rounded,
            variante: BotonVariante.fantasma,
            expandido: true,
            onPressed: () => _copiar(enlace),
          ),
        ],
        const SizedBox(height: Espacio.s2),
        Boton(
          texto: TextosInvitacion.invitarAOtra,
          variante: BotonVariante.fantasma,
          expandido: true,
          onPressed: widget.alInvitarOtra,
        ),
      ],
    );
  }
}
