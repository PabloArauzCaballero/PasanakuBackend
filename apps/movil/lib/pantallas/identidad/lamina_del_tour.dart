import 'package:aportaya_diseno/ilustraciones/lienzo.dart';
import 'package:aportaya_diseno/ilustraciones/pedestal.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'laminas_del_tour.dart';

/// Una lámina, centrada, que se mueve con el dedo en capas.
///
/// **Paralaje, como en las presentaciones de Apple.** Mientras la página se desliza,
/// el dibujo avanza a poco más de la mitad de la velocidad del dedo y se achica
/// apenas; el texto va un poco más rápido que el dibujo y se desvanece. Las tres
/// capas a distinto ritmo son lo que da profundidad: todo moviéndose junto se lee
/// como una hoja plana corriéndose de costado.
///
/// [desplazamiento] es cuánto le falta a esta lámina para estar centrada: 0 en el
/// centro, ±1 una pantalla entera a un lado.
class LaminaDelTour extends StatelessWidget {
  const LaminaDelTour({
    super.key,
    required this.lamina,
    required this.desplazamiento,
  });

  final Lamina lamina;
  final double desplazamiento;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final ancho = MediaQuery.sizeOf(context).width;
    final quieto = MediaQuery.disableAnimationsOf(context);
    final d = quieto ? 0.0 : desplazamiento.clamp(-1.0, 1.0);
    final cerca = 1 - d.abs();

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: Espacio.s5),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Transform.translate(
            offset: Offset(d * ancho * 0.45, 0),
            child: Transform.scale(
              scale: 0.86 + 0.14 * cerca,
              child: Opacity(
                opacity: (0.25 + 0.75 * cerca).clamp(0.0, 1.0),
                child: Pedestal(
                  tamano: 200,
                  hijo: Ilustracion(
                    pintor: lamina.arte,
                    etiqueta: lamina.etiqueta,
                    tamano: 200,
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(height: Espacio.s5),
          Transform.translate(
            offset: Offset(d * ancho * 0.18, 0),
            child: Opacity(
              opacity: cerca.clamp(0.0, 1.0),
              child: Column(
                children: [
                  Semantics(
                    header: true,
                    child: Text.rich(
                      TextSpan(
                        children: [
                          TextSpan(text: '${lamina.titulo}\n'),
                          TextSpan(
                            text: lamina.destacado,
                            style: TextStyle(color: t.brandTexto),
                          ),
                        ],
                      ),
                      textAlign: TextAlign.center,
                      style: Tipo.titulo1.copyWith(color: t.text),
                    ),
                  ),
                  const SizedBox(height: Espacio.s3),
                  // El ancho del párrafo lo fija la maqueta (`max-width: 31ch`): un
                  // renglón largo centrado se lee mal, y acá centrado es la norma.
                  ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 320),
                    child: Text(
                      lamina.texto,
                      textAlign: TextAlign.center,
                      style: Tipo.cuerpo.copyWith(color: t.text2),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
