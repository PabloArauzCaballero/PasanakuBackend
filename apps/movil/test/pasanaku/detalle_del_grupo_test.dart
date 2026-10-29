import 'dart:convert';
import 'dart:math' as math;

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_firmar_reglamento.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_sorteo.dart';
import 'package:aportaya_movil/pantallas/pasanaku/ruleta_del_sorteo.dart';
import 'package:aportaya_movil/pantallas/pasanaku/seccion_del_grupo.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// El grupo por dentro (2026-10-20): resumen y tarjetas, firma del reglamento y sorteo
/// con la ruleta. El servidor es un doble; nombres y códigos son sintéticos.
const _grupo = 'c6900000-0000-4000-8000-0000000000f1';
const _hash =
    'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc';
const _cupos = [
  'c6900000-0000-4000-8000-0000000000a1',
  'c6900000-0000-4000-8000-0000000000a2',
  'c6900000-0000-4000-8000-0000000000a3',
];

Map<String, dynamic> _integrante(
  int n,
  String? nombre,
  String estado, {
  bool organiza = false,
  bool yo = false,
  int? turno,
}) => {
  'numero': n,
  'cupoId': _cupos[n - 1],
  'nombre': nombre,
  'estado': estado,
  'esOrganizador': organiza,
  'soyYo': yo,
  'turno': turno,
};

Map<String, dynamic> _detalle({
  String estado = 'ABIERTO_A_INSCRIPCION',
  bool organizador = true,
  bool debeFirmar = false,
  bool puedeSortear = false,
  int ocupados = 2,
  Map<String, dynamic>? sorteo,
  List<Map<String, dynamic>>? integrantes,
}) => {
  'grupoId': _grupo,
  'nombre': 'Pasanaku del barrio',
  'estado': estado,
  'montoAporte': '500.00',
  'moneda': 'BOB',
  'periodicidad': 'MENSUAL',
  'cuposTotales': 3,
  'cuposOcupados': ocupados,
  'soyOrganizador': organizador,
  'debeFirmar': debeFirmar,
  'puedeSortear': puedeSortear,
  'reglamento': {'texto': 'Reglamento de prueba.', 'hash': _hash},
  'sorteo': ?sorteo,
  'integrantes':
      integrantes ??
      [
        _integrante(1, 'Rosa Quispe', 'FIRMADO', organiza: true, yo: true),
        _integrante(2, 'Juan Mamani', 'PENDIENTE_FIRMA'),
        _integrante(3, null, 'LIBRE'),
      ],
};

String _token() {
  String parte(Object v) =>
      base64Url.encode(utf8.encode(jsonEncode(v))).replaceAll('=', '');
  return '${parte({'alg': 'none'})}.${parte({
    'sub': 'b8000000-0000-4000-8000-000000000088',
    'permisos': ['BILLETERA_VER'],
  })}.';
}

Future<List<String>> _montar(
  WidgetTester tester,
  Widget pantalla,
  Dio dio, {
  bool quieto = false,
}) async {
  final sesion = Sesion(AlmacenEnMemoria());
  await sesion.abrir(acceso: _token());
  addTearDown(sesion.dispose);
  tester.view.physicalSize = const Size(412, 2000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  final visitadas = <String>[];
  final enrutador = GoRouter(
    routes: [
      GoRoute(
        path: '/',
        // La sección va dentro de una pantalla con scroll; las pantallas traen el suyo.
        builder: (c, s) => pantalla is SeccionDelGrupo
            ? Scaffold(body: SingleChildScrollView(child: pantalla))
            : pantalla,
      ),
      GoRoute(
        path: '/pasanaku/grupos/:id/:accion',
        builder: (c, s) {
          visitadas.add(s.uri.path);
          return const Scaffold(body: Text('destino'));
        },
      ),
    ],
  );
  await tester.pumpWidget(
    ProviderScope(
      retry: (_, _) => null,
      overrides: [
        almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
        dioProvider.overrideWithValue(dio),
        sesionProvider.overrideWithValue(sesion),
      ],
      child: MediaQuery(
        data: MediaQueryData(disableAnimations: quieto),
        child: MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
          routerConfig: enrutador,
        ),
      ),
    ),
  );
  await asentar(tester);
  return visitadas;
}

