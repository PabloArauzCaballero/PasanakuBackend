import 'dart:ui' show lerpDouble;

import 'package:flutter/material.dart';

import '../atomos/logotipo.dart';
import '../tokens/tokens.dart';
import 'panel_de_marca.dart';

/// **El zoom de marca, estilo Netflix**, al salir de la portada o la bienvenida hacia
/// cualquiera de sus destinos: el tour, el ingreso y el alta de cuenta.
///
/// Cuatro tiempos, en un segundo y medio:
/// 1. El panel verde cubre la pantalla entera y se acerca: aparece ya a pantalla
///    completa, apenas más grande, y se asienta.
/// 2. Aparece el logotipo entero —isotipo y palabra—, lo cruza un destello y se queda
///    grande: es el momento de verlo.
/// 3. El logotipo se lanza hacia la cámara y se lo atraviesa.
/// 4. Del otro lado ya está la pantalla nueva, asentándose desde apenas más grande.
///
/// **El panel no crece desde un sello de 72 px.** Así estaba, y era el bug que se veía
/// en la web: un rectángulo verde chico se materializaba en el medio de la pantalla
/// anterior —que seguía entera y a la vista detrás— con el logotipo asomando cortado
/// por sus bordes, porque el panel recorta y el logotipo se mide en grande. Parecía una
/// calcomanía pegada encima, o un error de dibujo. El sello del que supuestamente
/// crecía solo existe en la bienvenida; desde la portada, y desde cualquier otro lado,
/// arrancaba de la nada. Un panel que cubre desde el primer cuadro no depende de qué
/// había debajo y no tiene bordes contra los que recortar nada.
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

  /// Cuánto de la pantalla ocupa el isotipo cuando está quieto en el medio.
  ///
  /// El tope lo pone la palabra, que es más ancha que el isotipo y encima sigue
  /// creciendo durante la pausa: con 0,44 llegaba a medir más que la pantalla y
  /// «AportaYa» se cortaba en las dos puntas justo en el cuadro que se quiere mirar.
  /// 0,42 × 1,9 de palabra × 1,12 de empuje = 89 % del ancho, con aire a los costados.
  static const _parteDeLaPantalla = 0.42;

  /// De cuánto arranca el logotipo. Por debajo de esto el isotipo es una mancha y la
  /// palabra todavía no se lee; el tramo que sobra no cuenta nada.
  static const _logoDesde = 0.55;

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

        // El panel entra a pantalla completa: se acerca y se asienta, no crece desde
        // un sello. `cubrir` es lo único que pasa en el primer tramo.
        final cubrir = Curves.easeOutCubic.transform(_tramo(v, 0, 0.14));
        final crecer = Curves.easeOutCubic.transform(_tramo(v, 0.10, 0.36));
        final mirar = Curves.easeInOut.transform(_tramo(v, 0.36, 0.66));
        final atravesar = Curves.easeInCubic.transform(_tramo(v, 0.66, 0.90));
        final destino = Curves.easeOutCubic.transform(_tramo(v, 0.50, 0.76));
        final asiento = Curves.easeOutCubic.transform(_tramo(v, 0.66, 1));

        // Del tamaño de entrada al tamaño grande, y de ahí a la cámara.
        final escala =
            lerpDouble(_logoDesde, 1, crecer)! + 0.12 * mirar + 20 * atravesar;

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
                      cubrir *
                      (1 - Curves.easeOut.transform(_tramo(v, 0.88, 1))),
                  // El panel es siempre la pantalla entera. Lo que se mueve es el
                  // acercamiento: entra al 108 % y se asienta al 100 %.
                  child: Transform.scale(
                    scale: 1.08 - 0.08 * cubrir,
                    child: PanelDeMarca(
                      ancho: medida.width,
                      alto: medida.height,
                      radio: 0,
                      resplandor: cubrir,
                      destello: _tramo(v, 0.38, 0.70),
                      hijo: Opacity(
                        opacity:
                            _tramo(v, 0.08, 0.18) * (1 - _tramo(v, 0.80, 0.92)),
                        child: Transform.scale(
                          scale: escala,
                          child: Logotipo(
                            tamano: lado,
                            colorDelTrazo: t.sobreVerdeSolido,
                            opacidadDeLaPalabra: _tramo(v, 0.24, 0.44),
                            volumen: true,
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
