import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_perfil.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_cuenta.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_datos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'cargar_fuentes_de_aplicacion.dart';
import '../comun.dart';

const _cuenta = '11111111-1111-4111-8111-111111111111';
const _rutaSaldo = '/billetera/$_cuenta/saldo';
const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h8-break-ui';

final _correo150 = '${'a' * 64}@${'b' * 60}.${'c' * 21}.bo';

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  void viewport200(WidgetTester tester) {
    tester.view.physicalSize = const Size(360, 740);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 2;
    addTearDown(tester.view.reset);
    addTearDown(tester.platformDispatcher.clearAllTestValues);
  }

  void comprobarEscala200(WidgetTester tester) {
    final contexto = tester.element(find.byType(Scaffold).first);
    expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(20, 0.001));
  }

  testWidgets('alta · nombre de 60 caracteres con tildes y ñ · 200 %', (
    tester,
  ) async {
    viewport200(tester);
    final contenedor = ProviderContainer();
    addTearDown(contenedor.dispose);
    contenedor.read(altaProvider.notifier).siguiente();
    contenedor.read(altaProvider.notifier).siguiente();
    contenedor.read(altaProvider.notifier).siguiente();
    contenedor
        .read(altaProvider.notifier)
        .actualizarDatos(
          contenedor
              .read(altaProvider)
              .datos
              .copiarCon(
                primerNombre: 'Áñé' * 20,
                apellidoPaterno: 'Peña',
                apellidoMaterno: 'Álvarez',
                fechaNacimiento: DateTime(1995, 4, 12),
              ),
        );
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const Scaffold(body: SingleChildScrollView(child: PasoDatos())),
        ),
      ),
    );
    await tester.pumpAndSettle();

    comprobarEscala200(tester);
    expect('Áñé' * 20, hasLength(60));
    expect(
      tester.widget<TextField>(find.byType(TextField).first).controller!.text,
      'Áñé' * 20,
    );
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(PasoDatos),
      matchesGoldenFile('$_evidencia/alta-datos-60-200.png'),
    );
  });

  testWidgets('alta · correo de 150 caracteres · 200 %', (tester) async {
    viewport200(tester);
    final (:dio, :adaptador) = dioSimulado();
    final contenedor = ProviderContainer();
    addTearDown(contenedor.dispose);
    contenedor
        .read(altaProvider.notifier)
        .actualizarDatos(
          contenedor
              .read(altaProvider)
              .datos
              .copiarCon(telefono: '+59170000001', correo: _correo150),
        );
    await tester.pumpWidget(
      ProviderScope(
        overrides: [dioProvider.overrideWithValue(dio)],
        child: UncontrolledProviderScope(
          container: contenedor,
          child: MaterialApp(
            theme: temaDesde(Tokens.oscuro, Brightness.dark),
            home: const Scaffold(
              body: SingleChildScrollView(child: PasoCuenta()),
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    comprobarEscala200(tester);
    expect(_correo150, hasLength(150));
    expect(
      tester.widget<TextField>(find.byType(TextField).at(1)).controller!.text,
      _correo150,
    );
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(PasoCuenta),
      matchesGoldenFile('$_evidencia/alta-cuenta-email-150-200.png'),
    );
  });

  testWidgets('billetera · saldo 9.999.999,99 y vacíos · 200 %', (
    tester,
  ) async {
    viewport200(tester);
    final (:dio, :adaptador) = dioSimulado();
    // «Tus pasanakus» pide sus grupos aparte (2026-10-21): sin grupos, como antes.
    adaptador.onGet('/grupos/participaciones', (s) => s.reply(200, <Object>[]));
    adaptador.onGet(
      _rutaSaldo,
      (s) => s.reply(200, {
        'cuentaId': _cuenta,
        'disponible': {'monto': '9999999.99', 'moneda': 'BOB'},
        'retenido': {'monto': '0.00', 'moneda': 'BOB'},
        'alCorteDe': '2026-10-12T10:30:00Z',
      }),
    );
    await tester.pumpWidget(
      conApp(const PantallaDeSaldo(cuentaId: _cuenta), dio: dio),
    );
    await asentar(tester);

    comprobarEscala200(tester);
    expect(find.text('Tus pasanakus'), findsOneWidget);
    expect(
      find.textContaining('Todavía no estás en ningún pasanaku'),
      findsOneWidget,
    );
    expect(find.textContaining('9.999.999,99'), findsOneWidget);
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(PantallaDeSaldo),
      matchesGoldenFile('$_evidencia/billetera-saldo-alto-vacios-200.png'),
    );
    await tester.drag(find.byType(ListView).first, const Offset(0, -560));
    await tester.pumpAndSettle();
    expect(find.text('Movimientos'), findsOneWidget);
    expect(find.textContaining('Todavía no tenés movimientos'), findsOneWidget);
  });

  testWidgets('billetera · saldo cero y vacíos · 200 % · oscuro', (
    tester,
  ) async {
    viewport200(tester);
    final (:dio, :adaptador) = dioSimulado();
    // «Tus pasanakus» pide sus grupos aparte (2026-10-21): sin grupos, como antes.
    adaptador.onGet('/grupos/participaciones', (s) => s.reply(200, <Object>[]));
    adaptador.onGet(
      _rutaSaldo,
      (s) => s.reply(200, {
        'cuentaId': _cuenta,
        'disponible': {'monto': '0.00', 'moneda': 'BOB'},
        'retenido': {'monto': '0.00', 'moneda': 'BOB'},
        'alCorteDe': '2026-10-12T10:30:00Z',
      }),
    );
    await tester.pumpWidget(
      conApp(
        const PantallaDeSaldo(cuentaId: _cuenta),
        dio: dio,
        brillo: Brightness.dark,
      ),
    );
    await asentar(tester);

    comprobarEscala200(tester);
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(PantallaDeSaldo),
      matchesGoldenFile('$_evidencia/billetera-saldo-cero-200.png'),
    );
    // Sin movimientos la pantalla puede no tener nada que desplazar: el vacío ya se
    // ve entero. Se arrastra solo si hay algo desplazable; la aserción no cambia.
    final desplazable = find.byType(Scrollable);
    if (desplazable.evaluate().isNotEmpty) {
      await tester.drag(desplazable.first, const Offset(0, -560));
      await tester.pumpAndSettle();
    }
    expect(find.textContaining('Todavía no tenés movimientos'), findsOneWidget);
  });

  testWidgets('billetera · 40 movimientos · 200 %', (tester) async {
    viewport200(tester);
    final (:dio, :adaptador) = dioSimulado();
    // «Tus pasanakus» pide sus grupos aparte (2026-10-21): sin grupos, como antes.
    adaptador.onGet('/grupos/participaciones', (s) => s.reply(200, <Object>[]));
    adaptador.onGet(
      '/billetera/$_cuenta/movimientos',
      (s) => s.reply(200, {
        'cuentaId': _cuenta,
        'movimientos': [
          for (var i = 0; i < 40; i++)
            {
              'id': 'mov-$i',
              'sentido': i.isEven ? 'CREDITO' : 'DEBITO',
              'monto': {'monto': '1234.56', 'moneda': 'BOB'},
              'saldoDisponiblePosterior': {'monto': '1234.56', 'moneda': 'BOB'},
              'saldoRetenidoPosterior': {'monto': '0.00', 'moneda': 'BOB'},
              'glosa': 'Movimiento de prueba ${i.toString().padLeft(2, '0')}',
              'registradoEn': DateTime.utc(
                2026,
                10,
                12,
                10,
              ).subtract(Duration(minutes: i)).toIso8601String(),
            },
        ],
      }),
    );
    adaptador.onGet(
      _rutaSaldo,
      (s) => s.reply(200, {
        'cuentaId': _cuenta,
        'disponible': {'monto': '9999999.99', 'moneda': 'BOB'},
        'retenido': {'monto': '0.00', 'moneda': 'BOB'},
        'alCorteDe': '2026-10-12T10:30:00Z',
      }),
    );
    await tester.pumpWidget(
      conApp(const PantallaDeSaldo(cuentaId: _cuenta), dio: dio),
    );
    await asentar(tester);
    await tester.pumpAndSettle();

    comprobarEscala200(tester);
    await tester.drag(find.byType(ListView).first, const Offset(0, -560));
    await tester.pumpAndSettle();
    expect(find.text('Movimiento de prueba 00'), findsOneWidget);
    expect(find.text('Movimiento de prueba 39'), findsOneWidget);
    expect(find.byType(ListTile), findsNWidgets(40));
    expect(tester.takeException(), isNull);
    await tester.ensureVisible(find.text('Movimiento de prueba 39'));
    await tester.pumpAndSettle();
    await expectLater(
      find.byType(PantallaDeSaldo),
      matchesGoldenFile('$_evidencia/billetera-40-movimientos-200.png'),
    );

    await tester.pumpWidget(
      conApp(
        const PantallaDeSaldo(cuentaId: _cuenta),
        dio: dio,
        brillo: Brightness.dark,
      ),
    );
    await asentar(tester);
    await tester.pumpAndSettle();
    expect(
      Theme.of(tester.element(find.byType(Scaffold).first)).brightness,
      Brightness.dark,
    );
    await tester.drag(find.byType(ListView).first, const Offset(0, -560));
    await tester.pumpAndSettle();
    expect(find.byType(ListTile), findsNWidgets(40));
    expect(tester.takeException(), isNull);
    await tester.ensureVisible(find.text('Movimiento de prueba 39'));
    await tester.pumpAndSettle();
    await expectLater(
      find.byType(PantallaDeSaldo),
      matchesGoldenFile('$_evidencia/billetera-40-movimientos-200-oscuro.png'),
    );
  });

  testWidgets('perfil · correo de 150 caracteres · 200 %', (tester) async {
    viewport200(tester);
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const PantallaDePerfil(),
        ),
      ),
    );
    await tester.enterText(find.byType(TextField).first, _correo150);
    await tester.pumpAndSettle();

    comprobarEscala200(tester);
    expect(_correo150, hasLength(150));
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(PantallaDePerfil),
      matchesGoldenFile('$_evidencia/perfil-email-150-200.png'),
    );
  });
}
