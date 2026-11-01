import 'dart:convert';

import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/insignia_de_etapa.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_firmar_reglamento.dart';
import 'package:aportaya_movil/pantallas/pasanaku/seccion_del_grupo.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// Etapa por integrante (H1.S4.M2) y versión nueva del reglamento tras iniciar
/// (H1.S7.M4), 2026-10-30. El servidor es un doble de `grupos.yaml`
/// (`consultarDetalleDelGrupo`, `firmarReglamentoDelGrupo`) y de `transparencia.yaml`
/// (`listarEtapas`). Nombres, ids y tramos son SINTÉTICOS (catálogo PROPUESTO del
/// plan, H1.S6); el nombre de la etapa viaja corto, como lo manda el servidor.
const _grupo = 'c6900000-0000-4000-8000-0000000000e7';
const _hash =
    'dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd';

Map<String, dynamic> _integrante(
  int n,
  String? nombre,
  String estado, {
  Map<String, dynamic>? etapa,
}) => {
  'numero': n,
  'cupoId': 'c6900000-0000-4000-8000-0000000000c$n',
  'nombre': nombre,
  'estado': estado,
  'esOrganizador': n == 1,
  'soyYo': n == 1,
  'etapa': ?etapa,
};

Map<String, dynamic> _detalle({
  bool debeFirmar = false,
  bool? pendiente,
  int? version,
  String estado = 'CONFORMADO',
}) => {
  'grupoId': _grupo,
  'nombre': 'Pasanaku de la feria',
  'estado': estado,
  'montoAporte': '300.00',
  'moneda': 'BOB',
  'periodicidad': 'MENSUAL',
  'cuposTotales': 3,
  'cuposOcupados': 2,
  'soyOrganizador': false,
  'debeFirmar': debeFirmar,
  'puedeSortear': false,
  'reglamentoPendienteDeAceptar': ?pendiente,
  'reglamento': {
    'texto': 'Reglamento con los números reales.',
    'hash': _hash,
    'version': ?version,
  },
  'integrantes': [
    _integrante(
      1,
      'Rosa Quispe',
      'FIRMADO',
      etapa: {'codigo': 'EN_MARCHA', 'nombre': 'En marcha'},
    ),
    _integrante(
      2,
      'Juan Mamani',
      'FIRMADO',
      etapa: {'codigo': 'PILAR', 'nombre': 'Pilar'},
    ),
    _integrante(3, null, 'LIBRE'),
  ],
};

Map<String, dynamic> _etapa(String codigo, String nombre, int orden, int d) => {
  'codigo': codigo,
  'nombre': nombre,
  'orden': orden,
  'desde': '$d.00',
  'hasta': '${d + 50}.00',
};

final _catalogo = [
  _etapa('RECUPERACION', 'En recuperación', 0, 0),
  _etapa('INICIAL', 'Inicial', 1, 500),
  _etapa('EN_MARCHA', 'En marcha', 2, 550),
  _etapa('PILAR', 'Pilar', 10, 980),
];

