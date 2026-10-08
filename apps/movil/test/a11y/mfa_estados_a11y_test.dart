import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_sesion.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_mfa.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

class _MfaPendiente extends SesionIdentidadNotifier {
  @override
  EstadoSesion build() => const EstadoSesion(paso: PasoSesion.mfa);
}

class _MfaConError extends SesionIdentidadNotifier {
  @override
  EstadoSesion build() => const EstadoSesion(
    paso: PasoSesion.mfa,
    error: 'El código no coincide. Revisalo e intentá de nuevo.',
  );
}

void main() {
  for (final pendiente in [false, true]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      testWidgets(
        'MFA ${pendiente ? 'pendiente' : 'sin desafío'} · ${brillo.name} cumple a11y',
        (tester) async {
          tester.view.physicalSize = const Size(360, 760);
          tester.view.devicePixelRatio = 1;
          addTearDown(tester.view.reset);
          await tester.pumpWidget(
            ProviderScope(
              overrides: pendiente
                  ? [sesionIdentidadProvider.overrideWith(_MfaPendiente.new)]
                  : [],
              child: MaterialApp(
                theme: temaDesde(Tokens.claro, Brightness.light),
                darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
                themeMode: brillo == Brightness.dark
                    ? ThemeMode.dark
                    : ThemeMode.light,
                home: const PantallaDeMfa(),
              ),
            ),
          );
          await tester.pumpAndSettle();

          await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
          await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
          await expectLater(tester, meetsGuideline(textContrastGuideline));
        },
      );
    }
  }

  testWidgets('MFA con error al 200 % conserva contraste y objetivos', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(320, 760);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      ProviderScope(
        overrides: [sesionIdentidadProvider.overrideWith(_MfaConError.new)],
        child: MaterialApp(
          theme: temaDesde(Tokens.oscuro, Brightness.dark),
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

    await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
    await expectLater(tester, meetsGuideline(textContrastGuideline));
    expect(tester.takeException(), isNull);
  });
}
