import 'package:aportaya_movil/dominio/puertos/haptica.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_sesion.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';
import 'cargar_fuentes_de_aplicacion.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h10-visual/';
const _ingresoH8 =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h8-ingreso-oscuro-360-130.png';
const _errorH8 =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h8-ingreso-error-oscuro-360-130.png';

enum _EstadoVisual { principal, cargando, error }

class _SesionEnviando extends SesionIdentidadNotifier {
  @override
  EstadoSesion build() => const EstadoSesion(enviando: true);
}

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

  for (final ancho in [360, 412, 600]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      final tema = brillo == Brightness.light ? 'claro' : 'oscuro';
      for (final estado in _EstadoVisual.values) {
        final nombreEstado = switch (estado) {
          _EstadoVisual.principal => 'principal',
          _EstadoVisual.cargando => 'cargando',
          _EstadoVisual.error => 'error',
        };

        testWidgets(
          'ingreso · $nombreEstado · $tema · $ancho dp · texto 130 %',
          (tester) async {
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
                  'codigo': 'AP-CU04-01',
                  'mensaje': 'texto sintético del servidor',
                  'trazaId': 'traza-sintetica',
                }),
                data: Matchers.any,
              );
            }

            final app = conApp(
              const PantallaDeSesion(),
              dio: dio,
              brillo: brillo,
              movimientoReducido: true,
              haptica: _HapticaSinEfecto(),
            );
            await tester.pumpWidget(
              estado == _EstadoVisual.cargando
                  ? ProviderScope(
                      overrides: [
                        sesionIdentidadProvider.overrideWith(
                          _SesionEnviando.new,
                        ),
                      ],
                      child: app,
                    )
                  : app,
            );

            if (estado == _EstadoVisual.error) {
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
            } else if (estado == _EstadoVisual.cargando) {
              // El indicador indeterminado anima de forma continua: asentarlo
              // intentaría detener un ticker que, correctamente, no termina.
              await tester.pump();
            } else {
              await asentar(tester);
            }

            final pantalla = find.byType(PantallaDeSesion);
            final contexto = tester.element(pantalla);
            expect(Theme.of(contexto).brightness, brillo);
            expect(
              MediaQuery.textScalerOf(contexto).scale(10),
              closeTo(13, 0.001),
            );
            switch (estado) {
              case _EstadoVisual.principal:
                expect(find.text('Ingresar'), findsOneWidget);
                expect(find.byType(CircularProgressIndicator), findsNothing);
              case _EstadoVisual.cargando:
                expect(
                  find.bySemanticsLabel('Ingresar, enviando'),
                  findsOneWidget,
                );
                expect(find.byType(CircularProgressIndicator), findsOneWidget);
              case _EstadoVisual.error:
                expect(find.text('Ingresar'), findsOneWidget);
                expect(find.text('texto sintético del servidor'), findsNothing);
            }
            final alta = find.text('Crear mi cuenta');
            expect(alta, findsOneWidget);
            expect(
              tester.getRect(alta).bottom,
              lessThanOrEqualTo(alto.toDouble()),
              reason: 'la salida al alta debe quedar dentro del viewport',
            );
            expect(tester.takeException(), isNull);

            final ruta = switch ((estado, ancho, brillo)) {
              (_EstadoVisual.principal, 360, Brightness.dark) => _ingresoH8,
              (_EstadoVisual.error, 360, Brightness.dark) => _errorH8,
              _ =>
                '$_evidencia'
                    'h10-ingreso-$nombreEstado-$tema-$ancho-130.png',
            };
            await expectLater(pantalla, matchesGoldenFile(ruta));
          },
        );
      }
    }
  }
}
