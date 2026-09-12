import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';

const cuenta = '11111111-1111-4111-8111-111111111111';
const ruta = '/billetera/$cuenta/saldo';

/// Los cuatro estados de la pantalla real de F0, contra los ejemplos del contrato:
/// los mismos JSON que Prism sirve en desarrollo. Una pantalla probada solo en su
/// camino feliz es una pantalla que en la calle se queda en blanco.
void main() {
  testWidgets('cargando: lo dice mientras espera, sin pantalla en blanco', (
    tester,
  ) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      ruta,
      (s) => s.reply(
        200,
        ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
        delay: const Duration(milliseconds: 80),
      ),
    );
    await tester.pumpWidget(
      conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
    );
    await tester.pump();
    expect(find.bySemanticsLabel('Cargando tu saldo'), findsOneWidget);
    await asentar(tester);
  });

  testWidgets(
    'éxito: muestra el saldo que respondió el contrato, sin recalcular nada',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        ruta,
        (s) => s.reply(
          200,
          ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
        ),
      );
      await tester.pumpWidget(
        conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
      );
      await asentar(tester);
      // El importe se presenta como la maqueta —`Bs 1.240,00`— y se anuncia con su concepto.
      expect(
        find.bySemanticsLabel(
          RegExp(r'^Saldo disponible: (Bs|USD) -?[\d.]+,\d{2}$'),
        ),
        findsOneWidget,
      );
      expect(find.text('Ver aportes pendientes'), findsOneWidget);
      expect(find.text('Movimientos'), findsOneWidget);
      expect(find.textContaining('Banco Unión'), findsOneWidget);
    },
  );

  testWidgets(
    'vacío: una cuenta en cero dice por qué no hay nada y qué hacer',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      final cero = {
        'cuentaId': cuenta,
        'disponible': {'monto': '0.00', 'moneda': 'BOB'},
        'retenido': {'monto': '0.00', 'moneda': 'BOB'},
        'alCorteDe': '2026-01-15T10:30:00Z',
      };
      adaptador.onGet(ruta, (s) => s.reply(200, cero));
      await tester.pumpWidget(
        conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
      );
      await asentar(tester);
      expect(
        find.textContaining('Todavía no tenés movimientos'),
        findsOneWidget,
      );
    },
  );

  testWidgets(
    'error: mensaje humano, reintento a la vista y traza para soporte',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        ruta,
        (s) => s.reply(401, {
          'codigo': 'AP-CU04-01',
          'mensaje': 'tecnico',
          'trazaId': 'traza-401',
        }),
      );
      await tester.pumpWidget(
        conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
      );
      await asentar(tester);
      expect(find.text('Volver a intentar'), findsOneWidget);
      expect(
        find.textContaining('Código de seguimiento: traza-401'),
        findsOneWidget,
      );
      expect(
        find.text('tecnico'),
        findsNothing,
        reason: 'el mensaje del backend no se muestra crudo',
      );
    },
  );

  testWidgets(
    'sin red: no se queda cargando para siempre, y dice que no hay conexión',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        ruta,
        (s) => s.throws(
          0,
          DioException.connectionError(
            requestOptions: RequestOptions(path: ruta),
            reason: 'sin red',
          ),
        ),
      );
      await tester.pumpWidget(
        conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
      );
      await asentar(tester);
      expect(find.text('Volver a intentar'), findsOneWidget);
      expect(find.textContaining('No hay conexión'), findsOneWidget);
    },
  );

  testWidgets('un 403 no dice más que «no tenés acceso»', (tester) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      ruta,
      (s) => s.reply(403, {
        'codigo': 'AP-SEG-03',
        'mensaje': 'detalle',
        'trazaId': 't',
      }),
    );
    await tester.pumpWidget(
      conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
    );
    await asentar(tester);
    expect(find.text('No tenés acceso a esto.'), findsOneWidget);
  });

  testWidgets(
    'reintentar vuelve a pedir el saldo al servidor: nunca se ajusta en memoria',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      // Primero el gateway falla; después responde. Registrar la ruta de nuevo
      // reemplaza la respuesta anterior.
      adaptador.onGet(
        ruta,
        (s) => s.reply(503, {
          'codigo': 'AP-GW-503',
          'mensaje': '',
          'trazaId': 't',
        }),
      );
      await tester.pumpWidget(
        conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
      );
      await asentar(tester);
      expect(find.text('Volver a intentar'), findsOneWidget);
      adaptador.onGet(
        ruta,
        (s) => s.reply(
          200,
          ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
        ),
      );
      await tester.tap(find.text('Volver a intentar'));
      await asentar(tester);
      expect(
        find.bySemanticsLabel(RegExp(r'^Saldo disponible: ')),
        findsOneWidget,
      );
      expect(find.text('Volver a intentar'), findsNothing);
    },
  );
}
