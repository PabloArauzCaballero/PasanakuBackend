import 'dart:convert';

import 'package:aportaya_movil/pantallas/pasanaku/condiciones_del_traspaso.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_traspaso.dart';
import 'package:aportaya_movil/pantallas/pasanaku/tarjeta_de_cesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// CU-64 · traspasar el cupo. Datos SINTÉTICOS: ids de prueba y nombres inventados.
const _g = '11111111-1111-4111-8111-111111111111';
const _cupo = '22222222-2222-4222-8222-222222222222';
const _ana = '33333333-3333-4333-8333-333333333333';
const _post = '/grupos/$_g/cupos/$_cupo/traspasos';
const _motivo = 'Me mudo de ciudad y Ana sigue en el grupo';

Map<String, dynamic> _integrante(
  int n, {
  String? cupo,
  String? usuario,
  String? nombre,
  String estado = 'FIRMADO',
  bool yo = false,
  int? turno,
}) => {
  'numero': n,
  'cupoId': ?cupo,
  'usuarioId': usuario,
  'nombre': nombre,
  'estado': estado,
  'esOrganizador': false,
  'soyYo': yo,
  'turno': turno,
};

Map<String, dynamic> _detalle() => {
  'grupoId': _g,
  'nombre': 'Vecinos del barrio',
  'estado': 'CONFORMADO',
  'montoAporte': '200.00',
  'moneda': 'BOB',
  'periodicidad': 'MENSUAL',
  'cuposTotales': 4,
  'cuposOcupados': 3,
  'soyOrganizador': false,
  'debeFirmar': false,
  'puedeSortear': false,
  'integrantes': [
    _integrante(
      1,
      cupo: _cupo,
      usuario: 'yo',
      nombre: 'Yo',
      yo: true,
      turno: 3,
    ),
    _integrante(2, usuario: _ana, nombre: 'Ana', turno: 1),
    _integrante(3, usuario: 'beto', nombre: 'Beto', estado: 'PENDIENTE_FIRMA'),
    _integrante(4, estado: 'LIBRE'),
  ],
};

/// El cuerpo tal como viaja: el cliente generado lo manda ya serializado.
Map<String, dynamic> _cuerpo(Object? c) =>
    (c is String ? jsonDecode(c) : c) as Map<String, dynamic>;

typedef _Envio = ({String? clave, Object? cuerpo});

Future<(DioAdapter, List<_Envio>)> _montar(WidgetTester t) async {
  t.view.physicalSize = const Size(412, 2400);
  t.view.devicePixelRatio = 1;
  addTearDown(t.view.reset);
  final (:dio, :adaptador) = dioSimulado();
  final envios = <_Envio>[];
  dio.interceptors.insert(
    0,
    InterceptorsWrapper(
      onRequest: (o, h) {
        if (o.method == 'POST' && o.path == _post) {
          envios.add((
            clave: o.headers['Idempotency-Key'] as String?,
            cuerpo: o.data,
          ));
        }
        h.next(o);
      },
    ),
  );
  adaptador.onGet('/grupos/$_g/detalle', (s) => s.reply(200, _detalle()));
  await t.pumpWidget(
    conApp(
      const PantallaTraspaso(grupoId: _g, cupoId: _cupo),
      dio: dio,
    ),
  );
  await asentar(t);
  return (adaptador, envios);
}

Future<void> _hastaRevisar(WidgetTester t) async {
  await t.tap(find.text('Ana'));
  await t.pump();
  await t.tap(find.text('Continuar'));
  await t.pumpAndSettle();
  await t.enterText(find.byType(TextField), _motivo);
  await t.pump();
  await t.tap(find.text('Continuar'));
  await t.pumpAndSettle();
}

Future<void> _confirmar(WidgetTester t) async {
  await t.tap(find.text('Traspasar cupo'));
  await t.pumpAndSettle();
  await t.tap(find.text('Sí, traspasar'));
  await asentar(t);
}

void _responder(DioAdapter a, int estado, Object cuerpo) =>
    a.onPost(_post, (s) => s.reply(estado, cuerpo), data: Matchers.any);

Map<String, String> _rechazo(String codigo) => {
  'codigo': codigo,
  'mensaje': 'texto del backend que no se muestra',
  'trazaId': 'traza-sintetica',
};

