import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'textos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';

/// Las tres acciones del saldo: **una sola naranja**. En ancho estrecho o texto grande se apilan
/// para no cortar las etiquetas ni reducir los objetivos táctiles.
///
/// Aportar es lo que la persona viene a hacer —es lo que mantiene vivo su pasanaku—,
/// así que se lleva el color de acción; recargar y retirar quedan en contorno. Cuatro
/// íconos iguales en una grilla no dicen cuál importa; esto sí.
class AccionesDeSaldo extends StatelessWidget {
  const AccionesDeSaldo({super.key, this.operacionesPausadas = false});

  final bool operacionesPausadas;

  @override
  Widget build(BuildContext context) {
    final escalaDeTexto = MediaQuery.textScalerOf(context).scale(16) / 16;
    return LayoutBuilder(
      builder: (context, limites) {
        // La fila necesita ~520 dp internos para que los tres verbos y sus iconos
        // no se corten; el umbral crece con el tamaño de letra del sistema.
        final apilar = limites.maxWidth < 520 * escalaDeTexto;
        final botones = [
          Boton(
            texto: TextosBilletera.aportar,
            icono: Icons.arrow_outward,
            variante: BotonVariante.primario,
            expandido: true,
            maxLineas: apilar ? 2 : 1,
            onPressed: operacionesPausadas
                ? null
                : () => context.pushNamed('pasanaku.miEstado'),
          ),
          Boton(
            texto: TextosBilletera.recargar,
            icono: Icons.add,
            variante: BotonVariante.fantasma,
            expandido: true,
            maxLineas: apilar ? 2 : 1,
            onPressed: operacionesPausadas
                ? null
                : () => context.pushNamed('billetera.recargar'),
          ),
          Boton(
            texto: TextosBilletera.retirar,
            icono: Icons.account_balance_outlined,
            variante: BotonVariante.fantasma,
            expandido: true,
            maxLineas: apilar ? 2 : 1,
            onPressed: operacionesPausadas
                ? null
                : () => context.pushNamed('billetera.retirar'),
          ),
        ];
        if (apilar) {
          return Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              botones[0],
              const SizedBox(height: Espacio.s2),
              botones[1],
              const SizedBox(height: Espacio.s2),
              botones[2],
            ],
          );
        }
        return IntrinsicHeight(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Expanded(child: botones[0]),
              const SizedBox(width: Espacio.s2),
              Expanded(child: botones[1]),
              const SizedBox(width: Espacio.s2),
              Expanded(child: botones[2]),
            ],
          ),
        );
      },
    );
  }
}
