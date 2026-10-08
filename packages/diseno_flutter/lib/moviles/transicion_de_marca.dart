import 'package:flutter/material.dart';

import '../atomos/movimiento.dart';

/// **La transición al salir de la portada o la bienvenida** hacia el tour, el ingreso
/// o el alta: un fundido «a través» y la pantalla nueva que sube apenas al asentarse.
///
/// Antes era un «zoom de marca estilo Netflix» de un segundo y medio: un panel verde
/// cubría la pantalla, el logotipo crecía, lo cruzaba un destello y se lanzaba hacia la
/// cámara. Para una app donde la gente pone su plata eso se leía como un juego, y
/// obligaba a esperar 1,45 s cada vez que alguien tocaba «Crear cuenta». La marca ya se
/// presenta en la apertura de la app; acá alcanza con que el cambio sea limpio.
///
/// La pantalla vieja se apaga en el primer 35 % y recién entonces aparece la nueva,
/// así nunca se ven las dos superpuestas. Al volver, un fundido corto. Con «reducir
/// movimiento», nada.
abstract final class TransicionDeMarca {
  static const duracion = Duration(milliseconds: 420);
  static const duracionDeVuelta = Movimiento.paginaDeVuelta;

  /// Cuánto sube la pantalla nueva mientras aparece, en píxeles lógicos.
  static const _subida = 16.0;

  static const _aparece = Interval(0.35, 1, curve: Movimiento.llega);

  /// Firma de `transitionsBuilder` de `CustomTransitionPage` (go_router) y de
  /// `PageRouteBuilder`.
  static Widget construir(
    BuildContext context,
    Animation<double> animacion,
    Animation<double> secundaria,
    Widget pantalla,
  ) {
    if (MediaQuery.disableAnimationsOf(context)) return pantalla;
    return AnimatedBuilder(
      animation: animacion,
      child: pantalla,
      builder: (context, hijo) {
        final v = _aparece.transform(animacion.value);
        return Opacity(
          opacity: v,
          child: Transform.translate(
            offset: Offset(0, _subida * (1 - v)),
            child: hijo,
          ),
        );
      },
    );
  }
}
