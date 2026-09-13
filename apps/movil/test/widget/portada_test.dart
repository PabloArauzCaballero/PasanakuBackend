import 'package:aportaya_diseno/atomos/barra_de_puntos.dart';
import 'package:aportaya_diseno/moviles/barra_pestanas.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/app.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

/// **La entrada, como la maqueta**: bienvenida → tour de cuatro láminas → alta.
///
/// La app arrancaba dentro de una billetera ajena; después, en una sola pantalla con
/// todo junto y alineada a la izquierda. La maqueta (`identidad/bienvenida` y
/// `identidad/tour`) pide otra cosa, y la skill `disenar-frontend` lo fija: el
/// onboarding va **centrado**, con símbolo, título, texto y puntos.
void main() {
  Widget appCon(GoRouter router) => ProviderScope(
    child: MaterialApp.router(
      theme: temaDesde(Tokens.claro, Brightness.light),
      routerConfig: router,
    ),
  );

  Future<GoRouter> abrir(WidgetTester tester) async {
    tester.view.physicalSize = const Size(1170, 2532); // iPhone 13/14, 3x
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.reset);
    final router = crearEnrutador();
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();
    return router;
  }

  Future<void> abrirTour(WidgetTester tester) async {
    await tester.tap(find.text(TextosIdentidad.portadaCrearCuenta));
    await tester.pumpAndSettle();
  }

  testWidgets('la app abre en la bienvenida, no adentro de la billetera', (
    tester,
  ) async {
    final router = await abrir(tester);
    expect(router.state.uri.toString(), '/portada');
    expect(find.text(TextosIdentidad.portadaCrearCuenta), findsOneWidget);
    expect(find.text(TextosIdentidad.portadaYaTengoCuenta), findsOneWidget);
    expect(find.byType(BarraPestanas), findsNothing);
  });

  testWidgets('la bienvenida está centrada', (tester) async {
    await abrir(tester);
    for (final texto in [
      TextosIdentidad.bienvenidaTitulo,
      TextosIdentidad.bienvenidaTexto,
    ]) {
      expect(tester.widget<Text>(find.text(texto)).textAlign, TextAlign.center);
    }
  });

  testWidgets(
    'arrancando la app de verdad —no solo el enrutador— se llega a la bienvenida',
    (tester) async {
      await tester.pumpWidget(ProviderScope(child: AppAportaYa()));
      await tester.pumpAndSettle();
      expect(find.text(TextosIdentidad.portadaCrearCuenta), findsOneWidget);
      expect(find.text('Tu billetera'), findsNothing);
    },
  );

  testWidgets('«Ya tengo cuenta» lleva al ingreso, sin barra de pestañas', (
    tester,
  ) async {
    final router = await abrir(tester);
    await tester.tap(find.text(TextosIdentidad.portadaYaTengoCuenta));
    await tester.pumpAndSettle();
    expect(router.state.uri.toString(), '/ingreso');
    expect(find.byType(BarraPestanas), findsNothing);
  });

  testWidgets('«Crear mi cuenta» abre el tour de cuatro láminas', (
    tester,
  ) async {
    final router = await abrir(tester);
    await abrirTour(tester);
    expect(router.state.uri.toString(), '/tour');
    expect(find.byType(PageView), findsOneWidget);
    expect(tester.widget<BarraDePuntos>(find.byType(BarraDePuntos)).total, 4);
    expect(find.byType(BarraPestanas), findsNothing);
  });

  testWidgets(
    '«Siguiente» recorre las láminas, y en la última el botón abre el alta',
    (tester) async {
      final router = await abrir(tester);
      await abrirTour(tester);

      for (var i = 0; i < 3; i++) {
        await tester.tap(find.text(TextosIdentidad.tourSiguiente));
        await tester.pumpAndSettle();
      }
      expect(
        tester.widget<BarraDePuntos>(find.byType(BarraDePuntos)).actual,
        3,
      );
      expect(find.text(TextosIdentidad.tourSiguiente), findsNothing);

      await tester.tap(find.text(TextosIdentidad.portadaCrearCuenta));
      await tester.pumpAndSettle();
      expect(router.state.uri.toString(), '/registro');
      expect(find.byType(BarraPestanas), findsNothing);
    },
  );

  testWidgets('«Saltar» va directo al alta, sin barra de pestañas', (
    tester,
  ) async {
    final router = await abrir(tester);
    await abrirTour(tester);
    await tester.tap(find.text(TextosIdentidad.portadaSaltar));
    await tester.pumpAndSettle();
    expect(router.state.uri.toString(), '/registro');
    expect(
      find.byType(BarraPestanas),
      findsNothing,
      reason: 'el alta no puede traer la barra de pestañas de la app',
    );
  });
}
