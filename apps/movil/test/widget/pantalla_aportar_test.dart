import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/superficie_viva.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_aportar.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const _obligacion = '68a98585-2854-4b71-80fd-5ef490fe60f6';
const _ruta = '/aportes/obligaciones/$_obligacion/pago-con-saldo';

/// CU-21 con el saldo de la billetera (F1.L1.M11): la app no elige canal, no escribe
/// referencia ni dicta el monto. El cobro va sin campos (`{}`), solo con la clave de
/// idempotencia. El servidor está doblado (DOBLE declarado, regla 65): `dioSimulado`.
void main() {
  Future<void> abrir(WidgetTester tester, dio) async {
    tester.view.physicalSize = const Size(1080, 2280);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      conApp(
        const PantallaAportar(
          obligacionId: _obligacion,
          montoDelPeriodo: '4.44',
        ),
        dio: dio,
      ),
    );
    await asentar(tester);
  }

  Map<String, dynamic> pagoOk() =>
      ejemplo('aportes', 'pagarCuotaConSaldo', 'ok')['cuerpo']
          as Map<String, dynamic>;

  testWidgets('sin canal, sin referencia y sin monto editable', (tester) async {
    final (:dio, adaptador: _) = dioSimulado();
    await abrir(tester, dio);
    expect(find.textContaining('4,44'), findsWidgets);
    expect(find.byType(TextField), findsNothing);
    expect(find.text('Cómo pagás'), findsNothing);
    expect(
      find.textContaining('Se paga con el saldo de tu billetera'),
      findsOneWidget,
    );
    // «Aportar» es pagar algo que vence: el único botón naranja (A2), habilitado ya.
    expect(find.widgetWithText(Boton, 'Aportar Bs 4,44'), findsOneWidget);
    expect(
      tester.widget<SuperficieViva>(find.byType(SuperficieViva).first).color,
      Tokens.claro.accent,
    );
  });

  testWidgets('correcto: paga sin campos, con clave, y confirma en palabras', (
    tester,
  ) async {
    final (:dio, :adaptador) = dioSimulado();
    Object? cuerpoEnviado = 'sin pedido';
    String? clave;
    adaptador.onPost(_ruta, (s) => s.reply(201, pagoOk()));
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (o, h) {
          cuerpoEnviado = o.data;
          clave = o.headers['Idempotency-Key'] as String?;
          h.next(o);
        },
      ),
    );
    await abrir(tester, dio);
    await tester.tap(find.widgetWithText(Boton, 'Aportar Bs 4,44'));
    await asentar(tester);
    // Sin campos: ni monto, ni canal, ni referencia (el contrato acepta «ausente o {}»).
    expect(cuerpoEnviado, '{}');
    expect(clave, isNotEmpty);
    expect(find.text('Tu aporte quedó registrado'), findsOneWidget);
    expect(find.textContaining('PENDIENTE'), findsNothing);
  });

  testWidgets('límite: la respuesta de un reintento (200) también confirma', (
    tester,
  ) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onPost(_ruta, (s) => s.reply(200, pagoOk()));
    await abrir(tester, dio);
    await tester.tap(find.widgetWithText(Boton, 'Aportar Bs 4,44'));
    await asentar(tester);
    expect(find.text('Tu aporte quedó registrado'), findsOneWidget);
  });

  testWidgets(
    'inválido: saldo insuficiente muestra el texto de la app, no el del servidor',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        _ruta,
        (s) => s.reply(422, {
          'codigo': 'AP-CU21-03',
          'mensaje': 'saldo insuficiente (texto crudo del servidor)',
          'detalle': {},
          'trazaId': 't',
        }),
      );
      await abrir(tester, dio);
      await tester.tap(find.widgetWithText(Boton, 'Aportar Bs 4,44'));
      await asentar(tester);
      expect(find.textContaining('No te alcanza el saldo'), findsOneWidget);
      expect(find.textContaining('texto crudo'), findsNothing);
      expect(find.text('Tu aporte quedó registrado'), findsNothing);
    },
  );
}
