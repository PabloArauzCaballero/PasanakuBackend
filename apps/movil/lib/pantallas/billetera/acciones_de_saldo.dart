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
    // Tres acciones angostas no conservan sus verbos con texto grande. Refluye la
    // fila a una columna antes de que `Boton` tenga que ocultar parte de la etiqueta.
    final escala = MediaQuery.textScalerOf(context).scale(1);
    return LayoutBuilder(
      builder: (context, constraints) {
        // Con 360/412 dp y texto al 130 %, tres etiquetas con icono no caben
        // en una fila. Apilarlas conserva el verbo completo y el área táctil.
        if (escala >= 1.5 || constraints.maxWidth < 420) {
          return _accionesEnColumna(context);
        }
        return IntrinsicHeight(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Expanded(
                child: _botonAportar(context, expandido: true, maxLineas: 1),
              ),
              const SizedBox(width: Espacio.s2),
              Expanded(
                child: _botonRecargar(context, expandido: true, maxLineas: 1),
              ),
              const SizedBox(width: Espacio.s2),
              Expanded(
                child: _botonRetirar(context, expandido: true, maxLineas: 1),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _accionesEnColumna(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      _botonAportar(context, expandido: true, maxLineas: 1),
      const SizedBox(height: Espacio.s2),
      _botonRecargar(context, expandido: true, maxLineas: 1),
      const SizedBox(height: Espacio.s2),
      _botonRetirar(context, expandido: true, maxLineas: 1),
    ],
  );

  Widget _botonAportar(
    BuildContext context, {
    required bool expandido,
    required int maxLineas,
  }) => Boton(
    texto: TextosBilletera.aportar,
    icono: Icons.arrow_outward,
    variante: BotonVariante.primario,
    expandido: expandido,
    maxLineas: maxLineas,
    onPressed: () => context.pushNamed('pasanaku.miEstado'),
  );

  Widget _botonRecargar(
    BuildContext context, {
    required bool expandido,
    required int maxLineas,
  }) => Boton(
    texto: TextosBilletera.recargar,
    icono: Icons.add,
    variante: BotonVariante.fantasma,
    expandido: expandido,
    maxLineas: maxLineas,
    onPressed: () => context.pushNamed(
      'billetera.recargar',
      queryParameters: {'cuenta': cuentaId},
    ),
  );

  Widget _botonRetirar(
    BuildContext context, {
    required bool expandido,
    required int maxLineas,
  }) => Boton(
    texto: TextosBilletera.retirar,
    icono: Icons.account_balance_outlined,
    variante: BotonVariante.fantasma,
    expandido: expandido,
    maxLineas: maxLineas,
    onPressed: () => context.pushNamed(
      'billetera.retirar',
      queryParameters: {'cuenta': cuentaId},
    ),
  );
}
