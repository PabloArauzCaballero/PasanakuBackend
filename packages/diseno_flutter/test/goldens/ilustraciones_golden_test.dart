// Goldens: se corren con `yarn test:goldens`, NO en el CI.
//
// Comparan pixeles, y el rasterizador de Flutter no da el mismo resultado en macOS que en
// Linux aunque la fuente sea la misma. Estas lineas base estan hechas en macOS; la primera
// vez que el CI llego a ejecutarlas —hasta hoy el paso de lint fallaba antes— reporto
// diferencias de 2,5% a 3,9% en quince imagenes, sin que nadie hubiera tocado el diseño.
// Un gate que falla por la maquina donde corre no mide el diseño: mide la maquina.
//
// Para que el CI pueda exigirlas hay que generar las lineas base en linux/amd64, que es
// donde corre, y regenerarlas siempre ahi. Mientras tanto siguen siendo lo que el propio
// catalogo dice que son: la herramienta de la revision visual contra la maqueta, que se
// mira con ojos y no con un umbral de pixeles.

import 'package:aportaya_diseno/ilustraciones/arte_en_custodia.dart';
import 'package:aportaya_diseno/ilustraciones/arte_fondo_de_garantia.dart';
import 'package:aportaya_diseno/ilustraciones/arte_sin_cuaderno.dart';
import 'package:aportaya_diseno/ilustraciones/arte_sorteo_a_la_vista.dart';
import 'package:aportaya_diseno/ilustraciones/lienzo.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// Las cuatro ilustraciones del tour, grandes y en los dos temas.
///
/// Son dibujos, no imágenes: acá se revisan como se revisaría un dibujo, mirándolo.
/// Y grandes a propósito —240 px— porque un vector que se ve bien chico puede ser un
/// borrón al tamaño que le toque mañana.
void main() {
  final pintores = <String, CustomPainter Function(Tokens)>{
    'sin_cuaderno': ArteSinCuaderno.new,
    'en_custodia': ArteEnCustodia.new,
    'sorteo_a_la_vista': ArteSorteoALaVista.new,
    'fondo_de_garantia': ArteFondoDeGarantia.new,
  };

  for (final (tema, brillo) in [
    ('claro', Brightness.light),
    ('oscuro', Brightness.dark),
  ]) {
    testWidgets('las cuatro ilustraciones · $tema', (tester) async {
      pantallaChica(tester);
      await tester.pumpWidget(
        conTema(
          Wrap(
            spacing: Espacio.s4,
            runSpacing: Espacio.s4,
            children: [
              for (final entrada in pintores.entries)
                Ilustracion(
                  pintor: entrada.value,
                  etiqueta: entrada.key,
                  tamano: 150,
                ),
            ],
          ),
          brillo: brillo,
        ),
      );
      await tester.pumpAndSettle();
      await expectLater(
        find.byType(Wrap),
        matchesGoldenFile('imagenes/ilustraciones_$tema.png'),
      );
    });
  }
}
