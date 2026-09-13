import 'dart:ui' show lerpDouble;

import 'package:flutter/material.dart';

import '../atomos/logotipo.dart';
import '../tokens/tokens.dart';
import 'panel_de_marca.dart';

/// **El zoom de marca, estilo Netflix**, al salir de la bienvenida hacia cualquiera de
/// sus tres destinos: el tour, el ingreso y el alta de cuenta.
///
/// Cuatro tiempos, en un segundo y medio:
/// 1. El sello verde crece desde el centro hasta llenar la pantalla.
/// 2. Aparece el logotipo entero —isotipo y palabra—, lo cruza un destello y se queda
///    grande: es el momento de verlo.
/// 3. El logotipo se lanza hacia la cámara y se lo atraviesa.
/// 4. Del otro lado ya está la pantalla nueva, asentándose desde apenas más grande.
///
/// **La pantalla nueva termina de aparecer antes de que el panel se vaya** (opaca al
/// 76 %, el panel recién empieza a irse al 88 %). Al revés —que era como estaba— hay
/// un tramo en que el panel ya se fue, la pantalla nueva todavía está a media opacidad
/// y abajo se sigue viendo la vieja: las dos superpuestas, turbio, como un error de
/// dibujo. El panel es opaco justamente para que ese cambio no se vea.
///
/// Al volver no se repite el espectáculo: un fundido corto. La marca se presenta al
/// entrar, no cada vez que alguien retrocede. Con «reducir movimiento», nada.
abstract final class TransicionDeMarca {
  static const duracion = Duration(milliseconds: 1450);
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

  /// Cuánto de la pantalla ocupa el isotipo cuando está quieto en el medio.
  ///
  /// El tope lo pone la palabra, que es más ancha que el isotipo y encima sigue
  /// creciendo durante la pausa: con 0,44 llegaba a medir más que la pantalla y
  /// «AportaYa» se cortaba en las dos puntas justo en el cuadro que se quiere mirar.
  /// 0,42 × 1,9 de palabra × 1,12 de empuje = 89 % del ancho, con aire a los costados.
  static const _parteDeLaPantalla = 0.42;

  static double _tramo(double v, double desde, double hasta) =>
      ((v - desde) / (hasta - desde)).clamp(0.0, 1.0);

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final medida = MediaQuery.sizeOf(context);
    final lado = medida.width * _parteDeLaPantalla;
    return AnimatedBuilder(
      animation: animacion,
      child: pantalla,
      builder: (context, hijo) {
        final v = animacion.value;
        if (animacion.status == AnimationStatus.reverse) {
          // Al volver, la pantalla se achica y se apaga rápido: un fundido parejo deja
          // las dos pantallas superpuestas a media opacidad y se lee sucio.
          final queda = Curves.easeInCubic.transform(v);
          return Opacity(
            opacity: queda,
            child: Transform.scale(scale: 0.92 + 0.08 * queda, child: hijo),
          );
        }

        final expandir = Curves.easeInOutCubic.transform(_tramo(v, 0, 0.28));
        final crecer = Curves.easeOutCubic.transform(_tramo(v, 0, 0.34));
        final mirar = Curves.easeInOut.transform(_tramo(v, 0.34, 0.66));
        final atravesar = Curves.easeInCubic.transform(_tramo(v, 0.66, 0.90));
        final destino = Curves.easeOutCubic.transform(_tramo(v, 0.50, 0.76));
        final asiento = Curves.easeOutCubic.transform(_tramo(v, 0.66, 1));

        // Del tamaño del sello al tamaño grande, y de ahí a la cámara.
        final escala =
            lerpDouble(_sello * 0.55 / lado, 1, crecer)! +
            0.12 * mirar +
            20 * atravesar;

        return Stack(
          fit: StackFit.expand,
          children: [
            Opacity(
              opacity: destino,
              child: Transform.scale(scale: 1.09 - 0.09 * asiento, child: hijo),
            ),
            IgnorePointer(
              child: ExcludeSemantics(
                child: Opacity(
                  opacity:
                      _tramo(v, 0, 0.035) *
                      (1 - Curves.easeOut.transform(_tramo(v, 0.88, 1))),
                  child: PanelDeMarca(
                    ancho: lerpDouble(_sello, medida.width, expandir)!,
                    alto: lerpDouble(_sello, medida.height, expandir)!,
                    radio: lerpDouble(Radios.lg + Espacio.s1, 0, expandir)!,
                    resplandor: expandir,
                    destello: _tramo(v, 0.36, 0.70),
                    hijo: Opacity(
                      opacity: 1 - _tramo(v, 0.80, 0.92),
                      child: Transform.scale(
                        scale: escala,
                        child: Logotipo(
                          tamano: lado,
                          colorDelTrazo: t.sobreVerdeSolido,
                          opacidadDeLaPalabra: _tramo(v, 0.22, 0.42),
                          volumen: true,
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
