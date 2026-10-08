import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/moviles/barra_pestanas.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
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

  testWidgets('una invitación externa abre su destino sin sesión ni pestañas', (
    tester,
  ) async {
    const codigo =
        '12345678-1234-4234-8234-123456789abc.'
        '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';
    final router = crearEnrutador(inicial: 'aportaya://unirse/$codigo');
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();

    expect(router.state.uri.toString(), '/pasanaku/unirse/$codigo');
    expect(find.text('Invitación al grupo'), findsOneWidget);
    expect(find.text('Ingresar'), findsOneWidget);
    expect(find.byType(BarraPestanas), findsNothing);
  });

  testWidgets(
    'enlace malformado no traslada datos a la ruta ni consulta grupo',
    (tester) async {
      final router = crearEnrutador(
        inicial: 'aportaya://unirse/ABC%3Fdocumento%3DCI_FICTICIA',
      );
      addTearDown(router.dispose);
      await tester.pumpWidget(appCon(router));
      await tester.pumpAndSettle();

      expect(router.state.uri.toString(), '/pasanaku/unirse/invalida');
      expect(
        find.textContaining('El enlace de invitación no es válido.'),
        findsOneWidget,
      );
      expect(find.textContaining('Pedí uno nuevo'), findsOneWidget);
      expect(find.text('Reintentar'), findsNothing);
      expect(find.text('Ir a portada'), findsOneWidget);
      expect(find.textContaining('CI_FICTICIA'), findsNothing);
      await tester.tap(find.text('Ir a portada'));
      await tester.pumpAndSettle();
      expect(router.state.uri.toString(), '/portada');
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('una ruta desconocida no copia su query al error', (
    tester,
  ) async {
    const ubicacion = '/ruta-inexistente?documento=CI_FICTICIA&monto=125.00';
    final router = crearEnrutador(inicial: ubicacion);
    addTearDown(router.dispose);
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();

    final error = tester.widget<EstadoError>(find.byType(EstadoError)).error;
    expect(error.toString(), contains('Ruta no encontrada'));
    expect(error.toString(), isNot(contains('CI_FICTICIA')));
    expect(error.toString(), isNot(contains('125.00')));
    expect(tester.takeException(), isNull);
  });
}
