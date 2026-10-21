import 'package:flutter/material.dart';

import '../atomos/movimiento.dart';
import '../atomos/hundido_al_tocar.dart';
import '../tokens/tokens.dart';

part 'barra_pestanas_destino.dart';

/// Tab bar de 3–5 destinos, el activo en marca, con punto de novedades opcional.
class BarraPestanas extends StatelessWidget {
  const BarraPestanas({
    super.key,
    required this.destinos,
    required this.actual,
    required this.onChanged,
  });
  final List<
    ({String texto, IconData icono, IconData iconoActivo, int novedades})
  >
  destinos;
  final int actual;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    assert(
      destinos.length >= 3 && destinos.length <= 5,
      'entre 3 y 5 destinos',
    );
    return Material(
      color: t.surface,
      child: DecoratedBox(
        decoration: BoxDecoration(
          border: Border(
            top: BorderSide(color: t.border, width: Borde.fino),
          ),
        ),
        child: SafeArea(
          top: false,
          child: SizedBox(
            height: Tactil.minimo + Espacio.s5,
            child: Row(
              children: [
                for (var i = 0; i < destinos.length; i++)
                  Expanded(
                    child: _DestinoDePestanas(
                      destino: destinos[i],
                      seleccionado: i == actual,
                      tokens: t,
                      duracionIcono: duracion(
                        context,
                        Movimiento.tildeDeValidez,
                      ),
                      alTocar: () => onChanged(i),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
