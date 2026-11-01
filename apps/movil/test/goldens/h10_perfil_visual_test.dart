import 'package:aportaya_movil/pantallas/identidad/pantalla_perfil.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'cargar_fuentes_de_aplicacion.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h10-visual/';

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  for (final ancho in [360, 412, 600]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      final tema = brillo == Brightness.light ? 'claro' : 'oscuro';
      testWidgets('perfil inicial · $tema · $ancho dp · texto 130 %', (
        tester,
      ) async {
        final alto = switch (ancho) {
          360 => 740,
          412 => 850,
          _ => 960,
        };
        tester.view.physicalSize = Size(ancho.toDouble(), alto.toDouble());
        tester.view.devicePixelRatio = 1;
        tester.platformDispatcher.textScaleFactorTestValue = 1.3;
        tester.platformDispatcher.accessibilityFeaturesTestValue =
            const FakeAccessibilityFeatures(disableAnimations: true);
        addTearDown(tester.view.reset);
        addTearDown(tester.platformDispatcher.clearAllTestValues);
        addTearDown(
          tester.platformDispatcher.clearAccessibilityFeaturesTestValue,
        );

        final (:dio, adaptador: _) = dioSimulado();
        await tester.pumpWidget(
          conApp(
            const PantallaDePerfil(),
            dio: dio,
            brillo: brillo,
            movimientoReducido: true,
          ),
        );
        await asentar(tester);

        final pantalla = find.byType(PantallaDePerfil);
        final contexto = tester.element(pantalla);
        expect(Theme.of(contexto).brightness, brillo);
        expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
        expect(find.text(TextosIdentidad.tituloPerfil), findsOneWidget);
        expect(find.text('Correo'), findsOneWidget);
        // 2026-10-19: el correo no se edita; no hay «Guardar».
        expect(find.text(TextosIdentidad.guardar), findsNothing);
        expect(
          find.text(TextosIdentidad.tituloContrasenaNueva),
          findsOneWidget,
        );
        expect(find.text(TextosIdentidad.verGuiaOtraVez), findsOneWidget);
        expect(find.text('@'), findsNothing);
        expect(tester.takeException(), isNull);

        await expectLater(
          pantalla,
          matchesGoldenFile(
            '$_evidencia'
            'h10-perfil-inicial-$tema-$ancho-130.png',
          ),
        );

        // Cerrar sesión y dar de baja quedan bajo el pliegue (perfil con cabecera de
        // verificación y lista agrupada, ADR-053): se miden después del golden, que es la
        // pantalla como se abre, desplazando HASTA ellos.
        for (final texto in [
          TextosIdentidad.cerrarSesion,
          TextosIdentidad.tituloBaja,
        ]) {
          await tester.dragUntilVisible(
            find.text(texto),
            find.byType(ListView).first,
            const Offset(0, -200),
          );
          expect(find.text(texto), findsOneWidget);
        }
        expect(tester.takeException(), isNull);
      });
    }
  }

  testWidgets('perfil: fila de contraseña mientras se hunde al tocar', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(412, 850);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 1.3;
    addTearDown(tester.view.reset);
    addTearDown(tester.platformDispatcher.clearAllTestValues);

    final (:dio, adaptador: _) = dioSimulado();
    await tester.pumpWidget(
      conApp(
        const PantallaDePerfil(),
        dio: dio,
        brillo: Brightness.light,
        movimientoReducido: false,
      ),
    );
    await asentar(tester);

    final gesto = await tester.startGesture(
      tester.getCenter(find.text(TextosIdentidad.tituloContrasenaNueva)),
    );
    await tester.pump();
    await tester.pump(Movimiento.micro);
    await expectLater(
      find.byType(PantallaDePerfil),
      matchesGoldenFile(
        '$_evidencia'
        'h10-perfil-fila-contrasena-presionada-claro-412-130.png',
      ),
    );
    await gesto.cancel();
    await tester.pumpAndSettle();
  });
}
