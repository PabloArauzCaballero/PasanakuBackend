part of 'barra_pestanas.dart';

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
    final color = seleccionado ? tokens.brandTexto : tokens.text2;
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
                customBorder: const StadiumBorder(),
                child: Column(
                  children: [
                    // El casillero del ícono: de alto fijo, para que la píldora que se
                    // desliza detrás (en la barra) quede siempre justo detrás.
                    const SizedBox(height: Espacio.s2),
                    SizedBox(
                      height: _altoDePildora,
                      child: Center(
                        child: Badge(
                          isLabelVisible: destino.novedades > 0,
                          label: Text('${destino.novedades}'),
                          backgroundColor: tokens.err,
                          // El ícono que se enciende entra con fundido y un rebote corto
                          // (Movimiento.tildeDeValidez); con movimiento reducido, sin rebote.
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
                              size: Espacio.s5 + Espacio.s1 / 2,
                              color: color,
                            ),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: Espacio.s1),
                    // «Movimientos» entra entero aunque la columna sea angosta: se achica
                    // antes de cortarse en «Movimien…».
                    FittedBox(
                      fit: BoxFit.scaleDown,
                      child: Text(
                        destino.texto,
                        maxLines: 1,
                        // Sin el tracking de `etiqueta` (pensado para mayúsculas): con él
                        // «Movimientos» se achicaba y las demás no, y quedaban de tamaños
                        // distintos en la misma barra.
                        style: Tipo.pestana.copyWith(
                          color: color,
                          fontWeight: seleccionado
                              ? FontWeight.w600
                              : FontWeight.w500,
                        ),
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

/// El círculo verde del medio, levantado sobre la cápsula: se lee como «lo que más hacés».
class _BotonCentral extends StatelessWidget {
  const _BotonCentral({required this.accion, required this.tokens});

  final AccionCentral accion;
  final Tokens tokens;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      container: true,
      button: true,
      label: accion.etiqueta,
      onTap: accion.alTocar,
      child: ExcludeSemantics(
        child: Tooltip(
          message: accion.etiqueta,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Transform.translate(
                offset: const Offset(0, -Espacio.s3),
                child: HundidoAlTocar(
                  key: const ValueKey('accion-central'),
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      color: tokens.verdeSolido,
                      shape: BoxShape.circle,
                      border: Border.all(color: tokens.bg, width: Espacio.s1),
                      boxShadow: Elevacion.destacada(tokens),
                    ),
                    child: Material(
                      type: MaterialType.transparency,
                      shape: const CircleBorder(),
                      clipBehavior: Clip.antiAlias,
                      child: InkWell(
                        onTap: accion.alTocar,
                        excludeFromSemantics: true,
                        child: SizedBox.square(
                          dimension: Tactil.minimo + Espacio.s2,
                          child: Icon(
                            accion.icono,
                            size: Espacio.s6,
                            color: tokens.sobreVerdeSolido,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
