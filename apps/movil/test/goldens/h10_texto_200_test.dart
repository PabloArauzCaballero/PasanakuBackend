import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/moleculas/campo_de_fecha.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_sesion.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_datos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'cargar_fuentes_de_aplicacion.dart';

const _cuenta = '11111111-1111-4111-8111-111111111111';
const _rutaSaldo = '/billetera/$_cuenta/saldo';
const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h10-texto-200';

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
    expect(tester.takeException(), isNull);
  }

  testWidgets('ingreso · sistema al 200 %', (tester) async {
    viewport200(tester);
    final (:dio, adaptador: _) = dioSimulado();
    await tester.pumpWidget(conApp(const PantallaDeSesion(), dio: dio));
    await asentar(tester);

    comprobarEscala200(tester);
    expect(find.text('Ingresar'), findsOneWidget);
    expect(find.text('Mínimo 8'), findsOneWidget);
    await expectLater(
      find.byType(PantallaDeSesion),
      matchesGoldenFile('$_evidencia/ingreso-360-200.png'),
    );
    await Scrollable.ensureVisible(
      tester.element(find.byType(TextField).last),
      alignment: 0.3,
      duration: Duration.zero,
    );
    await tester.drag(find.byType(ListView).first, const Offset(0, 48));
    await tester.drag(find.byType(ListView).first, const Offset(0, -32));
    await tester.pumpAndSettle();
    comprobarEscala200(tester);
    await expectLater(
      find.byType(PantallaDeSesion),
      matchesGoldenFile('$_evidencia/ingreso-360-200-desplazado.png'),
    );
  });

  testWidgets('alta · sistema al 200 %', (tester) async {
    viewport200(tester);
    final contenedor = ProviderContainer();
    addTearDown(contenedor.dispose);
    contenedor.read(altaProvider.notifier).siguiente();
    contenedor.read(altaProvider.notifier).siguiente();
    contenedor.read(altaProvider.notifier).siguiente();
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
    expect(find.text('Segundo nombre (opcional)'), findsOneWidget);
    await expectLater(
      find.byType(PasoDatos),
      matchesGoldenFile('$_evidencia/alta-360-200.png'),
    );
    await Scrollable.ensureVisible(
      tester.element(find.byType(CampoDeFecha)),
      alignment: 0.25,
      duration: Duration.zero,
    );
    await tester.pumpAndSettle();
    comprobarEscala200(tester);
    await expectLater(
      find.byType(PasoDatos),
      matchesGoldenFile('$_evidencia/alta-fecha-360-200.png'),
    );
    await tester.ensureVisible(find.text('Continuar'));
    await tester.pumpAndSettle();
    comprobarEscala200(tester);
    await expectLater(
      find.byType(PasoDatos),
      matchesGoldenFile('$_evidencia/alta-360-200-desplazado.png'),
    );
  });

  testWidgets('billetera · saldo alto · sistema al 200 %', (tester) async {
    viewport200(tester);
    final (:dio, :adaptador) = dioSimulado();
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
    expect(find.text('Aportar'), findsOneWidget);
    expect(find.text('Recargar'), findsOneWidget);
    expect(find.text('Retirar'), findsOneWidget);
    await expectLater(
      find.byType(PantallaDeSaldo),
      matchesGoldenFile('$_evidencia/billetera-360-200.png'),
    );
    await tester.ensureVisible(find.text('Movimientos'));
    await tester.pumpAndSettle();
    comprobarEscala200(tester);
    await expectLater(
      find.byType(PantallaDeSaldo),
      matchesGoldenFile('$_evidencia/billetera-360-200-desplazado.png'),
    );
  });
}
