import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:aportaya_movil/pantallas/pasanaku/textos.dart';
import 'package:aportaya_movil/pantallas/pasanaku/textos_accion_personal.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  const id = '11111111-1111-4111-8111-111111111111';
  final rutas = <(String, String)>[
    ('/pasanaku/grupos/nuevo?organizador=$id', TextosPasanaku.tituloCrearGrupo),
    (
      '/pasanaku/grupos/$id/retiro?participante=$id',
      TextosPasanaku.tituloRetiro,
    ),
    (
      '/pasanaku/turnos/permuta?origen=$id&destino=$id&contraparte=$id',
      TextosPasanaku.tituloPermuta,
    ),
  ];

  for (final (ruta, titulo) in rutas) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      testWidgets('$titulo no acepta identidad del enlace ($brillo)', (
        tester,
      ) async {
        tester.view.physicalSize = const Size(320, 760);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        final simulado = dioSimulado();
        var peticiones = 0;
        simulado.dio.interceptors.add(
          InterceptorsWrapper(
            onRequest: (opciones, manejador) {
              peticiones++;
              manejador.next(opciones);
            },
          ),
        );
        final router = crearEnrutador(inicial: ruta);
        addTearDown(router.dispose);

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
              dioProvider.overrideWithValue(simulado.dio),
            ],
            child: MaterialApp.router(
              theme: temaDesde(Tokens.claro, Brightness.light),
              darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
              themeMode: brillo == Brightness.dark
                  ? ThemeMode.dark
                  : ThemeMode.light,
              builder: (context, child) => MediaQuery(
                data: MediaQuery.of(
                  context,
                ).copyWith(textScaler: const TextScaler.linear(2)),
                child: child!,
              ),
              routerConfig: router,
            ),
          ),
        );
        await asentar(tester);

        expect(find.text(titulo), findsOneWidget);
        expect(
          find.text(TextosAccionPersonal.sinTitular),
          findsOneWidget,
        );
        expect(find.text(TextosPasanaku.irAAyuda), findsOneWidget);
        expect(peticiones, 0);
        expect(tester.takeException(), isNull);

        await tester.ensureVisible(find.text(TextosPasanaku.irAAyuda));
        await tester.pumpAndSettle();
        await tester.tap(find.text(TextosPasanaku.irAAyuda));
        await asentar(tester);
        expect(router.state.uri.path, '/soporte/ayuda');
        expect(peticiones, 0);
        expect(tester.takeException(), isNull);
      });
    }
  }

  testWidgets('crear grupo autogestionado sin query conserva el formulario', (
    tester,
  ) async {
    final router = crearEnrutador(inicial: '/pasanaku/grupos/nuevo');
    addTearDown(router.dispose);
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
          dioProvider.overrideWithValue(dioSimulado().dio),
        ],
        child: MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
          routerConfig: router,
        ),
      ),
    );
    await asentar(tester);

    expect(find.text(TextosPasanaku.tituloCrearGrupo), findsOneWidget);
    expect(find.text(TextosPasanaku.nombreDelGrupo), findsOneWidget);
    expect(find.text(TextosAccionPersonal.sinTitular), findsNothing);
    expect(tester.takeException(), isNull);
  });
}
