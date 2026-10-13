import 'dart:async';

import 'package:aportaya_movil/app.dart';
import 'package:aportaya_movil/dominio/verificacion_contrato.dart';
import 'package:aportaya_movil/dominio/puertos/biometria.dart';
import 'package:aportaya_movil/dominio/vigilante_de_sesion.dart';
import 'package:aportaya_movil/infraestructura/capacidades.dart';
import 'package:aportaya_movil/proveedores/biometria.dart';
import 'package:aportaya_movil/proveedores/bloqueo.dart';
import 'package:aportaya_movil/proveedores/capacidades.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  Future<
    ({
      ProviderContainer container,
      Sesion sesion,
      DateTime Function() ahora,
      void Function(Duration) avanzar,
    })
  >
  montarApp(WidgetTester tester, {Biometria? biometria}) async {
    var instante = DateTime.utc(2026, 10, 11, 12);
    DateTime ahora() => instante;
    void avanzar(Duration duracion) => instante = instante.add(duracion);

    final sesion = await sesionActivaDePrueba();
    final container = ProviderContainer(
      overrides: [
        sesionProvider.overrideWithValue(sesion),
        relojDeSesionProvider.overrideWithValue(ahora),
        capacidadesProvider.overrideWithValue(
          capacidadesDeLaPlataforma(forzarIOSParaPruebas: true),
        ),
        verificacionContratoProvider.overrideWith(
          (_) async => EstadoContrato.compatible,
        ),
        if (biometria != null) biometriaProvider.overrideWithValue(biometria),
      ],
    );
    addTearDown(container.dispose);
    addTearDown(sesion.dispose);

    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: const AppAportaYa(),
      ),
    );
    await tester.pump();
    await tester.pumpAndSettle();
    return (
      container: container,
      sesion: sesion,
      ahora: ahora,
      avanzar: avanzar,
    );
  }

  testWidgets('sesión activa queda bajo candado al hidratar antes de operar', (
    tester,
  ) async {
    final app = await montarApp(tester);

    expect(app.sesion.activa, isTrue);
    expect(app.container.read(bloqueoProvider).bloqueada, isTrue);
    expect(
      app.container.read(bloqueoProvider).motivo,
      MotivoDeBloqueo.arranque,
    );
  });

  testWidgets('al volver tras más de 60 s en segundo plano bloquea', (
    tester,
  ) async {
    final app = await montarApp(tester);
    app.container.read(bloqueoProvider).desbloquear();

    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
    app.avanzar(const Duration(seconds: 61));
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();

    expect(app.container.read(bloqueoProvider).bloqueada, isTrue);
    expect(
      app.container.read(bloqueoProvider).motivo,
      MotivoDeBloqueo.segundoPlano,
    );
  });

  testWidgets('inactive y resumed no simulan salida por hoja del sistema', (
    tester,
  ) async {
    final app = await montarApp(tester);
    app.container.read(bloqueoProvider).desbloquear();

    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();

    expect(app.container.read(bloqueoProvider).bloqueada, isFalse);
  });

  testWidgets(
    'tras cancelar biometría, una nueva vuelta de segundo plano reintenta',
    (tester) async {
      final biometria = _BiometriaCancelada();
      final app = await montarApp(tester, biometria: biometria);
      await tester.pumpAndSettle();
      expect(biometria.intentos, 1);

      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
      app.avanzar(const Duration(minutes: 2));
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await tester.pumpAndSettle();

      expect(app.container.read(bloqueoProvider).bloqueada, isTrue);
      expect(biometria.intentos, 2);
    },
  );

  testWidgets('espera el diálogo viejo antes de abrir el nuevo al reanudar', (
    tester,
  ) async {
    final biometria = _BiometriaPendiente();
    await montarApp(tester, biometria: biometria);
    await tester.pumpAndSettle();
    expect(biometria.intentos, 1);

    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pumpAndSettle();
    expect(biometria.intentos, 1);

    biometria.primera.complete(false);
    await tester.pump();
    await tester.pumpAndSettle();
    expect(biometria.intentos, 2);
  });

  testWidgets(
    'retomar desde segundo plano reintenta un candado por inactividad',
    (tester) async {
      final biometria = _BiometriaCancelada();
      final app = await montarApp(tester, biometria: biometria);
      app.container.read(bloqueoProvider).desbloquear();
      await tester.pumpAndSettle();
      app.container.read(bloqueoProvider).bloquear(MotivoDeBloqueo.inactividad);
      await tester.pumpAndSettle();
      expect(biometria.intentos, 1);

      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
      app.avanzar(const Duration(minutes: 2));
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await tester.pumpAndSettle();

      expect(app.container.read(bloqueoProvider).bloqueada, isTrue);
      expect(biometria.intentos, 2);
    },
  );

  testWidgets('un toque reinicia el plazo de inactividad de cinco minutos', (
    tester,
  ) async {
    final app = await montarApp(tester);
    app.container.read(bloqueoProvider).desbloquear();
    app.avanzar(const Duration(minutes: 4));
    await tester.tapAt(const Offset(8, 8));
    app.avanzar(const Duration(minutes: 4));
    await tester.pump(const Duration(seconds: 15));
    expect(app.container.read(bloqueoProvider).bloqueada, isFalse);

    app.avanzar(const Duration(minutes: 1));
    await tester.pump(const Duration(seconds: 15));
    expect(app.container.read(bloqueoProvider).bloqueada, isTrue);
    expect(
      app.container.read(bloqueoProvider).motivo,
      MotivoDeBloqueo.inactividad,
    );
  });
}

class _BiometriaCancelada implements Biometria {
  int intentos = 0;

  @override
  Future<bool> disponible() async => true;

  @override
  Future<bool> confirmar(String motivo) async {
    intentos++;
    return false;
  }
}

class _BiometriaPendiente implements Biometria {
  final primera = Completer<bool>();
  int intentos = 0;

  @override
  Future<bool> disponible() async => true;

  @override
  Future<bool> confirmar(String motivo) {
    intentos++;
    return intentos == 1 ? primera.future : Future.value(false);
  }
}
