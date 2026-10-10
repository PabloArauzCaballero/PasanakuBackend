import 'package:flutter/material.dart';

import '../atomos/movimiento.dart';

/// **La transición entre pantallas: un eje lateral corto y un fundido.**
///
/// Reemplaza al zoom (la pantalla nueva crecía desde el 88 % mientras la de atrás se
/// inflaba y se lavaba). En una app de dinero ese gesto se leía como un efecto de
/// presentación: cada toque era un espectáculo y la pantalla de atrás quedaba borrosa
/// y agrandada durante un tercio de segundo, justo cuando la persona busca dónde está.
///
/// Ahora: la pantalla que llega entra 30 px desde la derecha y aparece; la que se va se
/// corre 30 px a la izquierda y se apaga **antes** de que la nueva esté a media
/// opacidad, así nunca se ven dos pantallas superpuestas. Nada se escala. Al volver,
/// el mismo eje al revés: se lee como retroceder.
///
/// En iOS se usa la transición del sistema (`CupertinoPageTransitionsBuilder`), porque
/// es la única que trae el gesto de deslizar desde el borde para volver; quitarle eso a
/// una persona de iPhone es romperle la mano, no una decisión de marca.
class TransicionDeEje extends PageTransitionsBuilder {
  const TransicionDeEje();

  @override
  Duration get transitionDuration => Movimiento.pagina;

  @override
  Duration get reverseTransitionDuration => Movimiento.paginaDeVuelta;

  @override
  Widget buildTransitions<T>(
    PageRoute<T>? route,
    BuildContext context,
    Animation<double> animation,
    Animation<double> secondaryAnimation,
    Widget child,
  ) {
    if (MediaQuery.disableAnimationsOf(context)) return child;
    return _Eje(llegada: animation, partida: secondaryAnimation, child: child);
  }
}

class _Eje extends StatelessWidget {
  const _Eje({
    required this.llegada,
    required this.partida,
    required this.child,
  });

  final Animation<double> llegada;
  final Animation<double> partida;
  final Widget child;

  /// La que llega recién aparece después de que la otra empezó a irse (fundido «a
  /// través»): 0–30 % se va la vieja, 30–100 % llega la nueva.
  static const _aparece = Interval(0.3, 1, curve: Movimiento.llega);
  static const _desaparece = Interval(0, 0.3, curve: Movimiento.sale);

  @override
  Widget build(BuildContext context) {
    final direccion = Directionality.of(context) == TextDirection.rtl ? -1 : 1;
    return AnimatedBuilder(
      animation: Listenable.merge([llegada, partida]),
      child: child,
      builder: (context, hijo) {
        final entra = _aparece.transform(llegada.value);
        final corre = Movimiento.llega.transform(llegada.value);
        final sale = partida.value;
        final apaga = _desaparece.transform(sale);
        final dx =
            direccion *
            Movimiento.corrimiento *
            ((1 - corre) - Movimiento.llega.transform(sale));
        return Opacity(
          opacity: (entra * (1 - apaga)).clamp(0.0, 1.0),
          child: Transform.translate(offset: Offset(dx, 0), child: hijo),
        );
      },
    );
  }
}
