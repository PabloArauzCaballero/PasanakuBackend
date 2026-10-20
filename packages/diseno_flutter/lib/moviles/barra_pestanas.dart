import 'package:flutter/material.dart';

import '../atomos/movimiento.dart';
import '../atomos/hundido_al_tocar.dart';
import '../tokens/tokens.dart';

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
          border: Border(top: BorderSide(color: t.border, width: Borde.fino)),
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
                      duracionIcono: duracion(context, Movimiento.tildeDeValidez),
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

class _DestinoDePestanas extends StatelessWidget {
  const _DestinoDePestanas({
    required this.destino,
    required this.seleccionado,
    required this.tokens,
    required this.duracionIcono,
    required this.alTocar,
  });

  final ({String texto, IconData icono, IconData iconoActivo, int novedades})
  destino;
  final bool seleccionado;
  final Tokens tokens;
  final Duration duracionIcono;
  final VoidCallback alTocar;

  @override
  Widget build(BuildContext context) {
    final etiqueta = destino.novedades > 0
        ? '${destino.texto}, ${destino.novedades} sin leer'
        : destino.texto;
    return Semantics(
      container: true,
      button: true,
      selected: seleccionado,
      label: etiqueta,
      onTap: alTocar,
      child: ExcludeSemantics(
        child: Tooltip(
          message: etiqueta,
          child: HundidoAlTocar(
            key: ValueKey('destino-${destino.texto}'),
            child: SizedBox.expand(
              child: InkWell(
                onTap: alTocar,
                excludeFromSemantics: true,
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    AnimatedContainer(
                      duration: duracion(context, Movimiento.cambioDeEstado),
                      curve: Movimiento.llega,
                      decoration: BoxDecoration(
                        color: seleccionado
                            ? tokens.brandBg
                            : Colors.transparent,
                        borderRadius: BorderRadius.circular(Radios.pill),
                      ),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: Espacio.s4,
                          vertical: Espacio.s1,
                        ),
                        child: Badge(
                          isLabelVisible: destino.novedades > 0,
                          label: Text('${destino.novedades}'),
                          backgroundColor: tokens.err,
                          child: AnimatedSwitcher(
                            duration: duracionIcono,
                            transitionBuilder: (child, animacion) =>
                                FadeTransition(
                                  opacity: animacion,
                                  child: ScaleTransition(
                                    scale: TweenSequence<double>([
                                      TweenSequenceItem(
                                        tween:
                                            Tween<double>(
                                              begin: 1,
                                              end: 1.12,
                                            ).chain(
                                              CurveTween(curve: Curves.easeOut),
                                            ),
                                        weight: 45,
                                      ),
                                      TweenSequenceItem(
                                        tween:
                                            Tween<double>(
                                              begin: 1.12,
                                              end: 1,
                                            ).chain(
                                              CurveTween(
                                                curve: Curves.easeInOut,
                                              ),
                                            ),
                                        weight: 55,
                                      ),
                                    ]).animate(animacion),
                                    child: child,
                                  ),
                                ),
                            child: Icon(
                              seleccionado
                                  ? destino.iconoActivo
                                  : destino.icono,
                              key: ValueKey(
                                seleccionado
                                    ? 'activo-${destino.iconoActivo.codePoint}'
                                    : 'inactivo-${destino.icono.codePoint}',
                              ),
                              color: seleccionado
                                  ? tokens.brandTexto
                                  : tokens.text2,
                            ),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: Espacio.s1),
                    AnimatedDefaultTextStyle(
                      duration: duracion(context, Movimiento.cambioDeEstado),
                      curve: Movimiento.llega,
                      style: (Theme.of(context).textTheme.labelSmall ??
                              const TextStyle())
                          .copyWith(
                            color: seleccionado
                                ? tokens.brandTexto
                                : tokens.text2,
                            fontWeight: seleccionado
                                ? FontWeight.w600
                                : FontWeight.w500,
                            letterSpacing: 0.1,
                          ),
                      child: Text(
                        destino.texto,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
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
