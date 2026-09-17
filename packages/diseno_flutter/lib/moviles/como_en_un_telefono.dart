import 'dart:math' as matematica;

import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// En una pantalla ancha, la app se muestra DENTRO de un teléfono.
///
/// Esto es la app móvil corriendo en el navegador, no una version de escritorio: sin
/// esta contención, en un monitor de 1440 px los botones cruzaban la pantalla entera
/// de lado a lado y el contenido quedaba flotando en el medio. No es solo feo — una
/// pantalla pensada para el pulgar, estirada a un metro de ancho, deja de decir la
/// verdad sobre cómo se ve en un celular, que es justamente para lo que sirve tener la
/// app en la web.
///
/// Solo en web y solo cuando la ventana es mas ancha que un telefono: en un celular de
/// verdad —y en la app compilada— esto no se interpone, el árbol sigue igual.
///
/// El `MediaQuery` se reescribe a proposito. Sin eso, la app de adentro seguiria
/// creyendo que la pantalla mide lo que mide el monitor, y todo lo que decide por ancho
/// disponible tomaria la decision equivocada dentro de un marco angosto.
class ComoEnUnTelefono extends StatelessWidget {
  const ComoEnUnTelefono({super.key, required this.child});

  final Widget child;

  /// Un telefono grande (un iPhone Pro Max mide 430 puntos de ancho).
  static const double _anchoDeTelefono = 430;

  /// Por debajo de esto la ventana ya es del tamano de un telefono: no hay nada que
  /// contener y meterla en un marco solo le robaria lugar.
  static const double _desdeAca = 600;

  /// Alto de un telefono grande. Con una ventana mas alta, el marco no crece: un
  /// telefono de dos metros de alto no se parece a ningun telefono.
  static const double _altoDeTelefono = 932;

  /// Aire alrededor del marco, para que no toque los bordes de la ventana.
  static const double _margen = 48;

  /// Hasta donde se agranda. Mas que esto deja de parecer un telefono grande y empieza a
  /// parecer un telefono de juguete.
  static const double _escalaMaxima = 1.8;

  @override
  Widget build(BuildContext context) {
    if (!kIsWeb) return child;

    final medios = MediaQuery.of(context);
    if (medios.size.width < _desdeAca) return child;

    final tokens = Tokens.of(context);
    final alto = matematica.min(medios.size.height - _margen, _altoDeTelefono);
    final pantalla = Size(_anchoDeTelefono, alto);

    // El telefono se AGRANDA cuando sobra pantalla, en vez de quedar como una estampilla
    // en medio del monitor. En un monitor de 3440x1440, 430 puntos sobre 3440 es una
    // franja ilegible; escalado ocupa el alto disponible y se lee de lejos.
    //
    // Es un escalado visual, no un cambio de tamano: por dentro la app sigue midiendo
    // 430 puntos, asi que maqueta igual que en un telefono. Si en vez de esto se le diera
    // mas ancho, dejaria de ser la pantalla que se esta probando.
    //
    // Solo hacia arriba y hasta 1,8: mas que eso no parece un telefono grande, parece un
    // telefono de juguete. Y nunca mas ancho que la ventana.
    final escala = matematica.min(
      ((medios.size.height - _margen) / _altoDeTelefono).clamp(
        1.0,
        _escalaMaxima,
      ),
      (medios.size.width - _margen) / _anchoDeTelefono,
    );

    return ColoredBox(
      color: tokens.surface2,
      child: Center(
        child: SizedBox(
          width: _anchoDeTelefono * escala,
          height: alto * escala,
          child: FittedBox(
            fit: BoxFit.fill,
            child: SizedBox(
              width: _anchoDeTelefono,
              height: alto,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(28),
                  border: Border.all(color: tokens.border),
                  boxShadow: [tokens.sombra3],
                ),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(28),
                  child: MediaQuery(
                    // Sin muescas ni barra de gestos: adentro de un marco en el navegador
                    // no hay nada que esquivar, y dejar el `padding` del monitor le abriria
                    // un hueco arriba que en un telefono no existe.
                    data: medios.copyWith(
                      size: pantalla,
                      padding: EdgeInsets.zero,
                      viewPadding: EdgeInsets.zero,
                      viewInsets: EdgeInsets.zero,
                    ),
                    child: child,
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
