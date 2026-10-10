import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/fecha.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/moviles/mostrar_snackbar.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'textos.dart';

/// El resultado de CU-69: la invitación quedó creada y esta pantalla entrega el enlace.
///
/// El enlace lleva el secreto de la invitación: se muestra y se copia, **nunca** se
/// escribe en un log ni en la ruta de navegación (regla 90.2.9). Por eso no hay un
/// `print` ni un `debugPrint` en este archivo.
class InvitacionCreada extends StatefulWidget {
  const InvitacionCreada({
    super.key,
    required this.resultado,
    required this.telefono,
    required this.alInvitarOtra,
  });

  final SalidaInvitacion resultado;

  /// El celular al que se invitó: lo escribió quien invita, no viene del servidor.
  final String telefono;
  final VoidCallback alInvitarOtra;

  @override
  State<InvitacionCreada> createState() => _InvitacionCreadaState();
}

class _InvitacionCreadaState extends State<InvitacionCreada> {
  bool _copiado = false;

  Future<void> _copiar(String enlace) async {
    await Clipboard.setData(ClipboardData(text: enlace));
    if (!mounted) return;
    setState(() => _copiado = true);
    mostrarSnackbar(context, TextosPasanaku.enlaceCopiado);
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final enlace = widget.resultado.enlace;
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
                      TextosPasanaku.invitacionCreadaTitulo,
                      style: Tipo.titulo3.copyWith(color: t.text),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: Espacio.s3),
              Text(
                TextosPasanaku.invitacionCreadaPara(widget.telefono),
                style: Tipo.cuerpo.copyWith(color: t.text2),
              ),
              if (vence != null) ...[
                const SizedBox(height: Espacio.s1),
                Text(
                  TextosPasanaku.invitacionVence(
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
            TextosPasanaku.enlaceDeInvitacion,
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
              style: Tipo.cuerpoChico.copyWith(
                color: t.text,
                fontFamily: 'monospace',
              ),
            ),
          ),
          const SizedBox(height: Espacio.s4),
          Boton(
            texto: _copiado
                ? TextosPasanaku.copiado
                : TextosPasanaku.copiarEnlace,
            icono: _copiado ? Icons.check_rounded : Icons.copy_rounded,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: () => _copiar(enlace),
          ),
        ],
        const SizedBox(height: Espacio.s2),
        Boton(
          texto: TextosPasanaku.invitarAOtra,
          variante: BotonVariante.fantasma,
          expandido: true,
          onPressed: widget.alInvitarOtra,
        ),
      ],
    );
  }
}
