import 'dart:async';

import 'package:aportaya_cliente_aportes/aportaya_cliente_aportes.dart'
    as aportes;
import 'package:aportaya_cliente_garantia/aportaya_cliente_garantia.dart'
    as garantia;
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/pasanaku/dominio/estado_de_mora.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_mi_estado.dart';
import 'package:aportaya_movil/pantallas/pasanaku/textos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'cargar_fuentes_de_aplicacion.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h10-visual/';
const _participante = 'pppppppp-0000-4000-8000-000000000001';
const _usuario = 'uuuuuuuu-0000-4000-8000-000000000001';
const _grupo = 'gggggggg-0000-4000-8000-000000000001';

enum _Fixture { deuda, alDia, cargando, error }

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  for (final ancho in [360, 412, 600]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      // El caso 360 oscuro ya está cubierto por h8_grupos_mi_estado_oscuro_test.dart.
      if (ancho == 360 && brillo == Brightness.dark) continue;

      for (final fixture in _Fixture.values) {
        final tema = brillo == Brightness.light ? 'claro' : 'oscuro';
        final estado = switch (fixture) {
          _Fixture.deuda => 'principal-deuda',
          _Fixture.alDia => 'principal-al-dia',
          _Fixture.cargando => 'cargando',
          _Fixture.error => 'error-parcial',
        };

        testWidgets('mi estado · $estado · $tema · $ancho dp · texto 130 %', (
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

          await tester.pumpWidget(
            ProviderScope(
              overrides: [
                estadoDelParticipanteProvider(_participante).overrideWith((
                  ref,
                ) {
                  return switch (fixture) {
                    _Fixture.cargando =>
                      Completer<aportes.EstadoDelParticipante>().future,
                    _Fixture.error => Future.error(
                      StateError('Fallo sintético de aportes'),
                    ),
                    _Fixture.deuda => Future.value(
                      _estadoDelParticipante(alDia: false),
                    ),
                    _Fixture.alDia => Future.value(
                      _estadoDelParticipante(alDia: true),
                    ),
                  };
                }),
                restriccionVigenteProvider(_usuario).overrideWith((ref) {
                  if (fixture == _Fixture.cargando) {
                    return Completer<garantia.RestriccionVigente>().future;
                  }
                  return Future.value(
                    _restriccion(vigente: fixture == _Fixture.deuda),
                  );
                }),
              ],
              child: MaterialApp(
                theme: temaDesde(Tokens.claro, Brightness.light),
                darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
                themeMode: brillo == Brightness.light
                    ? ThemeMode.light
                    : ThemeMode.dark,
                home: const PantallaMiEstado(
                  participanteId: _participante,
                  usuarioId: _usuario,
                  grupoId: _grupo,
                ),
              ),
            ),
          );

          if (fixture == _Fixture.cargando) {
            await tester.pump();
          } else {
            await tester.pumpAndSettle();
          }

          final contexto = tester.element(find.byType(PantallaMiEstado));
          expect(Theme.of(contexto).brightness, brillo);
          expect(
            MediaQuery.textScalerOf(contexto).scale(10),
            closeTo(13, 0.001),
          );
          switch (fixture) {
            case _Fixture.deuda:
              expect(find.text('Con deuda vigente'), findsOneWidget);
            case _Fixture.alDia:
              expect(find.text('Al día'), findsOneWidget);
            case _Fixture.cargando:
              expect(
                find.bySemanticsLabel(TextosPasanaku.cargando),
                findsNWidgets(2),
              );
            case _Fixture.error:
              expect(
                find.text(
                  'Algo salió mal de nuestro lado. Probá de nuevo en un momento.',
                ),
                findsOneWidget,
              );
              expect(
                find.widgetWithText(FilledButton, 'Volver a intentar'),
                findsOneWidget,
              );
          }
          expect(tester.takeException(), isNull);

          await expectLater(
            find.byType(PantallaMiEstado),
            matchesGoldenFile(
              '$_evidencia'
              'h10-grupos-mi-estado-$estado-$tema-$ancho-130.png',
            ),
          );
        });
      }
    }
  }
}

aportes.EstadoDelParticipante _estadoDelParticipante({required bool alDia}) =>
    aportes.EstadoDelParticipante(
      alDia: alDia,
      totalAportado: alDia ? '2600.00' : '0.00',
      deudaVigente: alDia ? '0.00' : '1240.50',
      porAportar: alDia ? '0.00' : '4.44',
      obligacionesAbiertas: alDia ? 0 : 1,
      moneda: aportes.EstadoDelParticipanteMonedaEnum.BOB,
    );

garantia.RestriccionVigente _restriccion({required bool vigente}) =>
    garantia.RestriccionVigente(
      vigente: vigente,
      montoQueLaLevanta: vigente ? '1240.50' : '0.00',
    );
