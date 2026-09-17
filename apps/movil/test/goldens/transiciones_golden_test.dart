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

import '../comun.dart';

/// **Las transiciones, congeladas a mitad de camino.**
///
/// Una animación de navegación no se revisa mirándola pasar: dura poco más de un
/// segundo y a esa velocidad cualquier cosa «parece que anda». Acá se detiene el reloj
/// en los cuatro tiempos del zoom de marca —el sello creciendo, el logotipo grande y
/// quieto, el logotipo yéndose hacia la cámara, la pantalla nueva asentándose— y cada
/// uno queda como imagen. Si un día alguien cambia la curva, lo saca el diff.
///
/// El segundo cuadro es el que importa: ahí tiene que estar el logotipo **entero**,
/// isotipo y palabra, ocupando media pantalla. Si vuelve a aparecer solo el isotipo
/// chiquito, esta prueba lo muestra sin que nadie tenga que grabar un video.
void main() {
  testWidgets('portada → ingreso: el zoom de marca, cuadro por cuadro', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(1170, 2532);
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.reset);
    final router = crearEnrutador();
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
    // `pump()` sin duración arranca la transición; los siguientes la detienen en cada
    // tiempo del zoom. Los milisegundos son acumulados: 200, 720, 1150 y 1400 de los
    // 1450 que dura.
    await tester.pump();
    for (final (nombre, avance) in [
      ('sello', 200),
      ('marca', 520),
      ('atraviesa', 430),
      ('llega', 250),
    ]) {
      await tester.pump(Duration(milliseconds: avance));
      await expectLater(
        find.byType(MaterialApp),
        matchesGoldenFile('imagenes/transicion_ingreso_$nombre.png'),
      );
    }
    await tester.pumpAndSettle();
    expect(router.state.uri.toString(), '/ingreso');
  });
}
