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
import 'package:aportaya_movil/pantallas/pasanaku/textos_invitacion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'cargar_fuentes_de_aplicacion.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/';
const _participante = 'pppppppp-0000-4000-8000-000000000001';
const _usuario = 'uuuuuuuu-0000-4000-8000-000000000001';
const _grupo = 'gggggggg-0000-4000-8000-000000000001';

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  testWidgets('mi estado con deuda · oscuro · 360 px · texto 130 %', (
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

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          estadoDelParticipanteProvider(_participante).overrideWith(
            (ref) async => aportes.EstadoDelParticipante(
              alDia: false,
              totalAportado: '0.00',
              deudaVigente: '1240.50',
              porAportar: '4.44',
              obligacionesAbiertas: 1,
              moneda: aportes.EstadoDelParticipanteMonedaEnum.BOB,
            ),
          ),
          restriccionVigenteProvider(_usuario).overrideWith(
            (ref) async => garantia.RestriccionVigente(
              vigente: true,
              montoQueLaLevanta: '1240.50',
            ),
          ),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
          themeMode: ThemeMode.dark,
          home: const PantallaMiEstado(
            participanteId: _participante,
            usuarioId: _usuario,
            grupoId: _grupo,
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    final contexto = tester.element(find.byType(PantallaMiEstado));
    expect(Theme.of(contexto).brightness, Brightness.dark);
    expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
    expect(find.text('Con deuda vigente'), findsOneWidget);
    // Cada hecho con su nombre a la vista (ADR-053): la deuda, lo que falta aportar, las
    // obligaciones abiertas y el monto que levanta la restricción.
    expect(find.text(TextosPasanaku.etiquetaDeudaVigente), findsOneWidget);
    expect(find.text('Bs 1.240,50'), findsOneWidget);
    expect(find.text(TextosPasanaku.etiquetaPorAportar), findsOneWidget);
    expect(find.text('Bs 4,44'), findsOneWidget);
    expect(
      find.text(TextosPasanaku.etiquetaObligacionesAbiertas),
      findsOneWidget,
    );
    expect(find.text('1'), findsOneWidget);
    expect(find.text(TextosPasanaku.tituloRestriccion), findsOneWidget);
    expect(
      find.text(TextosPasanaku.levantaRestriccion('Bs\u00a01.240,50')),
      findsOneWidget,
    );
    expect(find.text(TextosInvitacion.accionInvitar), findsOneWidget);
    expect(find.text(TextosInvitacion.accionCrearGrupo), findsOneWidget);
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(PantallaMiEstado),
      matchesGoldenFile(
        '${_evidencia}h8-grupos-mi-estado-deuda-oscuro-360-130-inicio.png',
      ),
    );

    final crearGrupo = find.text(TextosInvitacion.accionCrearGrupo);
    await Scrollable.ensureVisible(
      tester.element(crearGrupo),
      alignment: 1,
      duration: Duration.zero,
    );
    await tester.pumpAndSettle();
    for (final texto in [
      TextosInvitacion.accionInvitar,
      TextosInvitacion.accionCrearGrupo,
    ]) {
      final rect = tester.getRect(find.text(texto));
      expect(rect.top, greaterThanOrEqualTo(0), reason: '$texto queda arriba');
      expect(
        rect.bottom,
        lessThanOrEqualTo(tester.view.physicalSize.height),
        reason: '$texto queda dentro de la vista',
      );
    }
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(PantallaMiEstado),
      matchesGoldenFile(
        '${_evidencia}h8-grupos-mi-estado-deuda-oscuro-360-130-acciones.png',
      ),
    );
  });

  testWidgets('mi estado al día · oscuro · 360 px · texto 130 %', (
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

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          estadoDelParticipanteProvider(_participante).overrideWith(
            (ref) async => aportes.EstadoDelParticipante(
              alDia: true,
              totalAportado: '2600.00',
              deudaVigente: '0.00',
              porAportar: '0.00',
              obligacionesAbiertas: 0,
              moneda: aportes.EstadoDelParticipanteMonedaEnum.BOB,
            ),
          ),
          restriccionVigenteProvider(_usuario).overrideWith(
            (ref) async => garantia.RestriccionVigente(
              vigente: false,
              montoQueLaLevanta: '0.00',
            ),
          ),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
          themeMode: ThemeMode.dark,
          home: const PantallaMiEstado(
            participanteId: _participante,
            usuarioId: _usuario,
            grupoId: _grupo,
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    final contexto = tester.element(find.byType(PantallaMiEstado));
    expect(Theme.of(contexto).brightness, Brightness.dark);
    expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
    expect(find.text('Al día'), findsOneWidget);
    expect(find.text(TextosPasanaku.sinRestriccion), findsOneWidget);
    expect(find.text(TextosPasanaku.alDiaTitulo), findsOneWidget);
    expect(
      find.text(TextosPasanaku.etiquetaObligacionesAbiertas),
      findsOneWidget,
    );
    expect(find.text('0'), findsOneWidget);
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(PantallaMiEstado),
      matchesGoldenFile(
        '${_evidencia}h8-grupos-mi-estado-al-dia-oscuro-360-130.png',
      ),
    );
  });

  testWidgets('mi estado cargando · oscuro · 360 px · texto 130 %', (
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

    final estadoPendiente = Completer<aportes.EstadoDelParticipante>();
    final restriccionPendiente = Completer<garantia.RestriccionVigente>();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          estadoDelParticipanteProvider(
            _participante,
          ).overrideWith((ref) => estadoPendiente.future),
          restriccionVigenteProvider(
            _usuario,
          ).overrideWith((ref) => restriccionPendiente.future),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
          themeMode: ThemeMode.dark,
          home: const PantallaMiEstado(
            participanteId: _participante,
            usuarioId: _usuario,
            grupoId: _grupo,
          ),
        ),
      ),
    );
    await tester.pump();

    final contexto = tester.element(find.byType(PantallaMiEstado));
    expect(Theme.of(contexto).brightness, Brightness.dark);
    expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
    expect(find.bySemanticsLabel(TextosPasanaku.cargando), findsNWidgets(2));
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(PantallaMiEstado),
      matchesGoldenFile(
        '${_evidencia}h8-grupos-mi-estado-cargando-oscuro-360-130.png',
      ),
    );
  });

  testWidgets(
    'mi estado error permite reintentar · oscuro · 360 px · texto 130 %',
    (tester) async {
      tester.view.physicalSize = const Size(360, 740);
      tester.view.devicePixelRatio = 1;
      tester.platformDispatcher.textScaleFactorTestValue = 1.3;
      tester.platformDispatcher.accessibilityFeaturesTestValue =
          const FakeAccessibilityFeatures(disableAnimations: true);
      addTearDown(tester.view.reset);
      addTearDown(tester.platformDispatcher.clearAllTestValues);
      addTearDown(
        tester.platformDispatcher.clearAccessibilityFeaturesTestValue,
      );

      var intentos = 0;
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            estadoDelParticipanteProvider(_participante).overrideWith((
              ref,
            ) async {
              intentos++;
              if (intentos == 1) throw StateError('Fallo sintético');
              return aportes.EstadoDelParticipante(
                alDia: true,
                totalAportado: '2600.00',
                deudaVigente: '0.00',
                porAportar: '0.00',
                obligacionesAbiertas: 0,
                moneda: aportes.EstadoDelParticipanteMonedaEnum.BOB,
              );
            }),
            restriccionVigenteProvider(_usuario).overrideWith(
              (ref) async => garantia.RestriccionVigente(
                vigente: false,
                montoQueLaLevanta: '0.00',
              ),
            ),
          ],
          child: MaterialApp(
            theme: temaDesde(Tokens.claro, Brightness.light),
            darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
            themeMode: ThemeMode.dark,
            home: const PantallaMiEstado(
              participanteId: _participante,
              usuarioId: _usuario,
              grupoId: _grupo,
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      final contexto = tester.element(find.byType(PantallaMiEstado));
      expect(Theme.of(contexto).brightness, Brightness.dark);
      expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
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
      expect(tester.takeException(), isNull);
      await expectLater(
        find.byType(PantallaMiEstado),
        matchesGoldenFile(
          '${_evidencia}h8-grupos-mi-estado-error-oscuro-360-130.png',
        ),
      );

      await tester.tap(find.widgetWithText(FilledButton, 'Volver a intentar'));
      await tester.pumpAndSettle();
      expect(intentos, 2);
      expect(find.text('Al día'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );
}
