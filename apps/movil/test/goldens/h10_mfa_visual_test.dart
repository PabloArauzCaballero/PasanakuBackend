import 'package:aportaya_movil/pantallas/identidad/dominio/estado_sesion.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_mfa.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';
import 'cargar_fuentes_de_aplicacion.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h10-visual/';
const _mfaH8 =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h8-mfa-oscuro-360-130.png';
const _errorH8 =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h8-mfa-error-oscuro-360-130.png';

enum _EstadoVisual { principal, cargando, error }

class _SesionEnviando extends SesionIdentidadNotifier {
  @override
  EstadoSesion build() =>
      const EstadoSesion(paso: PasoSesion.mfa, enviando: true);
}

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  for (final ancho in [360, 412, 600]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      final tema = brillo == Brightness.light ? 'claro' : 'oscuro';
      for (final estado in _EstadoVisual.values) {
        final nombreEstado = switch (estado) {
          _EstadoVisual.principal => 'principal',
          _EstadoVisual.cargando => 'cargando',
          _EstadoVisual.error => 'error',
        };

        testWidgets('MFA · $nombreEstado · $tema · $ancho dp · texto 130 %', (
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

          final (:dio, :adaptador) = dioSimulado();
          if (estado == _EstadoVisual.error) {
            adaptador.onPost(
              '/sesiones',
              (s) => s.reply(422, {
                'codigo': 'AP-CU04-04',
                'mensaje': 'texto sintético del servidor',
                'trazaId': 'traza-sintetica',
              }),
              data: Matchers.any,
            );
          }

          final app = conApp(
            const PantallaDeMfa(),
            dio: dio,
            brillo: brillo,
            movimientoReducido: true,
          );
          await tester.pumpWidget(
            estado == _EstadoVisual.cargando
                ? ProviderScope(
                    overrides: [
                      sesionIdentidadProvider.overrideWith(_SesionEnviando.new),
                    ],
                    child: app,
                  )
                : app,
          );

          if (estado == _EstadoVisual.error) {
            await tester.enterText(find.byType(TextField).first, '123456');
            await asentar(tester);
            expect(
              find.text('Ese código no sirve: revisalo o pedí uno nuevo.'),
              findsOneWidget,
            );
          } else if (estado == _EstadoVisual.cargando) {
            // El botón mantiene el indicador indeterminado de carga; capturamos
            // un frame estable sin esperar que la animación termine.
            await tester.pump();
            expect(
              find.bySemanticsLabel('Confirmar, enviando'),
              findsOneWidget,
            );
            expect(find.byType(CircularProgressIndicator), findsOneWidget);
            expect(
              tester.widget<TextField>(find.byType(TextField).first).enabled,
              isFalse,
            );
          } else {
            await asentar(tester);
            expect(find.byType(TextField), findsNWidgets(6));
          }

          final pantalla = find.byType(PantallaDeMfa);
          final contexto = tester.element(pantalla);
          expect(Theme.of(contexto).brightness, brillo);
          expect(
            MediaQuery.textScalerOf(contexto).scale(10),
            closeTo(13, 0.001),
          );
          expect(tester.takeException(), isNull);

          final ruta = switch ((estado, ancho, brillo)) {
            (_EstadoVisual.principal, 360, Brightness.dark) => _mfaH8,
            (_EstadoVisual.error, 360, Brightness.dark) => _errorH8,
            _ =>
              '$_evidencia'
                  'h10-mfa-$nombreEstado-$tema-$ancho-130.png',
          };
          await expectLater(pantalla, matchesGoldenFile(ruta));
        });
      }
    }
  }
}
