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
