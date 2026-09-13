import 'dart:ui' show lerpDouble;

import 'package:flutter/material.dart';

import '../atomos/marca.dart';
import '../tokens/tokens.dart';

/// **El zoom de marca, estilo Netflix**, al pasar de la bienvenida a lo que sigue.
///
/// Cuatro tiempos, en un poco más de un segundo:
/// 1. El sello verde de la bienvenida crece desde el centro hasta llenar la pantalla.
/// 2. La marca se queda grande en el medio: es el momento de verla.
/// 3. La marca se lanza hacia la cámara y se la atraviesa.
/// 4. Del otro lado aparece la pantalla nueva, asentándose desde apenas más grande.
///
/// Al volver no se repite el espectáculo: un fundido corto. La marca se presenta al
/// entrar, no cada vez que alguien retrocede. Con «reducir movimiento», nada.
abstract final class TransicionDeMarca {
  static const duracion = Duration(milliseconds: 1150);
  static const duracionDeVuelta = Duration(milliseconds: 280);

  /// Firma de `transitionsBuilder` de `CustomTransitionPage` (go_router) y de
  /// `PageRouteBuilder`.
  static Widget construir(
    BuildContext context,
    Animation<double> animacion,
    Animation<double> secundaria,
    Widget pantalla,
  ) {
    if (MediaQuery.disableAnimationsOf(context)) return pantalla;
    return _Zoom(animacion: animacion, pantalla: pantalla);
  }
}

class _Zoom extends StatelessWidget {
  const _Zoom({required this.animacion, required this.pantalla});

  final Animation<double> animacion;
  final Widget pantalla;

  /// El tamaño del sello de la bienvenida, del que arranca el panel.
  static const _sello = 72.0;

  static double _tramo(double v, double desde, double hasta) =>
      ((v - desde) / (hasta - desde)).clamp(0.0, 1.0);

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final medida = MediaQuery.sizeOf(context);
    return AnimatedBuilder(
      animation: animacion,
      child: pantalla,
      builder: (context, hijo) {
        final v = animacion.value;
        if (animacion.status == AnimationStatus.reverse) {
          return Opacity(opacity: v, child: hijo);
        }

        final expandir = Curves.easeInOutCubic.transform(_tramo(v, 0, 0.34));
        final mirar = Curves.easeInOut.transform(_tramo(v, 0.34, 0.62));
        final atravesar = Curves.easeInCubic.transform(_tramo(v, 0.62, 0.84));
        final aparece = Curves.easeOutCubic.transform(_tramo(v, 0.70, 1));

        final escalaMarca = 1 + 1.7 * expandir + 1.9 * mirar + 26 * atravesar;
        final opacidadMarca = 1 - _tramo(v, 0.74, 0.84);
        final opacidadPanel =
            _tramo(v, 0, 0.05) * (1 - Curves.easeOut.transform(_tramo(v, 0.80, 1)));

        return Stack(
          fit: StackFit.expand,
          children: [
            Opacity(
              opacity: aparece,
              child: Transform.scale(scale: 1.08 - 0.08 * aparece, child: hijo),
            ),
            IgnorePointer(
              child: ExcludeSemantics(
                child: Opacity(
                  opacity: opacidadPanel,
                  child: Center(
                    child: Container(
                      width: lerpDouble(_sello, medida.width, expandir),
                      height: lerpDouble(_sello, medida.height, expandir),
                      decoration: BoxDecoration(
                        color: t.verdeSolido,
                        borderRadius: BorderRadius.circular(
                          lerpDouble(Radios.lg + Espacio.s1, 0, expandir)!,
                        ),
                      ),
                      child: Center(
                        child: Opacity(
                          opacity: opacidadMarca,
                          child: Transform.scale(
                            scale: escalaMarca,
                            child: Marca(
                              tamano: 48,
                              colorDelTrazo: t.sobreVerdeSolido,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}
