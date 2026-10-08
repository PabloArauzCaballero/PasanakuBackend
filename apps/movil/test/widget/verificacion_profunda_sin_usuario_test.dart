import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_perfil.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_verificacion_profunda.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_verificacion_profunda.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

void main() {
  testWidgets('sin titular verificado no se pide ni simula declaración PEP', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(320, 760);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final router = GoRouter(
      initialLocation: '/identidad/verificacion-profunda',
      routes: [
        GoRoute(
          path: '/identidad/verificacion-profunda',
          builder: (_, _) => const PantallaDeVerificacionProfunda(),
        ),
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
        child: MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
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
    await tester.pumpAndSettle();

    expect(find.text(TextosVerificacionProfunda.aviso), findsOneWidget);
    expect(find.byType(Campo), findsNothing);
    expect(find.byType(SegmentedButton<bool>), findsNothing);
    expect(find.text('Continuar'), findsNothing);
    await tester.tap(find.text('Volver al perfil'));
    await tester.pumpAndSettle();
    expect(router.state.uri.path, '/identidad/perfil');
  });
}
