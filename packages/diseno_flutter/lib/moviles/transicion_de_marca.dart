import 'dart:ui' show lerpDouble;

import 'package:flutter/material.dart';

import '../atomos/logotipo.dart';
import '../tokens/tokens.dart';

/// **El zoom de marca, estilo Netflix**, al salir de la bienvenida hacia cualquiera de
/// sus tres destinos: el tour, el ingreso y el alta de cuenta.
///
/// Cuatro tiempos, en poco más de un segundo:
/// 1. El sello verde crece desde el centro hasta llenar la pantalla.
/// 2. Aparece el logotipo **entero** —isotipo y palabra— y se queda grande: es el
///    momento de verlo. Antes acá iba solo el isotipo, y en grande tres trazos
///    abiertos sin la palabra no se leen como el logo de nadie.
/// 3. El logotipo se lanza hacia la cámara y se lo atraviesa.
/// 4. Del otro lado aparece la pantalla nueva, asentándose desde apenas más grande.
///
/// Al volver no se repite el espectáculo: un fundido corto. La marca se presenta al
/// entrar, no cada vez que alguien retrocede. Con «reducir movimiento», nada.
abstract final class TransicionDeMarca {
  static const duracion = Duration(milliseconds: 1250);
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

  /// Cuánto de la pantalla ocupa el isotipo cuando está quieto en el medio. Con 0,46
  /// el logotipo entero —isotipo, aire y palabra— ocupa poco más de medio alto: se ve
  /// grande sin tocar los bordes.
  static const _parteDeLaPantalla = 0.44;

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

        final expandir = Curves.easeInOutCubic.transform(_tramo(v, 0, 0.30));
        final crecer = Curves.easeOutCubic.transform(_tramo(v, 0, 0.36));
        final mirar = Curves.easeInOut.transform(_tramo(v, 0.36, 0.64));
        final atravesar = Curves.easeInCubic.transform(_tramo(v, 0.64, 0.88));
        final aparece = Curves.easeOutCubic.transform(_tramo(v, 0.70, 1));

        // De la medida del sello a la medida grande, y de ahí a la cámara.
        final escala =
            lerpDouble(_sello * 0.55 / lado, 1, crecer)! +
            0.16 * mirar +
            21 * atravesar;

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
                  opacity:
                      _tramo(v, 0, 0.04) *
                      (1 - Curves.easeOut.transform(_tramo(v, 0.84, 1))),
                  child: _Panel(
                    t: t,
                    ancho: lerpDouble(_sello, medida.width, expandir)!,
                    alto: lerpDouble(_sello, medida.height, expandir)!,
                    radio: lerpDouble(Radios.lg + Espacio.s1, 0, expandir)!,
                    resplandor: expandir,
                    child: Opacity(
                      opacity: 1 - _tramo(v, 0.78, 0.90),
                      child: Transform.scale(
                        scale: escala,
                        child: Logotipo(
                          tamano: lado,
                          colorDelTrazo: t.sobreVerdeSolido,
                          opacidadDeLaPalabra: _tramo(v, 0.24, 0.42),
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

/// El sello que se convierte en pantalla: verde de marca con un degradado hacia el
/// verde tinta abajo, y un resplandor detrás del logotipo que aparece recién cuando el
/// panel es grande —en 72 px no se vería, y ahí solo ensuciaría el borde.
class _Panel extends StatelessWidget {
  const _Panel({
    required this.t,
    required this.ancho,
    required this.alto,
    required this.radio,
    required this.resplandor,
    required this.child,
  });

  final Tokens t;
  final double ancho;
  final double alto;
  final double radio;
  final double resplandor;
  final Widget child;

  @override
  Widget build(BuildContext context) => Center(
    child: Container(
      width: ancho,
      height: alto,
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(radio),
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [t.verdeSolido, t.brandInk],
        ),
      ),
      child: Stack(
        alignment: Alignment.center,
        children: [
          DecoratedBox(
            decoration: BoxDecoration(
              gradient: RadialGradient(
                colors: [
                  t.brand.withValues(alpha: 0.55 * resplandor),
                  t.brand.withValues(alpha: 0),
                ],
              ),
            ),
            child: const SizedBox.expand(),
          ),
          // El logotipo se mide siempre en grande y se escala; sin esto lo aprieta el
          // panel, que arranca de 72 px, y en el primer cuadro sale la barra amarilla
          // de desborde.
          OverflowBox(
            minWidth: 0,
            maxWidth: double.infinity,
            minHeight: 0,
            maxHeight: double.infinity,
            child: child,
          ),
        ],
      ),
    ),
  );
}
