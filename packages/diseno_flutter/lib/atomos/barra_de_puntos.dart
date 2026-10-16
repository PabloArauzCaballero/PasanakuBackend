import 'dart:async';

import 'package:flutter/material.dart';

import 'hundido_al_tocar.dart';
import '../moviles/alcance_haptica.dart';
import '../tokens/tokens.dart';
import 'movimiento.dart';

/// Progreso por láminas; puede ser indicador pasivo o navegación táctil.
class BarraDePuntos extends StatelessWidget {
  const BarraDePuntos({
    super.key,
    required this.total,
    required this.actual,
    this.onElegir,
    this.completados = const {},
  });
  final int total;
  final int actual;
  final ValueChanged<int>? onElegir;
  final Set<int> completados;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final fila = Row(
      mainAxisSize: MainAxisSize.min,
      children: [for (var i = 0; i < total; i += 1) _punto(context, t, i)],
    );
    if (onElegir != null) return fila;
    return Semantics(
      label: 'Pantalla ${actual + 1} de $total',
      excludeSemantics: true,
      child: fila,
    );
  }

  Widget _punto(BuildContext context, Tokens t, int indice) {
    final estaActual = indice == actual;
    final estaCompleto = completados.contains(indice);
    final punto = AnimatedContainer(
      duration: duracion(context, Movimiento.cambioDeEstado),
      width: estaActual ? Espacio.s5 : Espacio.s2,
      height: Espacio.s2,
      decoration: BoxDecoration(
        color: estaCompleto ? t.ok : (estaActual ? t.brand : t.border),
        borderRadius: BorderRadius.circular(Radios.pill),
      ),
    );
    final accion = onElegir;
    if (accion == null) {
      return Padding(
        padding: const EdgeInsets.symmetric(horizontal: Espacio.s1),
        child: punto,
      );
    }
    return Semantics(
      button: true,
      selected: estaActual,
      label:
          'Lámina ${indice + 1} de $total${estaCompleto ? ', completa' : ''}',
      excludeSemantics: true,
      child: HundidoAlTocar(
        child: InkResponse(
          onTap: () {
            unawaited(AlcanceHaptica.maybeOf(context)?.seleccion());
            accion(indice);
          },
          radius: Tactil.minimo / 2,
          child: SizedBox(
            width: Tactil.minimo,
            height: Tactil.minimo,
            child: Center(child: punto),
          ),
        ),
      ),
    );
  }
}
