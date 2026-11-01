part of 'barra_pestanas.dart';

/// La píldora de la pestaña activa: un óvalo de marca detrás del ícono.
const double _anchoDePildora = Tactil.minimo + Espacio.s4;
const double _altoDePildora = Tactil.minimo - Espacio.s3;

/// La píldora que se desliza detrás de los íconos. [posicion] está en casilleros (0 = el
/// primero) y la mueve el resorte de la barra; acá solo se dibuja.
class _Pildora extends StatelessWidget {
  const _Pildora({
    required this.posicion,
    required this.casilleros,
    required this.color,
  });

  final Animation<double> posicion;
  final int casilleros;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Positioned.fill(
      child: LayoutBuilder(
        builder: (context, caja) {
          final ancho = caja.maxWidth / casilleros;
          return AnimatedBuilder(
            animation: posicion,
            builder: (context, _) => Align(
              alignment: Alignment.topLeft,
              child: Transform.translate(
                offset: Offset(
                  posicion.value * ancho + (ancho - _anchoDePildora) / 2,
                  Espacio.s2,
                ),
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: color,
                    borderRadius: BorderRadius.circular(Radios.pill),
                  ),
                  child: const SizedBox(
                    width: _anchoDePildora,
                    height: _altoDePildora,
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}
