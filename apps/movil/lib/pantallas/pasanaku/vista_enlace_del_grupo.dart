import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/codigo_q_r.dart';
import 'package:aportaya_diseno/atomos/fecha.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos_enlace_del_grupo.dart';

/// Lo que se ve al invitar (2026-10-20): dos formas y nada más, **por QR** (la imagen
/// que se manda al grupo de WhatsApp) y **por enlace** (el texto que se pega en la
/// app). Sin enlace todavía, el aviso de que se está preparando o el botón para
/// reintentar. Solo pinta: las acciones las decide [PantallaEnlaceDelGrupo].
class VistaEnlaceDelGrupo extends StatelessWidget {
  const VistaEnlaceDelGrupo({
    super.key,
    required this.enlace,
    required this.expiraEn,
    required this.trabajando,
    required this.alCrear,
    required this.alCompartirQr,
    required this.alCompartirEnlace,
    required this.alCopiar,
    required this.alGenerarOtro,
    required this.alAnular,
    required this.alVerSolicitudes,
  });

  final String? enlace;
  final DateTime? expiraEn;
  final bool trabajando;
  final VoidCallback alCrear;
  final ValueChanged<String> alCompartirQr;
  final ValueChanged<String> alCompartirEnlace;
  final ValueChanged<String> alCopiar;
  final VoidCallback alGenerarOtro;
  final VoidCallback alAnular;
  final VoidCallback alVerSolicitudes;

  @override
  Widget build(BuildContext context) {
    final e = enlace;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (e == null)
          EstadoVacio(
            mensaje: trabajando
                ? TextosEnlaceDelGrupo.preparando
                : '${TextosEnlaceDelGrupo.sinEnlaceTitulo}. ${TextosEnlaceDelGrupo.sinEnlaceDetalle}',
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
      ],
    );
  }

  List<Widget> _conEnlace(BuildContext context, String e) {
    final t = Tokens.of(context);
    final vence = expiraEn;
    final detalle = Tipo.cuerpoChico.copyWith(color: t.text2);
    return [
      Tarjeta(
        etiqueta: TextosEnlaceDelGrupo.porQr,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              TextosEnlaceDelGrupo.porQr,
              style: Tipo.titulo3.copyWith(color: t.text),
            ),
            const SizedBox(height: Espacio.s1),
            Text(TextosEnlaceDelGrupo.porQrDetalle, style: detalle),
            const SizedBox(height: Espacio.s3),
            Center(
              child: CodigoQR(
                key: const ValueKey('qr-enlace-del-grupo'),
                contenido: e,
                etiqueta: TextosEnlaceDelGrupo.qrEtiqueta,
              ),
            ),
            const SizedBox(height: Espacio.s3),
            Boton(
              key: const ValueKey('compartir-qr'),
              texto: TextosEnlaceDelGrupo.compartirQr,
              variante: BotonVariante.primario,
              expandido: true,
              onPressed: trabajando ? null : () => alCompartirQr(e),
            ),
          ],
        ),
      ),
      const SizedBox(height: Espacio.s3),
      Tarjeta(
        etiqueta: TextosEnlaceDelGrupo.porEnlaceTitulo,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              TextosEnlaceDelGrupo.porEnlaceTitulo,
              style: Tipo.titulo3.copyWith(color: t.text),
            ),
            const SizedBox(height: Espacio.s1),
            Text(TextosEnlaceDelGrupo.porEnlaceDetalle, style: detalle),
            const SizedBox(height: Espacio.s3),
            SelectableText(
              e,
              key: const ValueKey('texto-enlace-del-grupo'),
              style: Tipo.cuerpoChico.copyWith(color: t.text2),
            ),
            const SizedBox(height: Espacio.s3),
            Boton(
              key: const ValueKey('compartir-enlace'),
              texto: TextosEnlaceDelGrupo.compartirEnlace,
              variante: BotonVariante.secundario,
              expandido: true,
              onPressed: trabajando ? null : () => alCompartirEnlace(e),
            ),
            const SizedBox(height: Espacio.s2),
            Boton(
              texto: TextosEnlaceDelGrupo.copiarEnlace,
              variante: BotonVariante.fantasma,
              expandido: true,
              onPressed: () => alCopiar(e),
            ),
          ],
        ),
      ),
      if (vence != null) ...[
        const SizedBox(height: Espacio.s2),
        Text(
          TextosEnlaceDelGrupo.vence(
            Fecha.formatear(vence.toUtc().toIso8601String(), conHora: false),
          ),
          textAlign: TextAlign.center,
          style: Tipo.cuerpoChico.copyWith(color: t.text3),
        ),
      ],
      const SizedBox(height: Espacio.s2),
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
