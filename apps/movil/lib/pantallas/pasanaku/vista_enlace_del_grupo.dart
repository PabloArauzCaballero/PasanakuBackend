import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/codigo_q_r.dart';
import 'package:aportaya_diseno/atomos/fecha.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos_enlace_del_grupo.dart';

/// Lo que se ve del enlace del grupo: sin enlace, la invitación a crearlo; con enlace,
/// el QR grande, el enlace y las formas de compartirlo. Solo pinta: las acciones las
/// decide [PantallaEnlaceDelGrupo].
class VistaEnlaceDelGrupo extends StatelessWidget {
  const VistaEnlaceDelGrupo({
    super.key,
    required this.enlace,
    required this.expiraEn,
    required this.trabajando,
    required this.alCrear,
    required this.alCompartirPorWhatsApp,
    required this.alCompartirConOtraApp,
    required this.alCopiar,
    required this.alGenerarOtro,
    required this.alAnular,
    required this.alVerSolicitudes,
    required this.alInvitarAUnNumero,
  });

  final String? enlace;
  final DateTime? expiraEn;
  final bool trabajando;
  final VoidCallback alCrear;
  final ValueChanged<String> alCompartirPorWhatsApp;
  final ValueChanged<String> alCompartirConOtraApp;
  final ValueChanged<String> alCopiar;
  final VoidCallback alGenerarOtro;
  final VoidCallback alAnular;
  final VoidCallback alVerSolicitudes;
  final VoidCallback alInvitarAUnNumero;

  @override
  Widget build(BuildContext context) {
    final e = enlace;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (e == null)
          EstadoVacio(
            mensaje:
                '${TextosEnlaceDelGrupo.sinEnlaceTitulo}. ${TextosEnlaceDelGrupo.sinEnlaceDetalle}',
            accion: Boton(
              key: const ValueKey('crear-enlace-del-grupo'),
              texto: TextosEnlaceDelGrupo.crearEnlace,
              variante: BotonVariante.primario,
              cargando: trabajando,
              onPressed: alCrear,
            ),
          )
        else
          ..._conEnlace(context, e),
        const SizedBox(height: Espacio.s4),
        Boton(
          texto: TextosEnlaceDelGrupo.verSolicitudes,
          variante: BotonVariante.secundario,
          expandido: true,
          onPressed: alVerSolicitudes,
        ),
        const SizedBox(height: Espacio.s2),
        Boton(
          texto: TextosEnlaceDelGrupo.invitarAUnNumero,
          variante: BotonVariante.enlace,
          expandido: true,
          onPressed: alInvitarAUnNumero,
        ),
      ],
    );
  }

  List<Widget> _conEnlace(BuildContext context, String e) {
    final t = Tokens.of(context);
    final vence = expiraEn;
    return [
      Center(
        child: CodigoQR(
          key: const ValueKey('qr-enlace-del-grupo'),
          contenido: e,
          etiqueta: TextosEnlaceDelGrupo.qrEtiqueta,
        ),
      ),
      const SizedBox(height: Espacio.s2),
      Text(
        TextosEnlaceDelGrupo.escaneaElQr,
        textAlign: TextAlign.center,
        style: Tipo.cuerpoChico.copyWith(color: t.text2),
      ),
      if (vence != null) ...[
        const SizedBox(height: Espacio.s1),
        Text(
          TextosEnlaceDelGrupo.vence(
            Fecha.formatear(vence.toUtc().toIso8601String(), conHora: false),
          ),
          textAlign: TextAlign.center,
          style: Tipo.cuerpoChico.copyWith(color: t.text3),
        ),
      ],
      const SizedBox(height: Espacio.s3),
      SelectableText(
        e,
        key: const ValueKey('texto-enlace-del-grupo'),
        textAlign: TextAlign.center,
        style: Tipo.cuerpoChico.copyWith(color: t.text2),
      ),
      const SizedBox(height: Espacio.s4),
      Boton(
        texto: TextosEnlaceDelGrupo.compartirPorWhatsApp,
        variante: BotonVariante.primario,
        expandido: true,
        onPressed: trabajando ? null : () => alCompartirPorWhatsApp(e),
      ),
      const SizedBox(height: Espacio.s2),
      Boton(
        texto: TextosEnlaceDelGrupo.compartirConOtraApp,
        variante: BotonVariante.secundario,
        expandido: true,
        onPressed: trabajando ? null : () => alCompartirConOtraApp(e),
      ),
      const SizedBox(height: Espacio.s2),
      Boton(
        texto: TextosEnlaceDelGrupo.copiarEnlace,
        variante: BotonVariante.fantasma,
        expandido: true,
        onPressed: () => alCopiar(e),
      ),
      const SizedBox(height: Espacio.s4),
      Row(
        children: [
          Expanded(
            child: Boton(
              texto: TextosEnlaceDelGrupo.generarOtro,
              variante: BotonVariante.enlace,
              cargando: trabajando,
              onPressed: alGenerarOtro,
            ),
          ),
          Expanded(
            child: Boton(
              texto: TextosEnlaceDelGrupo.anular,
              variante: BotonVariante.enlace,
              onPressed: trabajando ? null : alAnular,
            ),
          ),
        ],
      ),
    ];
  }
}
