import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

/// El gate de F2: «un deep link abre la pantalla correcta con la app cerrada y con
/// la app abierta» (`planes/12`, gate de salida F2). Con la app cerrada, `go_router`
/// recibe la URI del esquema propio como **ubicación inicial** (lo que hace
/// `MainActivity` al arrancar desde un intent `VIEW`); con la app abierta, llega
/// por `redirect` en caliente. Los dos casos pasan por `rutaInternaDesde`.
void main() {
  Widget appCon(GoRouter router) => ProviderScope(
    child: MaterialApp.router(
      theme: temaDesde(Tokens.claro, Brightness.light),
      routerConfig: router,
    ),
  );

  testWidgets('app cerrada: aportaya://billetera abre /billetera/inicio', (
    tester,
  ) async {
    final router = crearEnrutador(inicial: 'aportaya://billetera');
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();

    expect(router.state.uri.toString(), '/billetera/inicio');
  });

  testWidgets('app abierta: navegar a un deep link también redirige', (
    tester,
  ) async {
    final router = crearEnrutador(inicial: '/billetera/inicio');
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();

    router.go('aportaya://billetera');
    await tester.pumpAndSettle();

    expect(router.state.uri.toString(), '/billetera/inicio');
  });
}
