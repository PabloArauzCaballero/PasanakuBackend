import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'dominio/mis_limites.dart';
import 'textos_invitacion.dart';

/// Las dos puertas de entrada al organizador desde la pestaña «Grupos».
///
/// Hallazgo de la prueba en emulador (carril H, 4.ª vuelta): `/pasanaku/grupos/nuevo`
/// y `/pasanaku/grupos/:id/invitar` estaban registradas en `rutas.dart` pero ninguna
/// pantalla ni enlace llevaba hasta ellas, así que una persona real no podía crear un
/// grupo ni invitar a nadie. Acá quedan a un toque: crear un grupo siempre, e invitar
/// solo cuando ya participa de uno (hace falta el `grupoId`).
class AccionesDeGrupo extends StatelessWidget {
  const AccionesDeGrupo({super.key, this.grupoId, this.limites});

  /// El grupo de la sesión, si ya participa de alguno.
  final String? grupoId;

  /// Mis topes (2026-10-20): al tope, el botón se apaga y se dice por qué, en vez de
  /// dejar llenar un formulario que el servidor va a rechazar.
  final MisLimites? limites;

  @override
  Widget build(BuildContext context) {
    final id = grupoId;
    // Dentro de un grupo solo se invita a ESE grupo; organizar otro o unirse a otro se
    // ofrece en la lista de grupos (pedido del dueño, 2026-10-20).
    if (id != null && id.isNotEmpty) {
      return Boton(
        texto: TextosInvitacion.accionInvitar,
        variante: BotonVariante.primario,
        expandido: true,
        onPressed: () => context.push('/pasanaku/grupos/$id/invitar'),
      );
    }
    final l = limites;
    final t = Tokens.of(context);
    final aviso = Tipo.cuerpoChico.copyWith(color: t.text2);
    final organizarAlTope = l != null && l.organizarAlTope;
    final participarAlTope = l != null && l.participarAlTope;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Boton(
          key: const ValueKey('organizar-un-grupo'),
          texto: TextosInvitacion.accionCrearGrupo,
          // Las dos puertas de entrada al pasanaku se ven como botones, no como enlaces
          // sueltos al pie (pasada de marketing, 2026-10-29: skill `cro`).
          variante: BotonVariante.secundario,
          expandido: true,
          onPressed: organizarAlTope
              ? null
              : () => context.push('/pasanaku/grupos/nuevo'),
        ),
        if (organizarAlTope)
          Text(
            TextosInvitacion.organizarAlTope(l.limiteDeOrganizar!),
            style: aviso,
          ),
        const SizedBox(height: Espacio.s2),
        // Con el QR o el enlace que mandó el organizador (2026-10-20).
        Boton(
          key: const ValueKey('unirse-a-un-grupo'),
          texto: TextosInvitacion.accionUnirseAGrupo,
          variante: BotonVariante.secundario,
          expandido: true,
          onPressed: participarAlTope
              ? null
              : () => context.push('/pasanaku/unirse-a-un-grupo'),
        ),
        if (participarAlTope)
          Text(
            TextosInvitacion.participarAlTope(l.limiteDeParticipar),
            style: aviso,
          ),
      ],
    );
  }
}
