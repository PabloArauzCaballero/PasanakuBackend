import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'textos.dart';
import 'textos_de_conversion.dart';
import 'textos_invitacion.dart';

/// La confirmación de un grupo recién creado. Como el resto de las pantallas de éxito
/// (tarjeta con ícono, importe formateado), y deja a un toque lo que sigue: invitar.
///
/// Pasada de marketing (2026-10-29): la cifra que la persona vino a armar —lo que junta
/// cada turno— pasa de nota al pie a protagonista, y el paso que sigue se dice antes del
/// botón, para que invitar se lea como la continuación natural y no como un extra.
class GrupoCreado extends StatelessWidget {
  const GrupoCreado({
    super.key,
    required this.grupoId,
    required this.fondoPorPeriodo,
  });
  final String grupoId;
  final String fondoPorPeriodo;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
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
                    child: Semantics(
                      header: true,
                      child: Text(
                        TextosPasanaku.grupoCreadoTitulo,
                        style: Tipo.titulo3.copyWith(color: t.text),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: Espacio.s3),
              Text(
                TextosPasanaku.grupoCreado,
                style: Tipo.cuerpo.copyWith(color: t.text2),
              ),
              const SizedBox(height: Espacio.s4),
              Divider(height: Borde.fino, color: t.border),
              const SizedBox(height: Espacio.s4),
              Text(
                TextosDeConversion.cadaTurnoJunta,
                style: Tipo.cuerpoChico.copyWith(color: t.text2),
              ),
              const SizedBox(height: Espacio.s1),
              FittedBox(
                fit: BoxFit.scaleDown,
                alignment: AlignmentDirectional.centerStart,
                child: Monto(
                  monto: fondoPorPeriodo,
                  moneda: 'BOB',
                  etiqueta: TextosDeConversion.cadaTurnoJunta,
                  estilo: Tipo.cifra.copyWith(color: t.text),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: Espacio.s5),
        Text(
          TextosDeConversion.loQueSigue,
          style: Tipo.cuerpo.copyWith(color: t.text2),
        ),
        const SizedBox(height: Espacio.s3),
        Boton(
          texto: TextosInvitacion.accionInvitar,
          variante: BotonVariante.primario,
          expandido: true,
          onPressed: () => context.push('/pasanaku/grupos/$grupoId/invitar'),
        ),
      ],
    );
  }
}
