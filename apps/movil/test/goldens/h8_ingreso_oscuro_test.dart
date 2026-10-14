import 'package:aportaya_movil/pantallas/identidad/pantalla_sesion.dart';
import 'package:aportaya_movil/dominio/puertos/haptica.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';
import 'cargar_fuentes_de_aplicacion.dart';

class _HapticaSinEfecto implements Haptica {
  @override
  Future<void> seleccion() async {}

  @override
  Future<void> botonPrimario() async {}

  @override
  Future<void> avisoDeCredencialesIncorrectas() async {}

  @override
  Future<void> exito() async {}

  @override
  Future<void> errorQueBloqueaDinero() async {}
}

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  testWidgets('ingreso oscuro · 360 px · texto 130 %', (tester) async {
    tester.view.physicalSize = const Size(360, 740);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 1.3;
    addTearDown(tester.view.reset);
    addTearDown(tester.platformDispatcher.clearAllTestValues);

    final (:dio, adaptador: _) = dioSimulado();
    await tester.pumpWidget(
      conApp(const PantallaDeSesion(), dio: dio, brillo: Brightness.dark),
    );
    await asentar(tester);

    final contexto = tester.element(find.byType(Scaffold).first);
    expect(Theme.of(contexto).brightness, Brightness.dark);
    expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
    expect(find.text('Ingresar'), findsOneWidget);
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(PantallaDeSesion),
      matchesGoldenFile(
        '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
        'evidencia/h8-ingreso-oscuro-360-130.png',
      ),
    );
  });

  testWidgets('ingreso rechaza credenciales · oscuro · 360 px · texto 130 %', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 740);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 1.3;
    tester.platformDispatcher.accessibilityFeaturesTestValue =
        const FakeAccessibilityFeatures(disableAnimations: true);
    addTearDown(tester.view.reset);
    addTearDown(tester.platformDispatcher.clearAllTestValues);
    addTearDown(tester.platformDispatcher.clearAccessibilityFeaturesTestValue);

    final (:dio, :adaptador) = dioSimulado();
    adaptador.onPost(
      '/sesiones',
      (s) => s.reply(422, {
        'codigo': 'AP-CU04-01',
        'mensaje': 'texto sintético del servidor',
        'trazaId': 'traza-sintetica',
      }),
      data: Matchers.any,
    );
    await tester.pumpWidget(
      conApp(
        const PantallaDeSesion(),
        dio: dio,
        brillo: Brightness.dark,
        movimientoReducido: true,
        haptica: _HapticaSinEfecto(),
      ),
    );
    await asentar(tester);

    final campos = find.byType(EditableText);
    await tester.enterText(campos.at(0), '70000001');
    await tester.enterText(campos.at(1), 'Clave-Segura-123');
    await tester.pump();
    await tester.tap(find.text('Ingresar'));
    await asentar(tester);

    expect(
      find.text('El celular o la contraseña no coinciden.'),
      findsOneWidget,
    );
    final contexto = tester.element(find.byType(Scaffold).first);
    expect(Theme.of(contexto).brightness, Brightness.dark);
    expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(PantallaDeSesion),
      matchesGoldenFile(
        '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
        'evidencia/h8-ingreso-error-oscuro-360-130.png',
      ),
    );
  });
}
