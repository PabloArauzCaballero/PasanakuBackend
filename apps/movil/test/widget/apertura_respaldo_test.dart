import 'package:aportaya_diseno/moviles/apertura_de_marca.dart';
import 'package:aportaya_movil/app.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('la apertura oculta la portada al lector hasta salir', (
    tester,
  ) async {
    final semantica = tester.ensureSemantics();
    await tester.pumpWidget(
      ProviderScope(child: TickerMode(enabled: false, child: AppAportaYa())),
    );
    String arbol() => tester
        .binding
        .renderViews
        .first
        .owner!
        .semanticsOwner!
        .rootSemanticsNode!
        .toStringDeep();
    expect(arbol(), contains('Tocá para saltar la apertura'));
    expect(find.bySemanticsLabel('AportaYa'), findsOneWidget);
    expect(
      arbol(),
      isNot(contains('label: "${TextosIdentidad.portadaCrearCuenta}"')),
      reason: 'la portada tapada no debe entrar al árbol del lector',
    );

    await tester.tap(find.byType(AperturaDeMarca));
    await tester.pump();
    expect(arbol(), isNot(contains('Tocá para saltar la apertura')));
    expect(arbol(), contains('label: "${TextosIdentidad.portadaCrearCuenta}"'));
    semantica.dispose();
  });

  testWidgets('el respaldo solicita un cuadro aunque el ticker esté detenido', (
    tester,
  ) async {
    final binding = tester.binding as AutomatedTestWidgetsFlutterBinding;
    await tester.pumpWidget(
      ProviderScope(child: TickerMode(enabled: false, child: AppAportaYa())),
    );
    for (var i = 0; i < 5 && binding.hasScheduledFrame; i++) {
      await tester.pump();
    }

    expect(find.byType(AperturaDeMarca), findsOneWidget);
    expect(binding.hasScheduledFrame, isFalse);
    await binding.delayed(const Duration(seconds: 2));

    expect(
      binding.hasScheduledFrame,
      isTrue,
      reason: 'el temporizador no puede esperar un cuadro que nadie pidió',
    );
    await tester.pump();
    expect(find.byType(AperturaDeMarca), findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets('tocar salta la apertura y cancela el respaldo', (tester) async {
    final binding = tester.binding as AutomatedTestWidgetsFlutterBinding;
    await tester.pumpWidget(
      ProviderScope(child: TickerMode(enabled: false, child: AppAportaYa())),
    );
    expect(find.byType(AperturaDeMarca), findsOneWidget);

    await tester.tap(find.byType(AperturaDeMarca));
    await tester.pump();
    expect(find.byType(AperturaDeMarca), findsNothing);
    await binding.delayed(const Duration(seconds: 2));
    expect(tester.takeException(), isNull);
  });

  testWidgets('desmontar la app cancela el respaldo pendiente', (tester) async {
    final binding = tester.binding as AutomatedTestWidgetsFlutterBinding;
    await tester.pumpWidget(
      ProviderScope(child: TickerMode(enabled: false, child: AppAportaYa())),
    );
    expect(find.byType(AperturaDeMarca), findsOneWidget);

    await tester.pumpWidget(const SizedBox.shrink());
    for (var i = 0; i < 5 && binding.hasScheduledFrame; i++) {
      await tester.pump();
    }
    expect(binding.hasScheduledFrame, isFalse);
    await binding.delayed(const Duration(seconds: 2));
    expect(tester.takeException(), isNull);
    expect(binding.hasScheduledFrame, isFalse);
  });

  testWidgets('movimiento reducido omite la apertura en el primer cuadro', (
    tester,
  ) async {
    tester.platformDispatcher.accessibilityFeaturesTestValue =
        const FakeAccessibilityFeatures(disableAnimations: true);
    addTearDown(tester.platformDispatcher.clearAccessibilityFeaturesTestValue);

    await tester.pumpWidget(ProviderScope(child: AppAportaYa()));
    expect(find.byType(AperturaDeMarca), findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets('activar movimiento reducido corta una apertura en curso', (
    tester,
  ) async {
    await tester.pumpWidget(ProviderScope(child: AppAportaYa()));
    expect(find.byType(AperturaDeMarca), findsOneWidget);

    tester.platformDispatcher.accessibilityFeaturesTestValue =
        const FakeAccessibilityFeatures(disableAnimations: true);
    addTearDown(tester.platformDispatcher.clearAccessibilityFeaturesTestValue);
    await tester.pump();
    expect(find.byType(AperturaDeMarca), findsNothing);
    expect(tester.takeException(), isNull);
  });
}
