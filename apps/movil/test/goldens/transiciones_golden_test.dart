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

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'cargar_fuentes_de_aplicacion.dart';
import '../comun.dart';

/// Congela los hitos del fundido a través de la navegación: primero una superficie
/// opaca (sin asomar la pantalla anterior), luego aparece la pantalla nueva y termina
/// asentada. Las capturas impiden que regresen el zoom largo de marca o la superposición
/// de páginas.
void main() {
  setUpAll(cargarFuentesDeAplicacion);

  testWidgets('portada → ingreso: el fundido no superpone las pantallas', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(1170, 2532);
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.reset);
    final router = crearEnrutador(sesion: await sesionAnonimaDePrueba());
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
        ],
        child: MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
          routerConfig: router,
          debugShowCheckedModeBanner: false,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Ya tengo cuenta'));
    // `pump()` sin duración arranca la transición. Se captura la superficie opaca,
    // el primer tercio del fundido, el asentamiento y el estado final a los 300 ms.
    await tester.pump();
    for (final (nombre, avance) in [
      ('cubre', 0),
      ('aparece', 165),
      ('asienta', 135),
    ]) {
      await tester.pump(Duration(milliseconds: avance));
      await expectLater(
        find.byType(MaterialApp),
        matchesGoldenFile('imagenes/transicion_ingreso_$nombre.png'),
      );
    }
    await tester.pumpAndSettle();
    await expectLater(
      find.byType(MaterialApp),
      matchesGoldenFile('imagenes/transicion_ingreso_final.png'),
    );
    expect(router.state.uri.toString(), '/ingreso');
  });
}
