import 'package:aportaya_movil/pantallas/identidad/pantalla_mfa.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_portada.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';
import 'cargar_fuentes_de_aplicacion.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/';

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  void configurarViewport(WidgetTester tester) {
    tester.view.physicalSize = const Size(360, 740);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 1.3;
    addTearDown(tester.view.reset);
    addTearDown(tester.platformDispatcher.clearAllTestValues);
  }

  void comprobarTemaSinDesborde(WidgetTester tester, Brightness brillo) {
    final contexto = tester.element(find.byType(Scaffold).first);
    expect(Theme.of(contexto).brightness, brillo);
    expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
    expect(tester.takeException(), isNull);
  }

  testWidgets('portada oscura · 360 px · texto 130 %', (tester) async {
    configurarViewport(tester);
    final (:dio, adaptador: _) = dioSimulado();
    await tester.pumpWidget(
      conApp(
        const PantallaDePortada(),
        dio: dio,
        brillo: Brightness.dark,
        movimientoReducido: true,
      ),
    );
    await asentar(tester);

    expect(find.text(TextosIdentidad.bienvenidaTitulo), findsOneWidget);
    comprobarTemaSinDesborde(tester, Brightness.dark);
    final garantiaInferior = tester
        .getBottomRight(find.text(TextosIdentidad.portadaGarantiaTurno))
        .dy;
    final botonesSuperior = tester
        .getTopLeft(find.byKey(const ValueKey('portada-botones')))
        .dy;
    expect(
      garantiaInferior,
      lessThanOrEqualTo(botonesSuperior),
      reason: 'la tercera garantía debe leerse entera antes de las salidas',
    );
    await expectLater(
      find.byType(PantallaDePortada),
      matchesGoldenFile('${_evidencia}h8-portada-oscuro-360-130.png'),
    );
  });

  testWidgets('portada clara · 360 px · texto 130 %', (tester) async {
    configurarViewport(tester);
    final (:dio, adaptador: _) = dioSimulado();
    await tester.pumpWidget(
      conApp(
        const PantallaDePortada(),
        dio: dio,
        brillo: Brightness.light,
        movimientoReducido: true,
      ),
    );
    await asentar(tester);

    expect(find.text(TextosIdentidad.bienvenidaTitulo), findsOneWidget);
    comprobarTemaSinDesborde(tester, Brightness.light);
    final garantiaInferior = tester
        .getBottomRight(find.text(TextosIdentidad.portadaGarantiaTurno))
        .dy;
    final botonesSuperior = tester
        .getTopLeft(find.byKey(const ValueKey('portada-botones')))
        .dy;
    expect(garantiaInferior, lessThanOrEqualTo(botonesSuperior));
    await expectLater(
      find.byType(PantallaDePortada),
      matchesGoldenFile('${_evidencia}h8-portada-claro-360-130.png'),
    );
  });

  testWidgets('MFA oscuro inicial · 360 px · texto 130 %', (tester) async {
    configurarViewport(tester);
    final (:dio, adaptador: _) = dioSimulado();
    await tester.pumpWidget(
      conApp(const PantallaDeMfa(), dio: dio, brillo: Brightness.dark),
    );
    await asentar(tester);

    expect(find.text(TextosIdentidad.tituloMfa), findsOneWidget);
    comprobarTemaSinDesborde(tester, Brightness.dark);
    await expectLater(
      find.byType(PantallaDeMfa),
      matchesGoldenFile('${_evidencia}h8-mfa-oscuro-360-130.png'),
    );
  });

  testWidgets('MFA código rechazado · oscuro · 360 px · texto 130 %', (
    tester,
  ) async {
    configurarViewport(tester);
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onPost(
      '/sesiones',
      (s) => s.reply(422, {
        'codigo': 'AP-CU04-04',
        'mensaje': 'texto sintético del servidor',
        'trazaId': 'traza-sintetica',
      }),
      data: Matchers.any,
    );
    await tester.pumpWidget(
      conApp(const PantallaDeMfa(), dio: dio, brillo: Brightness.dark),
    );
    await asentar(tester);

    await tester.enterText(find.byType(TextField).first, '123456');
    await asentar(tester);

    expect(
      find.text('Ese código no sirve: revisalo o pedí uno nuevo.'),
      findsOneWidget,
    );
    comprobarTemaSinDesborde(tester, Brightness.dark);
    await expectLater(
      find.byType(PantallaDeMfa),
      matchesGoldenFile('${_evidencia}h8-mfa-error-oscuro-360-130.png'),
    );
  });
}
