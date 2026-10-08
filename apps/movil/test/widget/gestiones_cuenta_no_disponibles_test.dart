import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/casilla.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_baja.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_contrasena.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_dispositivos.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_perfil.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import '../comun.dart';

void main() {
  final pantallas =
      <({String nombre, Widget pantalla, String aviso, String boton})>[
        (
          nombre: 'dispositivos',
          pantalla: const PantallaDeDispositivos(),
          aviso: TextosIdentidad.dispositivosNoDisponibles,
          boton: TextosIdentidad.volverAlPerfil,
        ),
        (
          nombre: 'contraseña',
          pantalla: const PantallaDeContrasena(),
          aviso: TextosIdentidad.contrasenaNoDisponible,
          boton: TextosIdentidad.volverAlPerfil,
        ),
        (
          nombre: 'baja',
          pantalla: const PantallaDeBaja(),
          aviso: TextosIdentidad.bajaNoDisponible,
          boton: TextosIdentidad.volverAlPerfil,
        ),
        (
          nombre: 'perfil',
          pantalla: const PantallaDePerfil(),
          aviso: TextosIdentidad.perfilNoDisponible,
          boton: TextosIdentidad.cerrarSesion,
        ),
      ];

  for (final pantalla in pantallas) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      testWidgets(
        '${pantalla.nombre} no finge gestionar la cuenta · 320/200 · ${brillo.name}',
        (tester) async {
          tester.view.physicalSize = const Size(320, 760);
          tester.view.devicePixelRatio = 1;
          addTearDown(tester.view.reset);
          await tester.pumpWidget(
            conApp(
              pantalla.pantalla,
              dio: dioSimulado().dio,
              brillo: brillo,
              escalaDeTexto: 2,
            ),
          );
          await tester.pumpAndSettle();

          expect(find.text(pantalla.aviso), findsOneWidget);
          expect(find.byType(TextField), findsNothing);
          expect(find.byType(Casilla), findsNothing);
          expect(find.text('Guardar'), findsNothing);
          expect(find.text('Confirmar baja'), findsNothing);
          final accion = find.widgetWithText(Boton, pantalla.boton);
          expect(accion, findsOneWidget);
          expect(tester.getSize(accion).height, greaterThanOrEqualTo(48));
          expect(tester.takeException(), isNull);
        },
      );
    }
  }

  testWidgets('volver al perfil sale de la gestión no disponible', (
    tester,
  ) async {
    final router = GoRouter(
      initialLocation: '/identidad/baja',
      routes: [
        GoRoute(
          path: '/identidad/baja',
          builder: (_, _) => const PantallaDeBaja(),
        ),
        GoRoute(
          path: '/identidad/perfil',
          builder: (_, _) => const Scaffold(body: Text('Perfil destino')),
        ),
      ],
    );
    addTearDown(router.dispose);
    await tester.pumpWidget(
      MaterialApp.router(
        theme: temaDesde(Tokens.claro, Brightness.light),
        routerConfig: router,
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text(TextosIdentidad.volverAlPerfil));
    await tester.pumpAndSettle();
    expect(router.state.uri.path, '/identidad/perfil');
    expect(find.text('Perfil destino'), findsOneWidget);
  });

  testWidgets('cerrar sesión borra tokens y vuelve a portada', (tester) async {
    final almacen = AlmacenEnMemoria();
    final sesion = Sesion(almacen);
    await sesion.guardar(acceso: 'acceso-sintetico', refresco: 'ref-sintetico');
    final router = GoRouter(
      initialLocation: '/identidad/perfil',
      routes: [
        GoRoute(
          path: '/identidad/perfil',
          builder: (_, _) => const PantallaDePerfil(),
        ),
        GoRoute(
          path: '/portada',
          builder: (_, _) => const Scaffold(body: Text('Portada destino')),
        ),
      ],
    );
    addTearDown(router.dispose);
    await tester.pumpWidget(
      ProviderScope(
        overrides: [almacenSeguroProvider.overrideWithValue(almacen)],
        child: MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
          routerConfig: router,
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text(TextosIdentidad.cerrarSesion));
    await tester.pumpAndSettle();
    expect(router.state.uri.path, '/portada');
    expect(await sesion.tokenDeAcceso(), isNull);
    expect(await sesion.tokenDeRefresco(), isNull);
  });
}
