import 'package:flutter/material.dart';

/// **La transición de AportaYa: se entra hacia adentro, no de costado.**
///
/// La pantalla que llega arranca al 88 % y crece hasta su tamaño mientras aparece; la
/// que se va sigue creciendo un poco más y se desvanece, como si quedara atrás al
/// pasarle por encima. Es el mismo gesto que la apertura de marca —acercarse— aplicado
/// a la navegación, para que abrir una pantalla se sienta como entrar en ella.
///
/// El empujón lateral de iOS es lo que trae Flutter de fábrica; esto es una decisión.
/// Al volver, la animación corre al revés: la pantalla se aleja y encoge, así el gesto
/// de retroceso se lee como salir.
class TransicionConZoom extends PageTransitionsBuilder {
  const TransicionConZoom();

  /// Cuánto mide la pantalla que entra cuando arranca. Más abajo de 0.85 se siente
  /// un salto; más arriba de 0.92 no se percibe.
  static const _desde = 0.88;

  /// Lo que se agranda la que sale. Poco: es un fondo alejándose, no otra animación
  /// compitiendo por la atención.
  static const _hasta = 1.06;

  @override
  Duration get transitionDuration => const Duration(milliseconds: 340);

  @override
  Duration get reverseTransitionDuration => const Duration(milliseconds: 260);

  @override
  Widget buildTransitions<T>(
    PageRoute<T>? route,
    BuildContext context,
    Animation<double> animation,
    Animation<double> secondaryAnimation,
    Widget child,
  ) {
    if (MediaQuery.disableAnimationsOf(context)) return child;

    final entra = CurvedAnimation(
      parent: animation,
      curve: Curves.easeOutCubic,
      reverseCurve: Curves.easeInCubic,
    );
    final sale = CurvedAnimation(
      parent: secondaryAnimation,
      curve: Curves.easeOutCubic,
      reverseCurve: Curves.easeInCubic,
    );

    return AnimatedBuilder(
      animation: sale,
      builder: (context, hijo) => Transform.scale(
        // La de abajo se aleja mientras la nueva se le pone encima.
        scale: 1 + (_hasta - 1) * sale.value,
        child: Opacity(opacity: 1 - sale.value * 0.35, child: hijo),
      ),
      child: FadeTransition(
        opacity: entra,
        child: ScaleTransition(
          scale: Tween(begin: _desde, end: 1.0).animate(entra),
          child: child,
        ),
      ),
    );
  }
}
