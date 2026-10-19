import 'dart:convert';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/puertos/compartir.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_enlace_del_grupo.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_solicitudes_del_grupo.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_unirse_al_grupo.dart';
import 'package:aportaya_movil/proveedores/compartir.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// El enlace del grupo en pantalla (2026-10-19). El servidor y el «compartir» nativo son
/// dobles; el código y los datos del grupo son sintéticos.
const _grupo = 'c6900000-0000-4000-8000-0000000000e1';
const _codigo =
    'c6900000-0000-4000-8000-0000000000e2.'
    'abababababababababababababababababababababababababababababababab';

String _token({required bool habilitada}) {
  String parte(Object v) =>
      base64Url.encode(utf8.encode(jsonEncode(v))).replaceAll('=', '');
  return '${parte({'alg': 'none'})}.${parte({
    'sub': 'b8000000-0000-4000-8000-000000000088',
    'permisos': habilitada ? ['BILLETERA_VER'] : <String>[],
  })}.';
}

void main() {
  Future<_CompartirFalso> montar(
    WidgetTester tester,
    Widget pantalla,
    Dio dio, {
    bool habilitada = true,
  }) async {
    final sesion = Sesion(AlmacenEnMemoria());
    await sesion.abrir(acceso: _token(habilitada: habilitada));
    addTearDown(sesion.dispose);
    final compartir = _CompartirFalso();
    tester.view.physicalSize = const Size(412, 1600);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      ProviderScope(
        retry: (_, _) => null,
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
          dioProvider.overrideWithValue(dio),
          sesionProvider.overrideWithValue(sesion),
          compartirProvider.overrideWithValue(compartir),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: pantalla,
        ),
      ),
    );
    await asentar(tester);
    return compartir;
  }

  group('organizador', () {
    testWidgets(
      'correcto: crea el enlace, ve el QR y lo comparte por WhatsApp',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onPost(
          '/grupos/$_grupo/enlace-del-grupo',
          (s) => s.reply(201, {
            'enlaceId': _codigo.split('.').first,
            'codigo': _codigo,
            'expiraEn': '2026-10-26T20:00:00Z',
          }),
        );
        final compartir = await montar(
          tester,
          const PantallaEnlaceDelGrupo(
            grupoId: _grupo,
            nombre: 'Del barrio',
            appWeb: 'https://app.aportaya.161.97.85.216.sslip.io',
          ),
          dio,
        );
        expect(find.text('Crear enlace del grupo'), findsOneWidget);

        await tester.tap(find.text('Crear enlace del grupo'));
        await asentar(tester);

        expect(
          find.byKey(const ValueKey('qr-enlace-del-grupo')),
          findsOneWidget,
        );
        expect(find.textContaining('/g/$_codigo'), findsOneWidget);
        await tester.tap(find.text('Compartir por WhatsApp'));
        await asentar(tester);
        expect(compartir.texto, contains('/g/$_codigo'));
        expect(compartir.texto, contains('«Del barrio»'));
      },
    );

    testWidgets('inválido: quien no administra el grupo ve por qué no puede', (
      tester,
    ) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        '/grupos/$_grupo/enlace-del-grupo',
        (s) => s.reply(422, {
          'codigo': 'AP-CU69-07',
          'mensaje': 'x',
          'trazaId': 't',
        }),
      );
      await montar(tester, const PantallaEnlaceDelGrupo(grupoId: _grupo), dio);
      await tester.tap(find.text('Crear enlace del grupo'));
      await asentar(tester);
      expect(find.textContaining('solo quien lo organiza'), findsOneWidget);
      expect(find.byKey(const ValueKey('qr-enlace-del-grupo')), findsNothing);
    });

    testWidgets('límite: la solicitud muestra el nombre y se acepta', (
      tester,
    ) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador
        ..onGet(
          '/grupos/$_grupo/solicitudes',
          (s) => s.reply(200, [
            {
              'solicitudId': 'c6800000-0000-4000-8000-000000000001',
              'usuarioId': 'c6800000-0000-4000-8000-000000000002',
              'cuposSolicitados': 1,
              'nombreVisible': 'Rosa Q.',
              'porEnlace': true,
              'estado': 'PENDIENTE',
              'fechaSolicitud': '2026-10-19T20:00:00Z',
            },
          ]),
        )
        ..onPost(
          '/grupos/solicitudes/c6800000-0000-4000-8000-000000000001/decision',
          (s) => s.reply(200, {
            'solicitudId': 'c6800000-0000-4000-8000-000000000001',
            'estado': 'APROBADA',
          }),
          data: Matchers.any,
        );
      await montar(
        tester,
        const PantallaSolicitudesDelGrupo(grupoId: _grupo),
        dio,
      );
      expect(find.text('Rosa Q.'), findsOneWidget);
      expect(
        find.textContaining('Pidió con el enlace del grupo'),
        findsOneWidget,
      );
      await tester.tap(find.text('Aceptar'));
      await asentar(tester);
      expect(find.textContaining('ya tiene su cupo'), findsOneWidget);
    });
  });

  group('quien recibe el enlace', () {
    void detalle(DioAdapter adaptador, {int cupos = 2}) => adaptador.onPost(
      '/grupos/enlace-del-grupo/consultar',
      (s) => s.reply(200, {
        'grupoId': _grupo,
        'nombre': 'Pasanaku del barrio',
        'montoAporte': '500.00',
        'moneda': 'BOB',
        'periodicidad': 'MENSUAL',
        'cuposLibres': cupos,
        'expiraEn': '2026-10-26T20:00:00Z',
      }),
      data: Matchers.any,
    );

    testWidgets('correcto: ve el grupo y pide unirse', (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      detalle(adaptador);
      adaptador.onPost(
        '/grupos/enlace-del-grupo/solicitudes',
        (s) => s.reply(201, {
          'solicitudId': 'c6800000-0000-4000-8000-000000000009',
          'grupoId': _grupo,
          'estado': 'PENDIENTE',
        }),
        data: Matchers.any,
      );
      await montar(tester, const PantallaUnirseAlGrupo(codigo: _codigo), dio);
      expect(find.text('Pasanaku del barrio'), findsOneWidget);
      expect(find.text('Quedan 2 cupos libres'), findsOneWidget);
      await tester.tap(find.text('Pedir unirme'));
      await asentar(tester);
      expect(find.text('Listo, le avisamos al organizador'), findsOneWidget);
    });

    testWidgets('límite: sin la identidad verificada se la pide primero', (
      tester,
    ) async {
      final (:dio, :adaptador) = dioSimulado();
      detalle(adaptador);
      await montar(
        tester,
        const PantallaUnirseAlGrupo(codigo: _codigo),
        dio,
        habilitada: false,
      );
      expect(find.text('Verificar mi identidad'), findsOneWidget);
      expect(find.text('Pedir unirme'), findsNothing);
    });

    testWidgets('inválido: un enlace vencido lo dice y no deja pedir', (
      tester,
    ) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        '/grupos/enlace-del-grupo/consultar',
        (s) => s.reply(422, {
          'codigo': 'AP-CU69-11',
          'mensaje': 'x',
          'trazaId': 't',
        }),
        data: Matchers.any,
      );
      await montar(tester, const PantallaUnirseAlGrupo(codigo: _codigo), dio);
      expect(find.textContaining('ya no sirve'), findsOneWidget);
      expect(find.text('Pedir unirme'), findsNothing);
    });
  });
}

class _CompartirFalso implements Compartir {
  String? texto;

  @override
  Future<bool> porWhatsApp({
    required String telefono,
    required String texto,
  }) async {
    this.texto = texto;
    return true;
  }

  @override
  Future<bool> porSms({required String telefono, required String texto}) async {
    this.texto = texto;
    return true;
  }

  @override
  Future<bool> conOtraApp({required String texto}) async {
    this.texto = texto;
    return true;
  }
}