void main() {
  testWidgets('elige solo entre integrantes que no soy yo, sin cupos libres', (
    t,
  ) async {
    await _montar(t);
    expect(find.text('Ana'), findsOneWidget);
    expect(find.text('Beto'), findsOneWidget);
    expect(find.text('Yo'), findsNothing);
    expect(find.text('Paso 1 de 3 · Quién entra'), findsOneWidget);
  });

  testWidgets(
    'correcto: confirma, envía UNA vez con clave y muestra el comprobante',
    (t) async {
      final (adaptador, envios) = await _montar(t);
      _responder(adaptador, 201, {
        'traspasoId': 'ae0661ac-a993-423a-8795-23965799a3e6',
      });
      await _hastaRevisar(t);
      expect(find.text('Tu turno se queda con el cupo'), findsOneWidget);
      expect(find.text('Tu deuda vencida se queda con vos'), findsOneWidget);
      expect(find.text('No hay precio ni pago'), findsOneWidget);
      await _confirmar(t);

      expect(envios, hasLength(1));
      expect(envios.single.clave, isNotEmpty);
      final cuerpo = _cuerpo(envios.single.cuerpo);
      expect(cuerpo['usuarioEntranteId'], _ana);
      expect(cuerpo['motivo'], _motivo);
      expect(cuerpo.containsKey('acuerdoId'), isFalse);
      expect(find.text('Cupo traspasado'), findsOneWidget);
      expect(find.text('Traspaso ae0661ac'), findsOneWidget);
      expect(find.text('Ir a mis grupos'), findsOneWidget);
    },
  );

  testWidgets('sin confirmar no se envía nada', (t) async {
    final (adaptador, envios) = await _montar(t);
    _responder(adaptador, 201, {'traspasoId': 'x'});
    await _hastaRevisar(t);
    await t.tap(find.text('Traspasar cupo'));
    await t.pumpAndSettle();
    expect(find.text('¿Traspasás tu cupo?'), findsOneWidget);
    await t.tap(find.text('Cancelar'));
    await t.pumpAndSettle();
    expect(envios, isEmpty);
    expect(find.text('Lo que se revisa al confirmar'), findsOneWidget);
  });

  testWidgets('doble toque: en la hoja y en la acción, un solo envío', (
    t,
  ) async {
    final (adaptador, envios) = await _montar(t);
    adaptador.onPost(
      _post,
      (s) => s.reply(201, {
        'traspasoId': 'x',
      }, delay: const Duration(milliseconds: 300)),
      data: Matchers.any,
    );
    await _hastaRevisar(t);
    await t.tap(find.text('Traspasar cupo'));
    // El segundo toque cae sobre la hoja que ya se abre: no abre otra.
    await t.tap(find.text('Traspasar cupo'), warnIfMissed: false);
    await t.pumpAndSettle();
    expect(find.text('¿Traspasás tu cupo?'), findsOneWidget);
    await t.tap(find.text('Sí, traspasar'));
    await t.tap(find.text('Sí, traspasar'), warnIfMissed: false);
    await t.pump();
    await t.tap(
      find.byKey(const ValueKey('accion-del-traspaso')),
      warnIfMissed: false,
    );
    await asentar(t);
    expect(envios, hasLength(1));
    expect(find.text('Cupo traspasado'), findsOneWidget);
  });

  const condiciones = [
    'Tus aportes están al día',
    'Quien entra tiene la verificación de identidad que pide el grupo',
    'Quien entra llega a la reputación mínima del grupo',
    'El grupo aprobó el traspaso en una votación',
  ];
  final casos = <String, (String, int?)>{
    'AP-CU64-01': ('Este cupo ya no se puede traspasar', null),
    'AP-CU64-02': ('Tenés aportes vencidos', 0),
    'AP-CU64-03': ('no tiene el nivel de verificación', 1),
    'AP-CU64-04': ('no llega a la reputación mínima', 2),
    'AP-CU64-05': ('aprobar este traspaso en una votación', 3),
  };
  for (final MapEntry(key: codigo, value: (texto, fila)) in casos.entries) {
    testWidgets('$codigo se muestra donde corresponde y conserva lo cargado', (
      t,
    ) async {
      final (adaptador, envios) = await _montar(t);
      _responder(adaptador, 422, _rechazo(codigo));
      await _hastaRevisar(t);
      await _confirmar(t);
      final mensaje = find.textContaining(texto);
      expect(mensaje, findsOneWidget);
      expect(find.textContaining('texto del backend'), findsNothing);
      if (fila == null) {
        expect(
          find.descendant(of: find.byType(TarjetaDeCesion), matching: mensaje),
          findsOneWidget,
        );
      } else {
        expect(
          find.descendant(
            of: find.byType(CondicionesDelTraspaso),
            matching: mensaje,
          ),
          findsOneWidget,
        );
        final y = t.getTopLeft(mensaje).dy;
        expect(y, greaterThan(t.getTopLeft(find.text(condiciones[fila])).dy));
        if (fila < 3) {
          expect(
            y,
            lessThan(t.getTopLeft(find.text(condiciones[fila + 1])).dy),
          );
        }
      }
      // Lo cargado sigue: la persona elegida está en la orden y el reintento lleva lo mismo.
      expect(
        find.descendant(
          of: find.byType(TarjetaDeCesion),
          matching: find.text('Ana'),
        ),
        findsOneWidget,
      );
      _responder(adaptador, 201, {'traspasoId': 'x'});
      await _confirmar(t);
      expect(envios, hasLength(2));
      expect(_cuerpo(envios.last.cuerpo)['motivo'], _motivo);
      expect(
        envios.last.clave,
        isNot(envios.first.clave),
        reason: 'tras un 422, otro intento',
      );
    });
  }

  testWidgets('sin señal: el reintento reusa la misma clave', (t) async {
    final (adaptador, envios) = await _montar(t);
    _responder(adaptador, 503, {
      'codigo': 'AP-GW-503',
      'trazaId': 'traza-sintetica',
    });
    await _hastaRevisar(t);
    await _confirmar(t);
    expect(
      find.textContaining('El servicio no está disponible'),
      findsOneWidget,
    );
    _responder(adaptador, 201, {'traspasoId': 'x'});
    await _confirmar(t);
    expect(envios, hasLength(2));
    expect(envios.last.clave, envios.first.clave);
  });

  testWidgets('403: se dice que la cuenta no tiene permiso', (t) async {
    final (adaptador, _) = await _montar(t);
    _responder(adaptador, 403, {
      'codigo': 'AP-AUTH-403',
      'trazaId': 'traza-sintetica',
    });
    await _hastaRevisar(t);
    await _confirmar(t);
    expect(
      find.textContaining('no tiene permiso para traspasar'),
      findsOneWidget,
    );
  });
}
