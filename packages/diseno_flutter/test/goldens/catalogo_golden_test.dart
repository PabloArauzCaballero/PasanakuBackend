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

import 'package:aportaya_diseno/catalogo/catalogo.dart';
import 'package:aportaya_diseno/catalogo/muestras.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// Un golden por grupo del catálogo, en claro y en oscuro, a 360 px. Es lo que se
/// compara contra `/catalogo` de Angular y contra la maqueta en la revisión visual.
/// `--update-goldens` no se corre a ciegas.
void main() {
  for (final grupo in catalogo) {
    for (final (brillo, nombre) in [
      (Brightness.light, 'claro'),
      (Brightness.dark, 'oscuro'),
    ]) {
      testWidgets('${grupo.grupo} · $nombre', (tester) async {
        tester.view.physicalSize = const Size(360, 2400);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        await tester.pumpWidget(
          conTema(
            PaginaDeCatalogo(grupo: grupo),
            brillo: brillo,
            pantallaEntera: true,
          ),
        );
        await tester.pump();
        await tester.pump(const Duration(milliseconds: 300));
        final archivo = sinAcentos(grupo.grupo)
            .toLowerCase()
            .replaceAll(RegExp(r'[^a-z]+'), '_')
            .replaceAll(RegExp(r'^_|_$'), '');
        await expectLater(
          find.byType(PaginaDeCatalogo),
          matchesGoldenFile('imagenes/${archivo}_$nombre.png'),
        );
      });
    }
  }
}

/// «Átomos · selección» → «atomos_seleccion»: un nombre de archivo sin acentos.
String sinAcentos(String s) => s
    .replaceAll('á', 'a')
    .replaceAll('é', 'e')
    .replaceAll('í', 'i')
    .replaceAll('ó', 'o')
    .replaceAll('ú', 'u')
    .replaceAll('ñ', 'n')
    .replaceAll('Á', 'A')
    .replaceAll('É', 'E')
    .replaceAll('Í', 'I')
    .replaceAll('Ó', 'O')
    .replaceAll('Ú', 'U');
