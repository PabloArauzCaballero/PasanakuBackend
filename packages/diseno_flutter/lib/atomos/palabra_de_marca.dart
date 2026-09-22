import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// La palabra «AportaYa», con «Ya» en el naranja de acento, como en
/// `docs/Views/AportaYa-logo-horizontal.svg`.
///
/// Se dimensiona por el [ancho] de la caja, no por un tamaño de letra: el logotipo la
/// pide en proporción al isotipo, y un `fontSize` calculado sería un número de
/// tipografía fuera de la bóveda de tokens.
class PalabraDeMarca extends StatelessWidget {
  const PalabraDeMarca({super.key, required this.ancho, this.color});

  final double ancho;

  /// El color de «Aporta». «Ya» siempre va en acento: es la mitad de la marca.
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      label: 'AportaYa',
      image: true,
      child: ExcludeSemantics(
        child: SizedBox(
          width: ancho,
          child: FittedBox(
            fit: BoxFit.contain,
            child: Text.rich(
              TextSpan(
                children: [
                  TextSpan(
                    text: 'Aporta',
                    style: TextStyle(color: color ?? t.brandInk),
                  ),
                  TextSpan(
                    text: 'Ya',
                    style: TextStyle(color: t.accent),
                  ),
                ],
              ),
              // Sin `decoration: none` se hereda el estilo de emergencia de Flutter,
              // que subraya en amarillo doble. La palabra se dibuja también fuera de
              // un `Material` —la capa del zoom va por encima del Navigator—, así que
              // el atajo de «ya lo pone el tema» acá no vale.
              style: Tipo.titulo1.copyWith(decoration: TextDecoration.none),
            ),
          ),
        ),
      ),
    );
  }
}
