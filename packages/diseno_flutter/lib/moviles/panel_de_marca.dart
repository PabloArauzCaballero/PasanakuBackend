import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// El sello verde que crece hasta ser pantalla durante el zoom de marca, con sus
/// cuatro capas de luz.
///
/// Un relleno verde plano detrás de un logo plano es lo que hace que la marca se vea
/// impresa en una cartulina. Acá hay: un degradado de fondo que baja hacia el verde
/// tinta, un resplandor detrás del logotipo como si la luz viniera de atrás, un
/// viñeteo que apaga las esquinas —lo que en una foto separa el sujeto del fondo— y un
/// destello que cruza en diagonal mientras la marca está quieta.
///
/// Ninguna capa usa desenfoque: son degradados, así que el panel se dibuja nítido a
/// cualquier escala, incluso cuando el logotipo mide veinte pantallas.
class PanelDeMarca extends StatelessWidget {
  const PanelDeMarca({
    super.key,
    required this.ancho,
    required this.alto,
    required this.radio,
    required this.resplandor,
    required this.destello,
    required this.hijo,
  });

  final double ancho;
  final double alto;
  final double radio;

  /// De 0 a 1: cuánto se encendió el resplandor de atrás. Se enciende con el panel;
  /// en 72 px no se vería y solo ensuciaría el borde.
  final double resplandor;

  /// De 0 a 1: por dónde va el destello. Fuera de ese rango, no se dibuja.
  final double destello;

  final Widget hijo;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Center(
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
            _Capa(
              RadialGradient(
                center: const Alignment(0, -0.15),
                radius: 0.75,
                colors: [
                  t.brand.withValues(alpha: 0.6 * resplandor),
                  t.brand.withValues(alpha: 0),
                ],
              ),
            ),
            _Capa(
              RadialGradient(
                radius: 0.95,
                colors: [
                  Paleta.g900.withValues(alpha: 0),
                  Paleta.g900.withValues(alpha: 0.42 * resplandor),
                ],
                stops: const [0.45, 1],
              ),
            ),
            // El logotipo se mide siempre en grande y se escala; sin esto lo aprieta
            // el panel, que arranca de 72 px, y sale la barra amarilla de desborde.
            OverflowBox(
              minWidth: 0,
              maxWidth: double.infinity,
              minHeight: 0,
              maxHeight: double.infinity,
              child: hijo,
            ),
            if (destello > 0 && destello < 1)
              IgnorePointer(
                child: Transform.translate(
                  offset: Offset(ancho * (destello * 2.6 - 1.3), 0),
                  child: Transform.rotate(
                    angle: -0.42,
                    child: SizedBox(
                      width: ancho * 0.5,
                      height: alto * 2.8,
                      child: _Capa(
                        LinearGradient(
                          begin: Alignment.centerLeft,
                          end: Alignment.centerRight,
                          colors: [
                            Paleta.white.withValues(alpha: 0),
                            Paleta.white.withValues(alpha: 0.1),
                            Paleta.white.withValues(alpha: 0),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _Capa extends StatelessWidget {
  const _Capa(this.degradado);
  final Gradient degradado;

  @override
  Widget build(BuildContext context) => DecoratedBox(
    decoration: BoxDecoration(gradient: degradado),
    child: const SizedBox.expand(),
  );
}
