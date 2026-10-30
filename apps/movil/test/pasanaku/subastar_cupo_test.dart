import 'dart:convert';

import 'package:aportaya_movil/pantallas/pasanaku/pantalla_subastar_cupo.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// CU-64 por subasta · el saliente pone su cupo en subasta. Datos SINTÉTICOS.
const _g = '11111111-1111-4111-8111-111111111111';
const _cupo = '22222222-2222-4222-8222-222222222222';
const _sub = '55555555-5555-4555-8555-555555555555';

Map<String, dynamic> subastaSintetica({
  String estado = 'ABIERTA',
  String? masAlto,
  int pujas = 0,
  bool soyElSaliente = true,
  Map<String, dynamic>? miPuja,
}) => {
  'subastaId': _sub,
  'grupoId': _g,
  'cupoId': _cupo,
  'nombreDelGrupo': 'Vecinos del barrio',
  'periodicidad': 'MENSUAL',
  'montoAporte': {'monto': '200.00', 'moneda': 'BOB'},
  'montoBase': {'monto': '1500.00', 'moneda': 'BOB'},
  'montoMasAlto': masAlto == null ? null : {'monto': masAlto, 'moneda': 'BOB'},
  'montoMinimo': {'monto': masAlto ?? '1500.00', 'moneda': 'BOB'},
  'estado': estado,
  'cierraEn': '2026-11-02T12:00:00Z',
  'cantidadDePujas': pujas,
  'soyElSaliente': soyElSaliente,
  'miPuja': miPuja,
};

typedef _Envio = ({String path, String? clave, Map<String, dynamic>? cuerpo});

Future<(DioAdapter, List<_Envio>)> _montar(
  WidgetTester t, {
  List<Map<String, dynamic>> existentes = const [],
}) async {
  t.view.physicalSize = const Size(412, 2400);
  t.view.devicePixelRatio = 1;
  addTearDown(t.view.reset);
  final (:dio, :adaptador) = dioSimulado();
  final envios = <_Envio>[];
  dio.interceptors.insert(
    0,
    InterceptorsWrapper(
      onRequest: (o, h) {
        if (o.method == 'POST') {
          final d = o.data;
          envios.add((
            path: o.path,
            clave: o.headers['Idempotency-Key'] as String?,
            cuerpo: d == null
                ? null
                : (d is String ? jsonDecode(d) : d) as Map<String, dynamic>,
          ));
        }
        h.next(o);
      },
    ),
  );
  adaptador.onGet(
    '/entregas/subastas',
    (s) => s.reply(200, {'pagina': 0, 'subastas': existentes}),
    queryParameters: {'cupoId': _cupo, 'limite': 20, 'pagina': 0},
  );
  await t.pumpWidget(
    conApp(
      const PantallaSubastarCupo(grupoId: _g, cupoId: _cupo),
      dio: dio,
    ),
  );
  await asentar(t);
  return (adaptador, envios);
}

Future<void> _ponerYConfirmar(WidgetTester t, String base) async {
  await t.enterText(
    find.descendant(
      of: find.byKey(const ValueKey('precio-base')),
      matching: find.byType(TextField),
    ),
    base,
  );
  await t.pump();
  await t.tap(find.text('72 h'));
  await t.pump();
  await t.tap(find.byKey(const ValueKey('poner-en-subasta')));
  await t.pumpAndSettle();
  await t.tap(find.text('Sí, subastar'));
  await asentar(t);
}

void main() {
  testWidgets(
    'correcto: precio y plazo → un solo POST con la base, 72 h y clave',
    (t) async {
      final (a, envios) = await _montar(t);
      a.onPost(
        '/entregas/subastas',
        (s) => s.reply(201, subastaSintetica()),
        data: Matchers.any,
      );

      await _ponerYConfirmar(t, '1500');

      expect(envios, hasLength(1));
      expect(envios.single.path, '/entregas/subastas');
      expect(envios.single.clave, isNotNull);
      expect(envios.single.cuerpo, {
        'grupoId': _g,
        'cupoId': _cupo,
        'montoBase': {'monto': '1500.00', 'moneda': 'BOB'},
        'horas': 72,
      });
    },
  );

  testWidgets('límite: sin precio el botón está apagado y dice por qué', (
    t,
  ) async {
    final (_, envios) = await _montar(t);
    expect(find.text('Escribí el precio base para continuar.'), findsOneWidget);
    await t.tap(find.byKey(const ValueKey('poner-en-subasta')));
    await t.pumpAndSettle();
    expect(find.text('Sí, subastar'), findsNothing);
    expect(envios, isEmpty);
  });

  testWidgets('inválido: AP-CU136-01 se explica en voseo y lo escrito queda', (
    t,
  ) async {
    final (a, _) = await _montar(t);
    a.onPost(
      '/entregas/subastas',
      (s) => s.reply(422, {
        'codigo': 'AP-CU136-01',
        'mensaje': 'texto del backend que no se muestra',
        'trazaId': 'traza-sintetica',
      }),
      data: Matchers.any,
    );

    await _ponerYConfirmar(t, '1500');

    expect(
      find.textContaining('Este cupo no se puede subastar'),
      findsOneWidget,
    );
    expect(find.textContaining('texto del backend'), findsNothing);
    expect(find.text('1500'), findsOneWidget);
  });

  testWidgets(
    'con una subasta abierta sin ofertas se ve cómo va y se puede retirar',
    (t) async {
      final (a, envios) = await _montar(t, existentes: [subastaSintetica()]);
      a.onPost(
        '/entregas/subastas/$_sub/cancelacion',
        (s) => s.reply(200, subastaSintetica(estado: 'CANCELADA')),
      );
      expect(find.text('Vecinos del barrio'), findsOneWidget);
      expect(find.byKey(const ValueKey('poner-en-subasta')), findsNothing);

      await t.tap(find.byKey(const ValueKey('retirar-subasta')));
      await asentar(t);

      expect(envios.map((e) => e.path), [
        '/entregas/subastas/$_sub/cancelacion',
      ]);
    },
  );

  testWidgets(
    'con ofertas no se ofrece retirar: la subasta sigue hasta el cierre',
    (t) async {
      await _montar(
        t,
        existentes: [subastaSintetica(masAlto: '1800.00', pujas: 2)],
      );
      expect(find.text('Oferta más alta'), findsOneWidget);
      expect(find.byKey(const ValueKey('retirar-subasta')), findsNothing);
    },
  );
}
