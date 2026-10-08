import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_bienvenida.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_bienvenida.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

void main() {
  testWidgets('bienvenida sin sesión solo invita a ingresar', (tester) async {
    tester.view.physicalSize = const Size(320, 760);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final router = GoRouter(
      initialLocation: '/identidad/bienvenida',
      routes: [
        GoRoute(
          path: '/identidad/bienvenida',
          builder: (_, _) => const PantallaDeBienvenida(),
        ),
        GoRoute(
          path: '/ingreso',
          builder: (_, _) => const Scaffold(body: Text('Ingreso destino')),
        ),
      ],
    );
    addTearDown(router.dispose);
    await tester.pumpWidget(
      MaterialApp.router(
        theme: temaDesde(Tokens.claro, Brightness.light),
        builder: (context, child) => MediaQuery(
          data: MediaQuery.of(
            context,
          ).copyWith(textScaler: const TextScaler.linear(2)),
          child: child!,
        ),
        routerConfig: router,
      ),
    );
    await tester.pumpAndSettle();

    expect(find.textContaining('nueva cuenta'), findsNothing);
    expect(find.text('Ir a tu billetera'), findsNothing);
    expect(find.text('Completar tu perfil'), findsNothing);
    expect(find.text(TextosBienvenida.aviso), findsOneWidget);
    await tester.tap(find.text(TextosBienvenida.irAIngresar));
    await tester.pumpAndSettle();
    expect(router.state.uri.path, '/ingreso');
    expect(tester.takeException(), isNull);
  });
}
