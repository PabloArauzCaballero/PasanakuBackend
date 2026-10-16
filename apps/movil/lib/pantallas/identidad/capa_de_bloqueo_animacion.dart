part of 'capa_de_bloqueo.dart';

extension _AnimacionDeBloqueo on _CapaDeBloqueoState {
  void _sincronizarAnimacion(bool bloqueada) {
    final quieto = reducirMovimiento(context);
    if (bloqueada) _conservarCapa = true;
    if (quieto) {
      if (!bloqueada) _conservarCapa = false;
      _animacion.value = bloqueada ? 1 : 0;
    } else if (bloqueada) {
      if (_animacion.status != AnimationStatus.forward &&
          _animacion.status != AnimationStatus.completed) {
        _animacion.forward();
      }
    } else if (_animacion.status != AnimationStatus.reverse &&
        _animacion.status != AnimationStatus.dismissed) {
      _animacion.reverse();
    }
  }
}

/// Curva spring limitada a la entrada de marca (220 ms), terminando en 1.
class _CurvaResorte extends Curve {
  _CurvaResorte()
    : _simulacion = SpringSimulation(Movimiento.resorte, 0, 1, 0),
      _final = SpringSimulation(
        Movimiento.resorte,
        0,
        1,
        0,
      ).x(Movimiento.entrada.inMicroseconds / Duration.microsecondsPerSecond);

  final SpringSimulation _simulacion;
  final double _final;

  @override
  double transformInternal(double t) {
    if (t <= 0) return 0;
    if (t >= 1) return 1;
    return _simulacion.x(
          t *
              Movimiento.entrada.inMicroseconds /
              Duration.microsecondsPerSecond,
        ) /
        _final;
  }
}
