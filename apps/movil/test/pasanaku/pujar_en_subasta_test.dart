import 'dart:convert';

import 'package:aportaya_movil/pantallas/pasanaku/pantalla_subasta.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_subastas.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';
import 'subastar_cupo_test.dart' show subastaSintetica;

/// CU-64 por subasta · cualquiera ve los cupos en subasta y ofrece. Datos SINTÉTICOS.
const _sub = '55555555-5555-4555-8555-555555555555';

Future<(DioAdapter, List<Map<String, dynamic>>)> _detalle(
  WidgetTester t,
  Map<String, dynamic> subasta,
) async {
  t.view.physicalSize = const Size(412, 2400);
  t.view.devicePixelRatio = 1;
  addTearDown(t.view.reset);
  final (:dio, :adaptador) = dioSimulado();
  final pujas = <Map<String, dynamic>>[];
  dio.interceptors.insert(
    0,
    InterceptorsWrapper(
      onRequest: (o, h) {
        if (o.method == 'POST') {
          final d = o.data;
          pujas.add({
            'clave': o.headers['Idempotency-Key'],
            ...((d is String ? jsonDecode(d) : d) as Map<String, dynamic>),
          });
        }
        h.next(o);
      },
    ),
  );
  adaptador.onGet('/entregas/subastas/$_sub', (s) => s.reply(200, subasta));
  await t.pumpWidget(conApp(const PantallaSubasta(subastaId: _sub), dio: dio));
  await asentar(t);
  return (adaptador, pujas);
}

Future<void> _ofrecer(
  WidgetTester t,
  String monto, {
  bool acepto = true,
}) async {
  await t.enterText(
    find.descendant(
      of: find.byKey(const ValueKey('monto-de-la-puja')),
      matching: find.byType(TextField),
    ),
    monto,
  );
  await t.pump();
  if (acepto) {
    await t.tap(find.byKey(const ValueKey('acepto-reglas')));
    await t.pump();
  }
  await t.tap(find.byKey(const ValueKey('ofrecer')));
  await t.pumpAndSettle();
}

Future<void> _lista(WidgetTester t, int estado, Object cuerpo) async {
  t.view.physicalSize = const Size(412, 2400);
  t.view.devicePixelRatio = 1;
  addTearDown(t.view.reset);
  final (:dio, :adaptador) = dioSimulado();
  adaptador.onGet(
    '/entregas/subastas',
    (s) => s.reply(estado, cuerpo),
    queryParameters: {'limite': 20, 'pagina': 0},
  );
  await t.pumpWidget(conApp(const PantallaSubastas(), dio: dio));
  await asentar(t);
}

void main() {
  testWidgets(
    'la lista muestra cada cupo con su oferta más alta, sin quién sale',
    (t) async {
      await _lista(t, 200, {
        'pagina': 0,
        'subastas': [
          subastaSintetica(soyElSaliente: false, masAlto: '1800.00', pujas: 2),
        ],
      });
      expect(find.text('Vecinos del barrio'), findsOneWidget);
      expect(find.text('Oferta más alta'), findsOneWidget);
      expect(find.textContaining('2 ofertas'), findsOneWidget);
    },
  );

  testWidgets('vacío: sin cupos en subasta, lo dice y orienta', (t) async {
    await _lista(t, 200, {'pagina': 0, 'subastas': []});
    expect(
      find.textContaining('Ahora no hay cupos en subasta'),
      findsOneWidget,
    );
  });

  testWidgets('error: si la lista no carga se puede reintentar', (t) async {
    await _lista(t, 500, {'codigo': 'AP-X', 'mensaje': 'x', 'trazaId': 't'});
    expect(find.text('Volver a intentar'), findsOneWidget);
  });

  testWidgets(
    'correcto: ofrecer manda UNA puja con el monto, las reglas aceptadas y clave',
    (t) async {
      final (a, pujas) = await _detalle(
        t,
        subastaSintetica(soyElSaliente: false),
      );
      a.onPost(
        '/entregas/subastas/$_sub/pujas',
        (s) => s.reply(201, {'pujaId': 'p1', 'estado': 'VIGENTE'}),
        data: Matchers.any,
      );

      await _ofrecer(t, '1600');
      expect(find.textContaining('Retenemos'), findsOneWidget);
      await t.tap(find.text('Sí, ofrecer'));
      await asentar(t);

      expect(pujas, hasLength(1));
      expect(pujas.single['monto'], {'monto': '1600.00', 'moneda': 'BOB'});
      expect(pujas.single['aceptoReglamento'], true);
      expect(pujas.single['clave'], isNotNull);
    },
  );

  testWidgets(
    'límite: sin aceptar las reglas no se ofrece, y el botón dice por qué',
    (t) async {
      final (_, pujas) = await _detalle(
        t,
        subastaSintetica(soyElSaliente: false),
      );
      await _ofrecer(t, '1600', acepto: false);
      expect(
        find.text('Aceptá las reglas del grupo para ofrecer.'),
        findsOneWidget,
      );
      expect(find.text('Sí, ofrecer'), findsNothing);
      expect(pujas, isEmpty);
    },
  );

  testWidgets('inválido: AP-CU136-04 dice que alguien ofreció más, en voseo', (
    t,
  ) async {
    final (a, _) = await _detalle(t, subastaSintetica(soyElSaliente: false));
    a.onPost(
      '/entregas/subastas/$_sub/pujas',
      (s) => s.reply(422, {
        'codigo': 'AP-CU136-04',
        'mensaje': 'texto del backend',
        'trazaId': 'traza-sintetica',
      }),
      data: Matchers.any,
    );
    await _ofrecer(t, '1600');
    await t.tap(find.text('Sí, ofrecer'));
    await asentar(t);

    expect(find.textContaining('Alguien ya ofreció eso o más'), findsOneWidget);
    expect(find.textContaining('texto del backend'), findsNothing);
  });

  testWidgets('quien sale no ve el formulario: sigue la subasta', (t) async {
    await _detalle(t, subastaSintetica());
    expect(find.byKey(const ValueKey('ofrecer')), findsNothing);
    expect(
      find.text('Es tu cupo: seguís la subasta desde acá.'),
      findsOneWidget,
    );
  });

  testWidgets('si me superaron, se dice que el saldo ya volvió', (t) async {
    await _detalle(
      t,
      subastaSintetica(
        soyElSaliente: false,
        masAlto: '1900.00',
        pujas: 2,
        miPuja: {
          'monto': {'monto': '1600.00', 'moneda': 'BOB'},
          'estado': 'LIBERADA',
        },
      ),
    );
    expect(find.textContaining('Te superaron'), findsOneWidget);
  });
}
