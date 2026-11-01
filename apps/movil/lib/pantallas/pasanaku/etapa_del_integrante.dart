import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:aportaya_diseno/atomos/insignia_de_etapa.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../reputacion/textos_de_etapas.dart';
import 'textos_de_refirma_y_etapa.dart';

/// El orden de cada etapa del catálogo, por su código. `IntegranteDelGrupo.etapa` trae
/// solo código y nombre (nunca el puntaje, regla 90.2.8); el número y el color salen
/// del catálogo publicado por `transparencia`, no de una copia en la app.
Map<String, int> ordenesDeEtapa(List<EtapaDelCatalogo>? catalogo) => {
  for (final e in catalogo ?? const <EtapaDelCatalogo>[]) e.codigo: e.orden,
};

/// La etapa de un integrante en su tarjeta: la insignia y «Etapa 2 · En marcha». Sin
/// catálogo (no llegó o falló) se ve solo el nombre, sin número ni insignia: es mejor
/// eso que un número inventado. El nombre accesible siempre dice que es la etapa.
class EtapaDelIntegranteEnTarjeta extends StatelessWidget {
  const EtapaDelIntegranteEnTarjeta({
    super.key,
    required this.etapa,
    this.orden,
  });

  final EtapaDelIntegrante etapa;
  final int? orden;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final o = orden;
    final texto = o == null
        ? etapa.nombre
        : TextosDeEtapas.rotulo(o, etapa.nombre);
    final leida = o == null
        ? etapa.nombre
        : TextosDeEtapas.rotuloAccesible(o, etapa.nombre);
    return Semantics(
      container: true,
      label: TextosDeRefirmaYEtapa.etiquetaDeEtapa(leida),
      excludeSemantics: true,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (o != null) ...[
            InsigniaDeEtapa(orden: o, estado: EstadoDeEtapa.alcanzada),
            const SizedBox(width: Espacio.s2),
          ],
          Flexible(
            child: Text(
              texto,
              style: Tipo.cuerpoChico.copyWith(color: t.text2),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }
}
