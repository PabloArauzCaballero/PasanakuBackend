import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:aportaya_movil/pantallas/pasanaku/textos.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  const usuario = '11111111-1111-4111-8111-111111111111';
  const participante = '22222222-2222-4222-8222-222222222222';

  for (final (ruta, enlace, mensaje) in [
    (
      '/pasanaku/mi-estado',
      '/pasanaku/mi-estado?participante=$participante&usuario=$usuario',
      TextosPasanaku.faltaIdentificarParticipacion,
    ),
    (
      '/pasanaku/mi-puntaje',
      '/pasanaku/mi-puntaje?usuario=$usuario',
      TextosPasanaku.faltaIdentificarPuntaje,
    ),
  ]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      testWidgets('el enlace $ruta no usa IDs ajenos ($brillo)', (
        tester,
      ) async {
        tester.view.physicalSize = const Size(360, 760);
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
        final router = crearEnrutador(inicial: enlace);
        addTearDown(router.dispose);

        await tester.pumpWidget(
          ProviderScope(
            retry: (retryCount, error) => null,
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

        expect(router.state.uri.toString(), ruta);
        expect(find.text(mensaje), findsOneWidget);
        expect(peticiones, 0);
        expect(tester.takeException(), isNull);

        await tester.ensureVisible(find.text(TextosPasanaku.irAAyuda));
        await tester.pumpAndSettle();
        await tester.tap(find.text(TextosPasanaku.irAAyuda));
        await asentar(tester);
        expect(router.state.uri.path, '/soporte/ayuda');
        expect(tester.takeException(), isNull);
      });
    }
  }
}
