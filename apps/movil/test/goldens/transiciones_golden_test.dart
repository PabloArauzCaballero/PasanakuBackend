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
/// Una animación de navegación no se revisa mirándola pasar: en el teléfono dura
/// 300 ms y a esa velocidad cualquier cosa «parece que anda». Acá se detiene el reloj
/// en tres momentos del empujón de iOS —recién arrancando, a mitad, casi llegando— y
/// queda como imagen. Si un día alguien cambia la curva, lo saca el diff.
void main() {
  testWidgets('portada → ingreso: el empujón de iOS, cuadro por cuadro', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(1170, 2532);
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.reset);
    final router = crearEnrutador();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria())],
        child: MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
          routerConfig: router,
          debugShowCheckedModeBanner: false,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Ya tengo cuenta'));
    // `pump()` sin duración arranca la transición; los tres siguientes la detienen
    // en el punto donde se ve de qué lado entra la pantalla nueva.
    await tester.pump();
    for (final (nombre, avance) in [
      ('inicio', 60),
      ('medio', 90),
      ('final', 90),
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
