import 'dart:async';

import 'package:aportaya_movil/dominio/vigilante_de_sesion.dart';
import 'package:aportaya_movil/dominio/puertos/biometria.dart';
import 'package:aportaya_movil/pantallas/identidad/capa_de_bloqueo.dart';
import 'package:aportaya_movil/proveedores/biometria.dart';
import 'package:aportaya_movil/proveedores/bloqueo.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  // La app ya está al frente: es la condición para que la capa pida la huella.
  setUp(
    () => TestWidgetsFlutterBinding.instance.handleAppLifecycleStateChanged(
      AppLifecycleState.resumed,
    ),
  );

  testWidgets(
    'arranque en frío: la huella se pide recién cuando la app queda al frente',
    (tester) async {
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
      final biometria = _BiometriaFalsa();
      final control = ControlDeBloqueo(esWeb: false)
        ..bloquear(MotivoDeBloqueo.arranque);

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            bloqueoProvider.overrideWith((_) => control),
            biometriaProvider.overrideWithValue(biometria),
          ],
          child: MaterialApp(
            theme: temaDesde(Tokens.claro, Brightness.light),
            home: CapaDeBloqueo(
              hijo: const Scaffold(body: Text('Contenido')),
              onIngresarDeNuevo: () {},
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(biometria.llamadas, 0);

      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await tester.pumpAndSettle();
      expect(biometria.llamadas, 1);
    },
  );

  testWidgets('candado absorbe toques y oculta el hijo de semántica', (
    tester,
  ) async {
    var toquesDelHijo = 0;
    final control = ControlDeBloqueo(esWeb: false)
      ..bloquear(MotivoDeBloqueo.segundoPlano);
    final semantics = tester.ensureSemantics();

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          bloqueoProvider.overrideWith((_) => control),
          biometriaDisponibleProvider.overrideWith((_) async => false),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
          home: CapaDeBloqueo(
            hijo: Scaffold(
              body: Center(
                child: GestureDetector(
                  onTap: () => toquesDelHijo++,
                  child: const Text('Saldo privado'),
                ),
              ),
            ),
            onIngresarDeNuevo: () {},
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(
      find.text('Saliste de la app un rato. Confirmá que sos vos para seguir.'),
      findsOneWidget,
    );
    expect(find.text('Saldo privado'), findsOneWidget);
    expect(find.bySemanticsLabel('Saldo privado'), findsNothing);

    await tester.tapAt(tester.getCenter(find.text('Saldo privado')));
    expect(toquesDelHijo, 0);
    expect(find.text('Entrar con mi contraseña'), findsOneWidget);
    semantics.dispose();
  });

  testWidgets(
    'biometría automática pide una vez por episodio, pero no por inactividad',
    (tester) async {
      final biometria = _BiometriaFalsa();
      final control = ControlDeBloqueo(esWeb: false)
        ..bloquear(MotivoDeBloqueo.arranque);

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            bloqueoProvider.overrideWith((_) => control),
            biometriaProvider.overrideWithValue(biometria),
          ],
          child: MaterialApp(
            theme: temaDesde(Tokens.claro, Brightness.light),
            home: CapaDeBloqueo(
              hijo: const Scaffold(body: Text('Contenido')),
              onIngresarDeNuevo: () {},
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(biometria.llamadas, 1);

      // Canceló el diálogo: rebuilds y una llamada redundante a bloquear no lo
      // vuelven a abrir mientras el candado siga en el mismo episodio.
      control.bloquear(MotivoDeBloqueo.segundoPlano);
      await tester.pump();
      expect(biometria.llamadas, 1);

      control.desbloquear();
      await tester.pump();
      control.bloquear(MotivoDeBloqueo.inactividad);
      await tester.pumpAndSettle();
      expect(biometria.llamadas, 1);

      control.desbloquear();
      await tester.pump();
      control.bloquear(MotivoDeBloqueo.segundoPlano);
      await tester.pumpAndSettle();
      expect(biometria.llamadas, 2);
      expect(biometria.motivos, ['Desbloqueá AportaYa', 'Desbloqueá AportaYa']);
    },
  );

  testWidgets('el botón biométrico permite reintentar tras una cancelación', (
    tester,
  ) async {
    final biometria = _BiometriaFalsa()..resultado = true;
    final control = ControlDeBloqueo(esWeb: false)
      ..bloquear(MotivoDeBloqueo.inactividad);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          bloqueoProvider.overrideWith((_) => control),
          biometriaProvider.overrideWithValue(biometria),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: CapaDeBloqueo(
            hijo: const Scaffold(body: Text('Contenido')),
            onIngresarDeNuevo: () {},
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(biometria.llamadas, 0);
    await tester.tap(find.text('Entrar con huella o PIN'));
    await tester.pumpAndSettle();

    expect(biometria.llamadas, 1);
    expect(control.bloqueada, isFalse);
  });

  testWidgets('un pedido colgado no deja el botón sin efecto: el toque abre uno '
      'nuevo y desbloquea', (tester) async {
    // El primer diálogo nunca responde (el caso del teléfono con la huella rota o
    // el pedido del arranque que quedó colgado); el segundo confirma con el PIN.
    final colgado = Completer<bool>();
    final biometria = _BiometriaFalsa()
      ..respuestas = [colgado.future, Future.value(true)];
    final control = ControlDeBloqueo(esWeb: false)
      ..bloquear(MotivoDeBloqueo.inactividad);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          bloqueoProvider.overrideWith((_) => control),
          biometriaProvider.overrideWithValue(biometria),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: CapaDeBloqueo(
            hijo: const Scaffold(body: Text('Contenido')),
            onIngresarDeNuevo: () {},
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Entrar con huella o PIN'));
    await tester.pump();
    expect(biometria.llamadas, 1);
    expect(control.bloqueada, isTrue);

    await tester.tap(find.text('Entrar con huella o PIN'));
    await tester.pumpAndSettle();

    expect(biometria.llamadas, 2);
    expect(control.bloqueada, isFalse);
    colgado.complete(false);
  });
}

class _BiometriaFalsa implements Biometria {
  int llamadas = 0;
  bool resultado = false;
  final motivos = <String>[];

  @override
  Future<bool> disponible() async => true;

  /// Si hay respuestas preparadas, cada llamada toma la siguiente.
  List<Future<bool>> respuestas = [];

  @override
  Future<bool> confirmar(String motivo) {
    llamadas++;
    motivos.add(motivo);
    if (respuestas.isNotEmpty) return respuestas.removeAt(0);
    return Future.value(resultado);
  }
}
