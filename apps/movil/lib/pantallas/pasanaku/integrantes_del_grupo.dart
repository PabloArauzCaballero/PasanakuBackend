import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../reputacion/dominio/camino_de_etapas.dart';
import 'etapa_del_integrante.dart';
import 'tarjeta_de_integrante.dart';

/// Una tarjeta por cupo, con la etapa de cada ocupante cuando el servidor la manda
/// (`IntegranteDelGrupo.etapa`). El catálogo se pide solo si alguna tarjeta tiene
/// etapa; si no llega, las tarjetas siguen y la etapa se ve sin número.
class IntegrantesDelGrupo extends ConsumerWidget {
  const IntegrantesDelGrupo({super.key, required this.detalle});
  final DetalleDelGrupo detalle;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final d = detalle;
    final conEtapa = d.integrantes.any((i) => i.etapa != null);
    final ordenes = conEtapa
        ? ordenesDeEtapa(ref.watch(catalogoDeEtapasProvider).value)
        : const <String, int>{};
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (final integrante in d.integrantes) ...[
          TarjetaDeIntegrante(
            integrante: integrante,
            grupoId: d.grupoId,
            nombreGrupo: d.nombre,
            ordenDeEtapa: ordenes[integrante.etapa?.codigo],
          ),
          const SizedBox(height: Espacio.s2),
        ],
      ],
    );
  }
}
