import 'package:aportaya_diseno/atomos/campo_o_t_p.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_sesion.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_mfa.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_mfa.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

class _MfaPendiente extends SesionIdentidadNotifier {
  @override
  EstadoSesion build() => const EstadoSesion(
    paso: PasoSesion.mfa,
    telefono: '+59170000000',
    contrasena: 'Prueba123',
  );
}

class _MfaEnviando extends SesionIdentidadNotifier {
  @override
  EstadoSesion build() =>
      const EstadoSesion(paso: PasoSesion.mfa, enviando: true);
}

class _SesionAbierta extends SesionIdentidadNotifier {
  @override
  EstadoSesion build() => const EstadoSesion(paso: PasoSesion.autenticada);
}

void main() {
  testWidgets('un enlace directo a MFA sin desafío no solicita un código', (
    tester,
  ) async {
    final router = GoRouter(
      initialLocation: '/identidad/mfa',
      routes: [
        GoRoute(
          path: '/identidad/mfa',
          builder: (_, _) => const PantallaDeMfa(),
        ),
        GoRoute(
          path: '/ingreso',
          builder: (_, _) => const Scaffold(body: Text('Ingreso destino')),
        ),
      ],
    );
    addTearDown(router.dispose);
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
          routerConfig: router,
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(CampoOTP), findsNothing);
    expect(find.text(TextosMfa.sinDesafio), findsOneWidget);
    await tester.tap(find.text(TextosMfa.volverAIngresar));
    await tester.pumpAndSettle();
    expect(router.state.uri.path, '/ingreso');
  });

  testWidgets(
    'MFA con sesión abierta ofrece ir al inicio, no volver a ingresar',
    (tester) async {
      final router = GoRouter(
        initialLocation: '/identidad/mfa',
        routes: [
          GoRoute(
            path: '/identidad/mfa',
            builder: (_, _) => const PantallaDeMfa(),
          ),
          GoRoute(
            path: '/billetera/inicio',
            builder: (_, _) => const Scaffold(body: Text('Inicio destino')),
          ),
        ],
      );
      addTearDown(router.dispose);
      await tester.pumpWidget(
        ProviderScope(
          overrides: [sesionIdentidadProvider.overrideWith(_SesionAbierta.new)],
          child: MaterialApp.router(
            theme: temaDesde(Tokens.claro, Brightness.light),
            routerConfig: router,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byType(CampoOTP), findsNothing);
      expect(find.text(TextosMfa.yaAutenticadaDetalle), findsOneWidget);
      await tester.tap(find.text(TextosMfa.irAlInicio));
      await tester.pumpAndSettle();
      expect(router.state.uri.path, '/billetera/inicio');
    },
  );

  testWidgets(
    'MFA pendiente permite alcanzar el código con teclado y texto 200 %',
    (tester) async {
      tester.view.physicalSize = const Size(320, 480);
      tester.view.devicePixelRatio = 1;
      tester.view.viewInsets = const FakeViewPadding(bottom: 300);
      addTearDown(tester.view.reset);
      await tester.pumpWidget(
        ProviderScope(
          overrides: [sesionIdentidadProvider.overrideWith(_MfaPendiente.new)],
          child: MaterialApp(
            theme: temaDesde(Tokens.claro, Brightness.light),
            builder: (context, child) => MediaQuery(
              data: MediaQuery.of(
                context,
              ).copyWith(textScaler: const TextScaler.linear(2)),
              child: child!,
            ),
            home: const PantallaDeMfa(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byType(CampoOTP), findsOneWidget);
      expect(find.byType(ListView), findsOneWidget);
      await tester.ensureVisible(find.byType(CampoOTP));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('MFA indica el envío y bloquea nuevas entradas mientras espera', (
    tester,
  ) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [sesionIdentidadProvider.overrideWith(_MfaEnviando.new)],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const PantallaDeMfa(),
        ),
      ),
    );
    await tester.pump();

    expect(find.byType(LinearProgressIndicator), findsOneWidget);
    final bloqueo = tester.widget<IgnorePointer>(
      find
          .ancestor(
            of: find.byType(CampoOTP),
            matching: find.byType(IgnorePointer),
          )
          .first,
    );
    expect(bloqueo.ignoring, isTrue);
  });
}
