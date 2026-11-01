import 'package:aportaya_diseno/atomos/barra_de_puntos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/moleculas/aparicion_escalonada.dart';
import 'package:aportaya_diseno/moviles/barra_pestanas.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/app.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_de_portada.dart';
import 'package:aportaya_movil/proveedores/capacidades.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:aportaya_movil/infraestructura/capacidades.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import '../comun.dart';

/// **La entrada**: bienvenida → alta, y el tour de cuatro láminas a pedido.
///
/// «Crear mi cuenta» va directo al alta (2026-10-28, pedido de Pablo con las skills de
/// signup/onboarding: un tour obligatorio es fricción para quien ya decidió). El tour
/// se abre desde «¿Cómo funciona?».
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
    final router = crearEnrutador(sesion: await sesionAnonimaDePrueba());
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();
    return router;
  }

  Future<void> abrirTour(WidgetTester tester) async {
    await tester.tap(find.text(TextosIdentidad.portadaComoFunciona));
    await tester.pumpAndSettle();
  }

  testWidgets('la app abre en la bienvenida, no adentro de la billetera', (
    tester,
  ) async {
    final router = await abrir(tester);
    expect(router.state.uri.toString(), '/portada');
    expect(find.text(TextosIdentidad.portadaCrearCuenta), findsOneWidget);
    expect(find.text(TextosIdentidad.portadaYaTengoCuenta), findsOneWidget);
    expect(find.byType(Boton), findsNWidgets(2));
    expect(find.text(TextosIdentidad.portadaComoFunciona), findsOneWidget);
    expect(find.byType(BarraPestanas), findsNothing);
  });

  testWidgets('«¿Cómo funciona?» abre el tour sin cambiar las dos puertas', (
    tester,
  ) async {
    final router = await abrir(tester);
    await tester.tap(find.text(TextosIdentidad.portadaComoFunciona));
    await tester.pumpAndSettle();

    expect(router.state.uri.toString(), '/tour');
    expect(find.byType(PageView), findsOneWidget);
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

  testWidgets('marca, titular, garantías y botones entran en ese orden', (
    tester,
  ) async {
    await abrir(tester);
    final entradas = tester
        .widgetList<AparicionEscalonada>(find.byType(AparicionEscalonada))
        .toList();
    expect(entradas, hasLength(2));
    expect(entradas.first.children, hasLength(3));
    expect(entradas.first.escalon, const Duration(milliseconds: 40));
    expect(entradas.first.duracionPorHijo, const Duration(milliseconds: 220));
    expect(entradas.last.demoraInicial, const Duration(milliseconds: 120));
    expect(find.byKey(const ValueKey('portada-botones')), findsOneWidget);
  });

  testWidgets('las puertas no reciben toques antes de hacerse visibles', (
    tester,
  ) async {
    final router = crearEnrutador(sesion: await sesionAnonimaDePrueba());
    await tester.pumpWidget(appCon(router));
    await tester.pump();
    final bloqueo = find
        .ancestor(
          of: find.byKey(const ValueKey('portada-botones')),
          matching: find.byType(IgnorePointer),
        )
        .first;
    expect(tester.widget<IgnorePointer>(bloqueo).ignoring, isTrue);
  });

  testWidgets(
    'arrancando la app de verdad —no solo el enrutador— se llega a la bienvenida',
    (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
            capacidadesProvider.overrideWithValue(
              capacidadesDeLaPlataforma(forzarIOSParaPruebas: true),
            ),
          ],
          child: const AppAportaYa(),
        ),
      );
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

  testWidgets('«Crear mi cuenta» abre el alta directo, sin pasar por el tour', (
    tester,
  ) async {
    final router = await abrir(tester);
    await tester.tap(find.text(TextosIdentidad.portadaCrearCuenta));
    await tester.pumpAndSettle();
    expect(router.state.uri.toString(), '/registro');
    expect(find.byType(PageView), findsNothing);
    expect(find.byType(BarraPestanas), findsNothing);
  });

  testWidgets('la bienvenida dice qué hay que tener a mano para el alta', (
    tester,
  ) async {
    await abrir(tester);
    for (final texto in [
      TextosDePortada.necesitasTitulo,
      TextosDePortada.necesitasCarnet,
      TextosDePortada.necesitasContacto,
      TextosDePortada.necesitasDespues,
    ]) {
      expect(find.text(texto), findsOneWidget);
    }
  });

  testWidgets('el tour sigue teniendo cuatro láminas', (tester) async {
    final router = await abrir(tester);
    await abrirTour(tester);
    expect(router.state.uri.toString(), '/tour');
    expect(find.byType(PageView), findsOneWidget);
    expect(tester.widget<BarraDePuntos>(find.byType(BarraDePuntos)).total, 4);
    expect(find.byType(BarraPestanas), findsNothing);
  });

  testWidgets('«Siguiente» recorre las láminas, y «Empezar» abre el alta', (
    tester,
  ) async {
    final router = await abrir(tester);
    await abrirTour(tester);

    for (var i = 0; i < 3; i++) {
      await tester.tap(find.text(TextosIdentidad.tourSiguiente));
      await tester.pumpAndSettle();
    }
    expect(tester.widget<BarraDePuntos>(find.byType(BarraDePuntos)).actual, 3);
    expect(find.text(TextosIdentidad.tourSiguiente), findsNothing);

    await tester.tap(find.text(TextosIdentidad.tourEmpezar));
    await tester.pumpAndSettle();
    expect(router.state.uri.toString(), '/registro');
    expect(find.byType(BarraPestanas), findsNothing);
  });

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