Future<List<String>> _montar(
  WidgetTester tester,
  Widget pantalla,
  Dio dio,
) async {
  tester.view.physicalSize = const Size(412, 2000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  final visitadas = <String>[];
  final enrutador = GoRouter(
    routes: [
      GoRoute(
        path: '/',
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
      ],
      child: MaterialApp.router(
        theme: temaDesde(Tokens.claro, Brightness.light),
        routerConfig: enrutador,
      ),
    ),
  );
  await asentar(tester);
  return visitadas;
}

void main() {
  group('etapa en la tarjeta de integrante', () {
    testWidgets('correcto: insignia y «Etapa N · Nombre», nunca el puntaje', (
      tester,
    ) async {
      final semantica = tester.ensureSemantics();
      final (:dio, :adaptador) = dioSimulado();
      adaptador
        ..onGet('/grupos/$_grupo/detalle', (s) => s.reply(200, _detalle()))
        ..onGet('/reputacion/etapas', (s) => s.reply(200, _catalogo));
      await _montar(tester, const SeccionDelGrupo(grupoId: _grupo), dio);

      expect(find.text('Etapa 2 · En marcha'), findsOneWidget);
      expect(find.text('Etapa 10 · Pilar'), findsOneWidget);
      expect(
        find.bySemanticsLabel('Etapa de reputación: Etapa 2, En marcha'),
        findsOneWidget,
      );
      expect(
        find.bySemanticsLabel('Etapa de reputación: Etapa 10, Pilar'),
        findsOneWidget,
      );
      // Dos insignias: el cupo LIBRE no tiene etapa.
      expect(find.byType(InsigniaDeEtapa), findsNWidgets(2));
      expect(find.textContaining('puntos'), findsNothing);
      expect(
        find.textContaining(RegExp('arena', caseSensitive: false)),
        findsNothing,
      );
      expect(tester.takeException(), isNull);
      semantica.dispose();
    });

    testWidgets(
      'límite: sin catálogo se ve el nombre, sin número ni insignia',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador
          ..onGet('/grupos/$_grupo/detalle', (s) => s.reply(200, _detalle()))
          ..onGet(
            '/reputacion/etapas',
            (s) => s.reply(503, {'codigo': 'AP-GEN-503', 'trazaId': 't'}),
          );
        await _montar(tester, const SeccionDelGrupo(grupoId: _grupo), dio);

        expect(find.text('En marcha'), findsOneWidget);
        expect(find.textContaining('Etapa 2'), findsNothing);
        expect(find.byType(InsigniaDeEtapa), findsNothing);
        expect(find.text('Rosa Quispe'), findsOneWidget);
      },
    );
  });

  group('versión nueva del reglamento', () {
    testWidgets('correcto: el aviso lleva a la pantalla de firma', (
      tester,
    ) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador
        ..onGet(
          '/grupos/$_grupo/detalle',
          (s) => s.reply(200, _detalle(pendiente: true, version: 2)),
        )
        ..onGet('/reputacion/etapas', (s) => s.reply(200, _catalogo));
      final visitadas = await _montar(
        tester,
        const SeccionDelGrupo(grupoId: _grupo),
        dio,
      );

      expect(
        find.text('Firmá la versión nueva del reglamento'),
        findsOneWidget,
      );
      expect(find.text('Te falta firmar el reglamento'), findsNothing);
      await tester.tap(find.text('Leer y firmar la versión nueva'));
      await tester.pumpAndSettle();
      expect(visitadas, ['/pasanaku/grupos/$_grupo/reglamento']);
    });

    testWidgets('límite: sin nada que aceptar no hay aviso', (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador
        ..onGet(
          '/grupos/$_grupo/detalle',
          (s) => s.reply(200, _detalle(pendiente: false)),
        )
        ..onGet('/reputacion/etapas', (s) => s.reply(200, _catalogo));
      await _montar(tester, const SeccionDelGrupo(grupoId: _grupo), dio);

      expect(find.text('Firmá la versión nueva del reglamento'), findsNothing);
      expect(find.byKey(const ValueKey('firmar-version-nueva')), findsNothing);
    });

    testWidgets('correcto: en la firma se ve la versión y se manda su hash', (
      tester,
    ) async {
      final (:dio, :adaptador) = dioSimulado();
      Object? cuerpo;
      adaptador
        ..onGet(
          '/grupos/$_grupo/detalle',
          (s) => s.reply(200, _detalle(pendiente: true, version: 2)),
        )
        ..onPost('/grupos/$_grupo/reglamento/firma', (s) {
          s.reply(200, {
            'participanteId': 'c6900000-0000-4000-8000-0000000000b7',
            'estadoDelGrupo': 'CONFORMADO',
            'grupoConformado': true,
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

      expect(find.text('Versión 2 del reglamento'), findsOneWidget);
      expect(
        find.text('Firmá la versión nueva del reglamento'),
        findsOneWidget,
      );
      final boton = find.widgetWithText(Boton, 'Firmar la versión nueva');
      expect(tester.widget<Boton>(boton).onPressed, isNull);

      await tester.tap(find.text('Leí y acepto este reglamento'));
      await tester.pump();
      await tester.tap(boton);
      await asentar(tester);
      expect(jsonEncode(cuerpo), contains(_hash));
      expect(
        find.text('Listo, aceptaste la versión nueva del reglamento.'),
        findsOneWidget,
      );
      expect(find.textContaining('el grupo quedó completo'), findsNothing);
    });

    testWidgets(
      'inválido: si la versión cambió, se dice, se trae la vigente y se firma esa',
      (tester) async {
        // El servidor publica la versión 3 mientras la persona lee la 2: la primera
        // lectura trae la 2 y las siguientes, la 3 (otro hash, otro texto).
        const hashNuevo =
            'eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee';
        var lecturas = 0;
        final cuerpos = <String>[];
        final (:dio, :adaptador) = dioSimulado();
        adaptador
          ..onGet('/grupos/$_grupo/detalle', (s) {
            s.reply(200, (RequestOptions _) {
              lecturas++;
              if (lecturas == 1) return _detalle(pendiente: true, version: 2);
              final d = _detalle(pendiente: true, version: 3);
              d['reglamento'] = {
                'texto': 'Reglamento de la versión 3.',
                'hash': hashNuevo,
                'version': 3,
              };
              return d;
            });
          })
          ..onPost(
            '/grupos/$_grupo/reglamento/firma',
            (s) => s.reply(422, {
              'codigo': 'AP-CU69-05',
              'mensaje': 'texto del backend que la app no muestra',
              'trazaId': 'traza-refirma',
            }),
            data: Matchers.any,
          );
        dio.interceptors.add(
          InterceptorsWrapper(
            onRequest: (o, h) {
              if (o.method == 'POST') cuerpos.add(jsonEncode(o.data));
              h.next(o);
            },
          ),
        );
        await _montar(
          tester,
          const PantallaFirmarReglamento(grupoId: _grupo),
          dio,
        );
        await tester.tap(find.text('Leí y acepto este reglamento'));
        await tester.pump();
        await tester.tap(find.text('Firmar la versión nueva'));
        await asentar(tester);

        // El mensaje es el de la firma, no el del catálogo general (invitación).
        expect(
          find.text(
            'El reglamento cambió mientras lo leías. Te mostramos la versión '
            'vigente: leela y volvé a firmar.',
          ),
          findsOneWidget,
        );
        expect(find.textContaining('invitación'), findsNothing);
        expect(find.textContaining('texto del backend'), findsNothing);
        expect(
          find.text('Listo, aceptaste la versión nueva del reglamento.'),
          findsNothing,
        );
        // Se volvió a leer el detalle: se ve la versión vigente y hay que aceptarla
        // de nuevo (la casilla se apagó: aceptó otro texto).
        expect(find.text('Versión 3 del reglamento'), findsOneWidget);
        expect(find.text('Reglamento de la versión 3.'), findsOneWidget);
        final boton = find.widgetWithText(Boton, 'Firmar la versión nueva');
        expect(tester.widget<Boton>(boton).onPressed, isNull);

        await tester.tap(find.text('Leí y acepto este reglamento'));
        await tester.pump();
        await tester.tap(boton);
        await asentar(tester);
        expect(cuerpos, hasLength(2));
        expect(cuerpos.first, contains(_hash));
        expect(cuerpos.last, contains(hashNuevo));
        expect(cuerpos.last, isNot(contains(_hash)));
      },
    );

    testWidgets(
      'inválido: AP-CU69-13 dice que no hay nada que firmar, sin texto ajeno',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador
          ..onGet(
            '/grupos/$_grupo/detalle',
            (s) => s.reply(200, _detalle(pendiente: true, version: 2)),
          )
          ..onPost(
            '/grupos/$_grupo/reglamento/firma',
            (s) => s.reply(422, {'codigo': 'AP-CU69-13', 'trazaId': 't-13'}),
            data: Matchers.any,
          );
        await _montar(
          tester,
          const PantallaFirmarReglamento(grupoId: _grupo),
          dio,
        );
        await tester.tap(find.text('Leí y acepto este reglamento'));
        await tester.pump();
        await tester.tap(find.text('Firmar la versión nueva'));
        await asentar(tester);
        expect(
          find.text(
            'No tenés nada que firmar en este grupo: tu firma ya está registrada.',
          ),
          findsOneWidget,
        );
      },
    );

    testWidgets('sin nada que firmar el botón queda apagado', (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        '/grupos/$_grupo/detalle',
        (s) => s.reply(200, _detalle(pendiente: false)),
      );
      await _montar(
        tester,
        const PantallaFirmarReglamento(grupoId: _grupo),
        dio,
      );
      await tester.tap(find.text('Leí y acepto este reglamento'));
      await tester.pump();
      final boton = find.widgetWithText(Boton, 'Firmar el reglamento');
      expect(tester.widget<Boton>(boton).onPressed, isNull);
    });
  });
}
