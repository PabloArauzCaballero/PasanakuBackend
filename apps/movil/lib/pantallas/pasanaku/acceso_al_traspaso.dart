import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'textos_del_traspaso.dart';

/// La puerta a «Traspasar mi cupo» (CU-64), en la pantalla del grupo. Solo aparece a
/// quien ocupa un cupo; si además puede o no traspasarlo lo decide el servidor al
/// confirmar, y la pantalla del traspaso lo explica.
class AccesoAlTraspaso extends StatelessWidget {
  const AccesoAlTraspaso({super.key, required this.detalle});
  final DetalleDelGrupo detalle;

  @override
  Widget build(BuildContext context) {
    final mio = detalle.integrantes
        .where(
          (i) =>
              i.soyYo &&
              i.cupoId != null &&
              i.estado != IntegranteDelGrupoEstadoEnum.LIBRE,
        )
        .firstOrNull;
    if (mio == null) return const SizedBox.shrink();
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: Espacio.s3),
      child: Tarjeta(
        key: const ValueKey('traspasar-mi-cupo'),
        etiqueta:
            '${TextosDelTraspaso.titulo}. ${TextosDelTraspaso.accesoDetalle}',
        onTap: () => context.push(
          '/pasanaku/grupos/${detalle.grupoId}/cupos/${mio.cupoId}/traspaso',
        ),
        child: Row(
          children: [
            Icon(IconosDeMarca.movimientos, color: t.brandTexto),
            const SizedBox(width: Espacio.s3),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    TextosDelTraspaso.titulo,
                    style: Tipo.cuerpoFuerte.copyWith(color: t.text),
                  ),
                  Text(
                    TextosDelTraspaso.accesoDetalle,
                    style: Tipo.cuerpoChico.copyWith(color: t.text2),
                  ),
                ],
              ),
            ),
            Icon(IconosDeMarca.seguir, color: t.text3),
          ],
        ),
      ),
    );
  }
}
