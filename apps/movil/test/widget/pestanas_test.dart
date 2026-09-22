import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:aportaya_diseno/moviles/barra_pestanas.dart';
import 'package:go_router/go_router.dart';

/// **Cada pestaña abre su pantalla, no la primera ruta que encuentra.**
///
/// `StatefulShellBranch` arranca en la primera ruta declarada de la rama si no se le
/// dice otra cosa. Para `identidad` esa primera ruta es `/identidad`, que es **iniciar
/// sesión**: tocar «Perfil» dejaba a alguien ya identificado frente a un formulario de
/// login, sin saber por qué. Cada rama declara su casa, y esto lo vigila.
void main() {
  Widget appCon(GoRouter router) => ProviderScope(
    child: MaterialApp.router(
      theme: temaDesde(Tokens.claro, Brightness.light),
      routerConfig: router,
    ),
  );

  testWidgets('«Perfil» abre el perfil, nunca la pantalla de iniciar sesión', (
    tester,
  ) async {
    final router = crearEnrutador(inicial: '/billetera/inicio');
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Perfil'));
    await tester.pumpAndSettle();

    expect(router.state.uri.toString(), '/identidad/perfil');
    expect(
      router.state.uri.toString(),
      isNot('/identidad'),
      reason: 'esa es la pantalla de iniciar sesión',
    );
  });

  testWidgets(
    '«Grupos» abre el estado de los pasanakus, no el marcador vacío',
    (tester) async {
      final router = crearEnrutador(inicial: '/billetera/inicio');
      await tester.pumpWidget(appCon(router));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Grupos'));
      await tester.pumpAndSettle();

      expect(router.state.uri.toString(), '/pasanaku/mi-estado');
    },
  );

  testWidgets(
    '«Inicio» y «Movimientos» comparten rama y cada uno va a lo suyo',
    (tester) async {
      final router = crearEnrutador(inicial: '/billetera/inicio');
      await tester.pumpWidget(appCon(router));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Movimientos'));
      await tester.pumpAndSettle();
      expect(router.state.uri.toString(), startsWith('/billetera/extracto'));

      await tester.tap(find.text('Inicio'));
      await tester.pumpAndSettle();
      expect(router.state.uri.toString(), '/billetera/inicio');
    },
  );

  /// **Las pantallas de antes de tener sesión no llevan barra de pestañas.**
  ///
  /// El contrato de adhesión es el octavo paso de crear una cuenta, y vivía dentro del
  /// shell: se leía con «Inicio · Grupos · Movimientos · Perfil» debajo y «Perfil»
  /// encendido, como si ya hubiera una sesión y un perfil que mirar. Lo mismo la
  /// bienvenida, que se ve cuando la cuenta recién se pidió.
  for (final ruta in [
    '/portada',
    '/tour',
    '/ingreso',
    '/registro',
    '/identidad/contrato',
    '/identidad/bienvenida',
  ]) {
    testWidgets('$ruta se ve sin la barra de pestañas', (tester) async {
      // Un teléfono de verdad, no los 800×600 de fábrica del entorno de pruebas:
      // estas pantallas se maquetan para un celular y medirlas en una ventana que no
      // existe reporta desbordes que nadie ve nunca.
      tester.view.physicalSize = const Size(1290, 2796);
      tester.view.devicePixelRatio = 3;
      addTearDown(tester.view.reset);
      final router = crearEnrutador(inicial: ruta);
      await tester.pumpWidget(appCon(router));
      await tester.pumpAndSettle();

      expect(
        find.byType(BarraPestanas),
        findsNothing,
        reason:
            'ahí todavía no hay sesión: la barra ofrece destinos que no existen',
      );
    });
  }
}