void main() {
  group('el grupo por dentro', () {
    testWidgets(
      'correcto: una tarjeta por cupo, con el libre a la vista, e invitar para el organizador',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onGet(
          '/grupos/$_grupo/detalle',
          (s) => s.reply(200, _detalle()),
        );
        await _montar(tester, const SeccionDelGrupo(grupoId: _grupo), dio);
        expect(find.text('Rosa Quispe'), findsOneWidget);
        expect(find.text('Juan Mamani'), findsOneWidget);
        expect(find.text('Cupo libre'), findsOneWidget);
        expect(find.text('Falta firmar'), findsOneWidget);
        expect(find.text('2 de 3 cupos ocupados'), findsOneWidget);
        expect(find.text('Invitar a alguien a mi grupo'), findsOneWidget);
        expect(find.text('Iniciar sorteo'), findsNothing);
      },
    );

    testWidgets(
      'correcto: «Ver quién pidió unirse» va debajo de invitar y abre las solicitudes',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onGet(
          '/grupos/$_grupo/detalle',
          (s) => s.reply(200, _detalle()),
        );
        final visitadas = await _montar(
          tester,
          const SeccionDelGrupo(grupoId: _grupo),
          dio,
        );
        final invitar = tester.getTopLeft(
          find.text('Invitar a alguien a mi grupo'),
        );
        final solicitudes = tester.getTopLeft(
          find.text('Ver quién pidió unirse'),
        );
        expect(solicitudes.dy, greaterThan(invitar.dy));
        await tester.tap(find.text('Ver quién pidió unirse'));
        await asentar(tester);
        expect(visitadas, ['/pasanaku/grupos/$_grupo/solicitudes']);
      },
    );

    testWidgets(
      'límite: con todos firmados el organizador ve «Iniciar sorteo»',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onGet(
          '/grupos/$_grupo/detalle',
          (s) => s.reply(
            200,
            _detalle(estado: 'CONFORMADO', puedeSortear: true, ocupados: 3),
          ),
        );
        final visitadas = await _montar(
          tester,
          const SeccionDelGrupo(grupoId: _grupo),
          dio,
        );
        expect(find.text('Invitar a alguien a mi grupo'), findsNothing);
        expect(find.text('Ver quién pidió unirse'), findsNothing);
        await tester.tap(find.text('Iniciar sorteo'));
        await asentar(tester);
        expect(visitadas, ['/pasanaku/grupos/$_grupo/sorteo']);
      },
    );

    testWidgets(
      'quien tiene que firmar lo ve primero, y la tarjeta abre su información sin teléfono',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onGet(
          '/grupos/$_grupo/detalle',
          (s) => s.reply(200, _detalle(organizador: false, debeFirmar: true)),
        );
        await _montar(tester, const SeccionDelGrupo(grupoId: _grupo), dio);
        expect(find.text('Te falta firmar el reglamento'), findsOneWidget);
        expect(find.text('Invitar a alguien a mi grupo'), findsNothing);
        expect(find.text('Ver quién pidió unirse'), findsNothing);
        await tester.tap(find.text('Juan Mamani'));
        await asentar(tester);
        expect(find.text('Sin turno todavía'), findsOneWidget);
        expect(find.textContaining('+591'), findsNothing);
        expect(find.textContaining('@'), findsNothing);
      },
    );

    testWidgets(
      'inválido: si el servidor no deja ver el grupo, se ofrece reintentar',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onGet(
          '/grupos/$_grupo/detalle',
          (s) => s.reply(422, {
            'codigo': 'AP-CU69-14',
            'mensaje': 'x',
            'trazaId': 't',
          }),
        );
        await _montar(tester, const SeccionDelGrupo(grupoId: _grupo), dio);
        expect(find.text('Rosa Quispe'), findsNothing);
        expect(find.text('Volver a intentar'), findsOneWidget);
      },
    );
  });

  group('firmar el reglamento', () {
    testWidgets(
      'correcto: sin la casilla no firma; con la casilla firma ese texto',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        Object? cuerpo;
        adaptador
          ..onGet(
            '/grupos/$_grupo/detalle',
            (s) => s.reply(200, _detalle(organizador: false, debeFirmar: true)),
          )
          ..onPost('/grupos/$_grupo/reglamento/firma', (s) {
            s.reply(200, {
              'participanteId': 'c6900000-0000-4000-8000-0000000000b1',
              'estadoDelGrupo': 'ABIERTO_A_INSCRIPCION',
              'grupoConformado': false,
            });
          }, data: Matchers.any);
        dio.interceptors.add(
          InterceptorsWrapper(
            onRequest: (o, h) {
              if (o.method == 'POST') cuerpo = o.data;
              h.next(o);
            },
          ),
        );
        await _montar(
          tester,
          const PantallaFirmarReglamento(grupoId: _grupo),
          dio,
        );
        expect(find.text('Reglamento de prueba.'), findsOneWidget);
        await tester.tap(find.text('Firmar el reglamento'));
        await asentar(tester);
        expect(cuerpo, isNull, reason: 'sin aceptar no sale nada');

        await tester.tap(find.text('Leí y acepto este reglamento'));
        await tester.pump();
        await tester.tap(find.text('Firmar el reglamento'));
        await asentar(tester);
        expect(jsonEncode(cuerpo), contains(_hash));
        expect(find.text('Listo, firmaste el reglamento.'), findsOneWidget);
      },
    );

    testWidgets('inválido: si el reglamento cambió, se dice y no se cierra', (
      tester,
    ) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador
        ..onGet(
          '/grupos/$_grupo/detalle',
          (s) => s.reply(200, _detalle(organizador: false, debeFirmar: true)),
        )
        ..onPost(
          '/grupos/$_grupo/reglamento/firma',
          (s) => s.reply(422, {
            'codigo': 'AP-CU69-05',
            'mensaje': 'x',
            'trazaId': 't',
          }),
          data: Matchers.any,
        );
      await _montar(
        tester,
        const PantallaFirmarReglamento(grupoId: _grupo),
        dio,
      );
      await tester.tap(find.text('Leí y acepto este reglamento'));
      await tester.pump();
      await tester.tap(find.text('Firmar el reglamento'));
      await asentar(tester);
      expect(find.text('Reglamento de prueba.'), findsOneWidget);
      expect(find.text('Listo, firmaste el reglamento.'), findsNothing);
    });
  });

  group('sorteo con la ruleta', () {
    Map<String, dynamic> completo({
      List<Map<String, dynamic>>? integrantes,
      Map<String, dynamic>? sorteo,
    }) => _detalle(
      estado: 'CONFORMADO',
      puedeSortear: sorteo == null,
      ocupados: 3,
      sorteo: sorteo,
      integrantes:
          integrantes ??
          [
            _integrante(1, 'Rosa Quispe', 'FIRMADO', organiza: true, yo: true),
            _integrante(2, 'Juan Mamani', 'FIRMADO'),
            _integrante(3, 'Ana Choque', 'FIRMADO'),
          ],
    );

    for (final quieto in [false, true]) {
      testWidgets(
        'correcto${quieto ? ' (movimiento reducido)' : ''}: girar muestra el orden que dio el servidor',
        (tester) async {
          final (:dio, :adaptador) = dioSimulado();
          adaptador
            ..onGet('/grupos/$_grupo/detalle', (s) => s.reply(200, completo()))
            ..onPost(
              '/grupos/$_grupo/sorteo',
              (s) => s.reply(201, {
                'sorteoId': 'c6900000-0000-4000-8000-0000000000c1',
                'hashSemilla': _hash,
                'algoritmo': 'FISHER_YATES_SHA256',
              }),
            )
            ..onPost(
              '/grupos/$_grupo/sorteo/revelacion',
              (s) => s.reply(200, {
                'sorteoId': 'c6900000-0000-4000-8000-0000000000c1',
                'verificado': true,
                'cuposEnOrden': [_cupos[2], _cupos[0], _cupos[1]],
              }),
              data: Matchers.any,
            );
          await _montar(
            tester,
            const PantallaSorteo(grupoId: _grupo),
            dio,
            quieto: quieto,
          );
          await tester.tap(find.text('Girar la ruleta'));
          await tester.pumpAndSettle(const Duration(milliseconds: 100));
          expect(find.text('1.º  Ana Choque'), findsOneWidget);
          // Rosa es quien mira: su fila dice «Tú».
          expect(find.text('2.º  Tú'), findsOneWidget);
          expect(find.text('2.º  Rosa Quispe'), findsNothing);
          expect(find.text('3.º  Juan Mamani'), findsOneWidget);
          expect(find.text('Girar la ruleta'), findsNothing);
          // Se vacía el reloj antes de desmontar: el último giro deja su cuadro final.
          await tester.pump(const Duration(seconds: 5));
        },
      );
    }

    testWidgets('límite: con el sorteo ya hecho se ve el orden sin girar', (
      tester,
    ) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        '/grupos/$_grupo/detalle',
        (s) => s.reply(
          200,
          completo(
            sorteo: {
              'sorteoId': 'c6900000-0000-4000-8000-0000000000c1',
              'estado': 'REVELADO',
            },
            integrantes: [
              _integrante(1, 'Rosa Quispe', 'FIRMADO', turno: 2),
              _integrante(2, 'Juan Mamani', 'FIRMADO', turno: 3),
              _integrante(3, 'Ana Choque', 'FIRMADO', turno: 1),
            ],
          ),
        ),
      );
      await _montar(tester, const PantallaSorteo(grupoId: _grupo), dio);
      expect(find.text('Girar la ruleta'), findsNothing);
      expect(find.text('1.º  Ana Choque'), findsOneWidget);
    });

    testWidgets(
      'inválido: si el servidor rechaza el sorteo, se dice y se puede reintentar',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador
          ..onGet('/grupos/$_grupo/detalle', (s) => s.reply(200, completo()))
          ..onPost(
            '/grupos/$_grupo/sorteo',
            (s) => s.reply(422, {
              'codigo': 'AP-CU60-05',
              'mensaje': 'x',
              'trazaId': 't',
            }),
          );
        await _montar(
          tester,
          const PantallaSorteo(grupoId: _grupo),
          dio,
          quieto: true,
        );
        await tester.tap(find.text('Girar la ruleta'));
        await asentar(tester);
        expect(find.textContaining('.º  '), findsNothing);
        expect(find.text('Girar la ruleta'), findsOneWidget);
      },
    );

    test('la ruleta deja el segmento pedido bajo la aguja', () {
      for (var i = 0; i < 7; i++) {
        final angulo = RuletaDelSorteo.anguloPara(i, 7, desde: 1.3);
        const paso = 2 * math.pi / 7;
        final centro = (angulo + (i + 0.5) * paso) % (2 * math.pi);
        expect(math.min(centro, 2 * math.pi - centro), lessThan(1e-9));
        expect(angulo, greaterThan(1.3));
      }
    });
  });
}
