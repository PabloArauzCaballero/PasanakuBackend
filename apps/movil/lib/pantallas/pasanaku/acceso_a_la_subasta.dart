import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'textos_de_la_subasta.dart';

/// La puerta a «Subastar mi cupo», en la pantalla del grupo (reemplaza al traspaso con
/// votación, 2026-10-30). Solo aparece a quien ocupa un cupo; si se puede subastar lo
/// decide el servidor al abrir la subasta, y la pantalla lo explica.
class AccesoALaSubasta extends StatelessWidget {
  const AccesoALaSubasta({super.key, required this.detalle});
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
        key: const ValueKey('subastar-mi-cupo'),
        etiqueta:
            '${TextosDeLaSubasta.subastarMiCupo}. ${TextosDeLaSubasta.accesoDetalle}',
        onTap: () => context.push(
          '/pasanaku/grupos/${detalle.grupoId}/cupos/${mio.cupoId}/subasta',
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
                    TextosDeLaSubasta.subastarMiCupo,
                    style: Tipo.cuerpoFuerte.copyWith(color: t.text),
                  ),
                  Text(
                    TextosDeLaSubasta.accesoDetalle,
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
