import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import '../comun.dart';

void main() {
  Future<GoRouter> abrir(
    WidgetTester tester, {
    bool reducirMovimiento = false,
  }) async {
    tester.view.physicalSize = const Size(412, 915);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    tester.platformDispatcher.accessibilityFeaturesTestValue =
        FakeAccessibilityFeatures(disableAnimations: reducirMovimiento);
    addTearDown(
      () => tester.platformDispatcher.clearAccessibilityFeaturesTestValue(),
    );
    final router = crearEnrutador(sesion: await sesionAnonimaDePrueba());
    await tester.pumpWidget(
      MaterialApp.router(
        theme: temaDesde(Tokens.claro, Brightness.light),
        routerConfig: router,
      ),
    );
    router.go('/tour');
    await tester.pumpAndSettle();
    return router;
  }

  Future<void> llegarALaUltima(WidgetTester tester) async {
    for (var i = 0; i < 3; i++) {
      await tester.tap(find.byType(Boton));
      await tester.pump();
      await tester.pumpAndSettle();
    }
  }

  testWidgets('la última lámina ofrece solo «Empezar»', (tester) async {
    await abrir(tester);
    final anchoAntes = tester.getSize(find.byType(Boton)).width;
    await llegarALaUltima(tester);

    expect(find.text(TextosIdentidad.portadaYaTengoCuenta), findsNothing);
    expect(find.byType(Boton), findsOneWidget);
    expect(find.text(TextosIdentidad.tourEmpezar), findsOneWidget);
    expect(find.byIcon(Icons.arrow_forward), findsOneWidget);
    expect(tester.getSize(find.byType(Boton)).width, anchoAntes);
    expect(
      tester.widget<AnimatedOpacity>(find.byType(AnimatedOpacity).last).opacity,
      0,
    );
    expect(
      tester.widget<AnimatedSwitcher>(find.byType(AnimatedSwitcher)).duration,
      Movimiento.entrada,
    );
  });

  testWidgets('el cambio a «Empezar» respeta reducir movimiento', (
    tester,
  ) async {
    await abrir(tester, reducirMovimiento: true);
    // El sistema puede pedir menos animación; el contenido de la acción final
    // debe aparecer de inmediato y seguir siendo operable.
    await llegarALaUltima(tester);

    expect(find.text(TextosIdentidad.tourEmpezar), findsOneWidget);
    expect(find.byType(Boton), findsOneWidget);
    expect(
      tester.widget<AnimatedSwitcher>(find.byType(AnimatedSwitcher)).duration,
      Duration.zero,
    );
  });
}
