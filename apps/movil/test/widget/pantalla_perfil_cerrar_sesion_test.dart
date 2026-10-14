import 'dart:async';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/atomos/hundido_al_tocar.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_sesion.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_perfil.dart';
import 'package:aportaya_movil/proveedores/tutoriales.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/semantics.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

class _SesionIdentidadFalsa extends SesionIdentidadNotifier {
  final finDeCierre = Completer<void>();
  int cantidadDeCierres = 0;

  @override
  EstadoSesion build() => const EstadoSesion();

  @override
  Future<void> cerrarSesion() async {
    cantidadDeCierres++;
    await finDeCierre.future;
  }
}

void main() {
  testWidgets(
    'acciones de perfil son filas accesibles con respuesta al toque',
    (tester) async {
      var guiaReiniciada = false;
      final router = GoRouter(
        initialLocation: '/perfil',
        routes: [
          GoRoute(path: '/perfil', builder: (_, _) => const PantallaDePerfil()),
          GoRoute(
            path: '/identidad/contrasena',
            builder: (_, _) =>
                const Scaffold(body: Text('Cambio de contraseña')),
          ),
        ],
      );
      addTearDown(router.dispose);

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            verGuiaDeInicioOtraVezProvider.overrideWithValue(() async {
              guiaReiniciada = true;
              return true;
            }),
          ],
          child: MaterialApp.router(
            theme: temaDesde(Tokens.claro, Brightness.light),
            routerConfig: router,
          ),
        ),
      );

      const filaContrasena = ValueKey('perfil-fila-contrasena');
      const filaGuia = ValueKey('perfil-fila-guia');
      for (final key in [filaContrasena, filaGuia]) {
        final fila = find.byKey(key);
        expect(fila, findsOneWidget);
        expect(
          tester.getSize(fila).height,
          greaterThanOrEqualTo(Tactil.minimo),
        );
        expect(
          find.descendant(of: fila, matching: find.byType(HundidoAlTocar)),
          findsOneWidget,
        );
        final semantics = tester.getSemantics(fila).getSemanticsData();
        expect(semantics.label, isNotEmpty);
        expect(semantics.hint, isNotEmpty);
        expect(semantics.flagsCollection.isButton, isTrue);
        expect(semantics.hasAction(SemanticsAction.tap), isTrue);
      }

      final gesto = await tester.startGesture(
        tester.getCenter(find.text('Cambiar contraseña')),
      );
      await tester.pump();
      await tester.pump(Movimiento.micro);
      final transformacion = find
          .ancestor(
            of: find.text('Cambiar contraseña'),
            matching: find.byType(Transform),
          )
          .first;
      expect(
        tester.widget<Transform>(transformacion).transform.storage[0],
        closeTo(0.97, 0.001),
      );
      await gesto.cancel();
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(filaContrasena));
      await tester.pumpAndSettle();
      expect(find.text('Cambio de contraseña'), findsOneWidget);
      router.pop();
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(filaGuia));
      await tester.pump();
      expect(guiaReiniciada, isTrue);
    },
  );

  testWidgets('muestra el cierre en curso y no duplica el cierre', (
    tester,
  ) async {
    final sesion = _SesionIdentidadFalsa();
    final router = GoRouter(
      initialLocation: '/perfil',
      routes: [
        GoRoute(path: '/perfil', builder: (_, _) => const PantallaDePerfil()),
        GoRoute(
          path: '/portada',
          builder: (_, _) => const Scaffold(body: Text('Portada')),
        ),
      ],
    );
    addTearDown(router.dispose);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [sesionIdentidadProvider.overrideWith(() => sesion)],
        child: MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
          routerConfig: router,
        ),
      ),
    );

    await tester.tap(find.text('Cerrar sesión'));
    await tester.pump();

    expect(find.text('Cerrando sesión…'), findsOneWidget);
    expect(sesion.cantidadDeCierres, 1);

    await tester.tap(find.text('Cerrando sesión…'));
    await tester.pump();
    expect(sesion.cantidadDeCierres, 1);

    sesion.finDeCierre.complete();
    await tester.pump();
    expect(find.byType(PantallaDePerfil), findsOneWidget);
  });
}
