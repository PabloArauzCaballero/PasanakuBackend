import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/puertos/biometria.dart';
import 'package:aportaya_movil/dominio/vigilante_de_sesion.dart';
import 'package:aportaya_movil/pantallas/identidad/capa_de_bloqueo.dart';
import 'package:aportaya_movil/proveedores/biometria.dart';
import 'package:aportaya_movil/proveedores/bloqueo.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'cargar_fuentes_de_aplicacion.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h10-visual/';

class _BiometriaSinConfirmar implements Biometria {
  @override
  Future<bool> disponible() async => true;

  @override
  Future<bool> confirmar(String motivo) async => false;
}

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  for (final ancho in [360, 412, 600]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      final tema = brillo == Brightness.light ? 'claro' : 'oscuro';
      testWidgets('candado inicial · $tema · $ancho dp · texto 130 %', (
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

        final control = ControlDeBloqueo(esWeb: false)
          ..bloquear(MotivoDeBloqueo.arranque);
        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              bloqueoProvider.overrideWith((_) => control),
              biometriaProvider.overrideWithValue(_BiometriaSinConfirmar()),
            ],
            child: MaterialApp(
              theme: temaDesde(Tokens.claro, Brightness.light),
              darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
              themeMode: brillo == Brightness.light
                  ? ThemeMode.light
                  : ThemeMode.dark,
              home: CapaDeBloqueo(
                hijo: const Scaffold(body: Text('Contenido privado')),
                onIngresarDeNuevo: () {},
              ),
            ),
          ),
        );
        await tester.pumpAndSettle();

        final capa = find.byType(CapaDeBloqueo);
        final contexto = tester.element(capa);
        expect(Theme.of(contexto).brightness, brillo);
        expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
        expect(find.text('AportaYa está bloqueada'), findsOneWidget);
        expect(
          find.text('Para ver tu plata, confirmá que sos vos.'),
          findsOneWidget,
        );
        expect(find.text('Desbloquear con biometría'), findsOneWidget);
        expect(find.text('Ingresar de nuevo'), findsOneWidget);
        expect(tester.takeException(), isNull);

        await expectLater(
          capa,
          matchesGoldenFile(
            '${_evidencia}h10-candado-inicial-$tema-$ancho-130.png',
          ),
        );
      });
    }
  }
}
