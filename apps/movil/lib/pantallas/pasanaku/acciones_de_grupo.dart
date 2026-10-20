import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'textos_invitacion.dart';

/// Las dos puertas de entrada al organizador desde la pestaña «Grupos».
///
/// Hallazgo de la prueba en emulador (carril H, 4.ª vuelta): `/pasanaku/grupos/nuevo`
/// y `/pasanaku/grupos/:id/invitar` estaban registradas en `rutas.dart` pero ninguna
/// pantalla ni enlace llevaba hasta ellas, así que una persona real no podía crear un
/// grupo ni invitar a nadie. Acá quedan a un toque: crear un grupo siempre, e invitar
/// solo cuando ya participa de uno (hace falta el `grupoId`).
class AccionesDeGrupo extends StatelessWidget {
  const AccionesDeGrupo({super.key, this.grupoId});

  /// El grupo de la sesión, si ya participa de alguno.
  final String? grupoId;

  @override
  Widget build(BuildContext context) {
    final id = grupoId;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (id != null && id.isNotEmpty) ...[
          Boton(
            texto: TextosInvitacion.accionInvitar,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: () => context.push('/pasanaku/grupos/$id/invitar'),
          ),
          const SizedBox(height: Espacio.s2),
        ],
        Boton(
          texto: TextosInvitacion.accionCrearGrupo,
          variante: BotonVariante.fantasma,
          expandido: true,
          onPressed: () => context.push('/pasanaku/grupos/nuevo'),
        ),
        // Con el QR o el enlace que mandó el organizador (2026-10-20). Solo en la lista
        // de grupos: dentro de un grupo no tiene sentido ofrecer unirse a otro.
        if (id == null || id.isEmpty) ...[
          const SizedBox(height: Espacio.s2),
          Boton(
            key: const ValueKey('unirse-a-un-grupo'),
            texto: TextosInvitacion.accionUnirseAGrupo,
            variante: BotonVariante.fantasma,
            expandido: true,
            onPressed: () => context.push('/pasanaku/unirse-a-un-grupo'),
          ),
        ],
      ],
    );
  }
}
