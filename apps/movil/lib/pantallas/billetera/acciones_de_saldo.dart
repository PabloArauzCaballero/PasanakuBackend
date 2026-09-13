import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'textos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';

/// Las tres acciones del saldo, en una fila: **una sola naranja**.
///
/// Aportar es lo que la persona viene a hacer —es lo que mantiene vivo su pasanaku—,
/// así que se lleva el color de acción; recargar y retirar quedan en contorno. Cuatro
/// íconos iguales en una grilla no dicen cuál importa; esto sí.
class AccionesDeSaldo extends StatelessWidget {
  const AccionesDeSaldo({super.key, required this.cuentaId});

  final String cuentaId;

  @override
  Widget build(BuildContext context) {
    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Expanded(
            child: Boton(
              texto: TextosBilletera.aportar,
              variante: BotonVariante.primario,
              expandido: true,
              maxLineas: 1,
              onPressed: () => context.pushNamed('pasanaku.miEstado'),
            ),
          ),
          const SizedBox(width: Espacio.s2),
          Expanded(
            child: Boton(
              texto: TextosBilletera.recargar,
              variante: BotonVariante.fantasma,
              expandido: true,
              maxLineas: 1,
              onPressed: () => context.pushNamed(
                'billetera.recargar',
                queryParameters: {'cuenta': cuentaId},
              ),
            ),
          ),
          const SizedBox(width: Espacio.s2),
          Expanded(
            child: Boton(
              texto: TextosBilletera.retirar,
              variante: BotonVariante.fantasma,
              expandido: true,
              maxLineas: 1,
              onPressed: () => context.pushNamed(
                'billetera.retirar',
                queryParameters: {'cuenta': cuentaId},
              ),
            ),
          ),
        ],
      ),
    );
  }
}
