import 'package:flutter/material.dart';
import 'package:flutter/physics.dart';

import '../atomos/hundido_al_tocar.dart';
import '../atomos/movimiento.dart';
import '../tokens/tokens.dart';

part 'barra_pestanas_destino.dart';
part 'barra_pestanas_pildora.dart';

/// Lo que hace el botón del centro de la barra: no es una pestaña, es la acción más rápida
/// de la app (en AportaYa, escanear la invitación a un grupo: ADR-053, A6).
typedef AccionCentral = ({
  IconData icono,
  String etiqueta,
  VoidCallback alTocar,
});

/// La barra de pestañas como cápsula flotante (ADR-053): se separa de los bordes, lleva la
/// pestaña activa en una píldora de marca y, si hay [central], un círculo verde levantado
/// en el medio, como el «pagar» de las apps bancarias.
///
/// Flota dentro de su propia franja, sobre el fondo de la página, y no encima del contenido:
/// así ninguna pantalla con relleno fijo queda tapada por la barra.
class BarraPestanas extends StatefulWidget {
  const BarraPestanas({
    super.key,
    required this.destinos,
    required this.actual,
    required this.onChanged,
    this.central,
  });
  final List<
    ({String texto, IconData icono, IconData iconoActivo, int novedades})
  >
  destinos;
  final int actual;
  final ValueChanged<int> onChanged;
  final AccionCentral? central;

  @override
  State<BarraPestanas> createState() => _BarraPestanasState();
}

class _BarraPestanasState extends State<BarraPestanas>
    with SingleTickerProviderStateMixin {
  /// La posición de la píldora, en **casilleros** (0 = el primero): una sola píldora que se
  /// desliza, no una por pestaña que se enciende y apaga (H5.S2.M4, `apple-design`).
  late final AnimationController _posicion = AnimationController.unbounded(
    vsync: this,
    value: _casilleroDe(widget.actual).toDouble(),
  );

  /// El botón del medio ocupa un casillero propio: las pestañas de la derecha corren uno.
  int _casilleroDe(int i) {
    if (widget.central == null) return i;
    return i < (widget.destinos.length / 2).ceil() ? i : i + 1;
  }

  @override
  void didUpdateWidget(BarraPestanas anterior) {
    super.didUpdateWidget(anterior);
    if (anterior.actual == widget.actual) return;
    final meta = _casilleroDe(widget.actual).toDouble();
    if (reducirMovimiento(context)) {
      _posicion.value = meta;
      return;
    }
    // Resorte con amortiguación crítica: llega rápido y se asienta sin rebotar; conserva la
    // velocidad si se toca otra pestaña a mitad de camino (se puede interrumpir).
    _posicion.animateWith(
      SpringSimulation(
        Movimiento.resorteCritico,
        _posicion.value,
        meta,
        _posicion.velocity,
      ),
    );
  }

  @override
  void dispose() {
    _posicion.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final destinos = widget.destinos;
    final central = widget.central;
    assert(
      destinos.length >= 3 && destinos.length <= 5,
      'entre 3 y 5 destinos',
    );
    final oscuro = Theme.of(context).brightness == Brightness.dark;
    final duracionIcono = duracion(context, Movimiento.tildeDeValidez);
    // Con texto grande la etiqueta crece y la barra con ella: la píldora va anclada arriba.
    final extra =
        (MediaQuery.textScalerOf(context).scale(Tipo.pestana.fontSize!) -
            Tipo.pestana.fontSize!) *
        (Tipo.pestana.height ?? 1);
    Widget destino(int i) => Expanded(
      child: _DestinoDePestanas(
        destino: destinos[i],
        seleccionado: i == widget.actual,
        tokens: t,
        duracionIcono: duracionIcono,
        alTocar: () => widget.onChanged(i),
      ),
    );
    final mitad = (destinos.length / 2).ceil();
    final casilleros = destinos.length + (central == null ? 0 : 1);
    return ColoredBox(
      color: t.bg,
      child: SafeArea(
        top: false,
        minimum: const EdgeInsets.only(bottom: Espacio.s2),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(
            Espacio.s3,
            Espacio.s1,
            Espacio.s3,
            0,
          ),
          // La sombra va por fuera y la superficie es un `Material`: los toques de cada
          // destino necesitan uno, y sin recortar, para que el círculo del medio sobresalga.
          child: DecoratedBox(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(Radios.xxl),
              boxShadow: Elevacion.superpuesta(t),
            ),
            child: Material(
              color: t.surface,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(Radios.xxl),
                side: oscuro
                    ? BorderSide(color: t.border, width: Borde.fino)
                    : BorderSide.none,
              ),
              child: SizedBox(
                height: Tactil.minimo + Espacio.s5 + extra,
                child: Stack(
                  children: [
                    // La píldora, detrás de los íconos.
                    _Pildora(
                      posicion: _posicion,
                      casilleros: casilleros,
                      color: t.brandBg,
                    ),
                    Row(
                      children: [
                        if (central == null)
                          for (var i = 0; i < destinos.length; i++) destino(i)
                        else ...[
                          for (var i = 0; i < mitad; i++) destino(i),
                          Expanded(
                            child: _BotonCentral(accion: central, tokens: t),
                          ),
                          for (var i = mitad; i < destinos.length; i++)
                            destino(i),
                        ],
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
