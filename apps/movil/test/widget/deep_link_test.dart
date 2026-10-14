import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/moviles/barra_pestanas.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:aportaya_movil/navegacion/shell.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import '../comun.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';

/// El gate de F2: «un deep link abre la pantalla correcta con la app cerrada y con
/// la app abierta» (`planes/12`, gate de salida F2). Con la app cerrada, `go_router`
/// recibe la URI del esquema propio como **ubicación inicial** (lo que hace
/// `MainActivity` al arrancar desde un intent `VIEW`); con la app abierta, llega
/// por `redirect` en caliente. Los dos casos pasan por `rutaInternaDesde`.
void main() {
  Widget appCon(GoRouter router) => ProviderScope(
    overrides: [almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria())],
    child: MaterialApp.router(
      theme: temaDesde(Tokens.claro, Brightness.light),
      routerConfig: router,
    ),
  );

  testWidgets('app cerrada: aportaya://billetera abre /billetera/inicio', (
    tester,
  ) async {
    final router = crearEnrutador(
      inicial: 'aportaya://billetera',
      sesion: await sesionActivaDePrueba(),
    );
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();

    expect(router.state.uri.toString(), '/billetera/inicio');
  });

  testWidgets('app abierta: navegar a un deep link también redirige', (
    tester,
  ) async {
    final router = crearEnrutador(
      inicial: '/billetera/inicio',
      sesion: await sesionActivaDePrueba(),
    );
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();

    router.go('aportaya://billetera');
    await tester.pumpAndSettle();

    expect(router.state.uri.toString(), '/billetera/inicio');
  });

  testWidgets('una invitación externa abre su destino sin sesión ni pestañas', (
    tester,
  ) async {
    const codigo =
        '12345678-1234-4234-8234-123456789abc.'
        '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';
    final router = crearEnrutador(
      inicial: 'aportaya://unirse/$codigo',
      sesion: await sesionAnonimaDePrueba(),
    );
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();

    expect(router.state.uri.toString(), '/pasanaku/unirse/$codigo');
    expect(find.text('Invitación al grupo'), findsOneWidget);
    expect(find.text('Ingresar'), findsOneWidget);
    expect(find.byType(BarraPestanas), findsNothing);
  });

  testWidgets('MFA se muestra fuera del shell de navegación', (tester) async {
    final router = crearEnrutador(
      inicial: '/identidad/mfa',
      sesion: await sesionAnonimaDePrueba(),
    );
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();

    expect(find.byType(ShellPrincipal), findsNothing);
    expect(find.byType(BarraPestanas), findsNothing);
    expect(find.text(TextosIdentidad.mfaIndicacion), findsOneWidget);
    expect(find.text(TextosIdentidad.mfaSinCodigoAyuda), findsOneWidget);
  });

  testWidgets('las pautas abren directamente sin sesión ni shell', (
    tester,
  ) async {
    final router = crearEnrutador(
      inicial: '/registro/pautas',
      sesion: await sesionAnonimaDePrueba(),
    );
    addTearDown(router.dispose);
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();

    expect(router.state.uri.path, '/registro/pautas');
    expect(find.text('Pautas para tus fotos'), findsOneWidget);
    expect(find.text('Pautas para el documento de identidad'), findsOneWidget);
    expect(find.byType(ShellPrincipal), findsNothing);
    expect(find.byType(BarraPestanas), findsNothing);
  });
}
