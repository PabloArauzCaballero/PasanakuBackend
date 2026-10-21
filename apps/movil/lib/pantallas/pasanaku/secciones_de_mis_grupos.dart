import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'dominio/mis_grupos.dart';
import 'pantalla_mis_grupos.dart';
import 'textos.dart';

/// La pestaña «Grupos» en dos partes (2026-10-20): los que organizo y en los que
/// participo, cada una con su contador contra el tope («Organizás · 1 de 2»). Sin
/// respuesta de los topes, el contador va sin el «de N».
class SeccionesDeMisGrupos extends StatelessWidget {
  const SeccionesDeMisGrupos({super.key, required this.grupos, this.limites});
  final List<GrupoMio> grupos;
  final MisLimites? limites;

  @override
  Widget build(BuildContext context) {
    final organizo = [
      for (final g in grupos)
        if (g.esOrganizador) g,
    ];
    final participo = [
      for (final g in grupos)
        if (!g.esOrganizador) g,
    ];
    final l = limites;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _Seccion(
          titulo: TextosPasanaku.seccionOrganizas(
            l?.organizados ?? organizo.length,
            l?.limiteDeOrganizar,
          ),
          vacio: TextosPasanaku.sinGruposOrganizados,
          grupos: organizo,
        ),
        const SizedBox(height: Espacio.s4),
        _Seccion(
          titulo: TextosPasanaku.seccionParticipas(
            l?.participaciones ?? participo.length,
            l?.limiteDeParticipar,
          ),
          vacio: TextosPasanaku.sinGruposParticipados,
          grupos: participo,
        ),
      ],
    );
  }
}

class _Seccion extends StatelessWidget {
  const _Seccion({
    required this.titulo,
    required this.vacio,
    required this.grupos,
  });
  final String titulo;
  final String vacio;
  final List<GrupoMio> grupos;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Semantics(
          header: true,
          child: Text(titulo, style: Tipo.titulo3.copyWith(color: t.text)),
        ),
        const SizedBox(height: Espacio.s2),
        if (grupos.isEmpty)
          Text(vacio, style: Tipo.cuerpoChico.copyWith(color: t.text3))
        else
          for (final g in grupos) ...[
            TarjetaDeMiGrupo(grupo: g),
            const SizedBox(height: Espacio.s3),
          ],
      ],
    );
  }
}
