import 'dart:convert';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/puertos/compartir.dart';
import 'package:aportaya_movil/dominio/puertos/lector_de_qr_en_imagen.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_enlace_del_grupo.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_solicitudes_del_grupo.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_unirse_a_un_grupo.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_unirse_al_grupo.dart';
import 'package:aportaya_movil/proveedores/compartir.dart';
import 'package:aportaya_movil/proveedores/imagen_del_qr.dart';
import 'package:aportaya_movil/proveedores/lector_de_qr_en_imagen.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// El enlace del grupo en pantalla (2026-10-19, rehecho 2026-10-20: invitar solo por QR
/// o por enlace, y unirse subiendo la imagen del QR o pegando el enlace). El servidor,
/// el «compartir» nativo y el lector de imágenes son dobles; los datos son sintéticos.
const _grupo = 'c6900000-0000-4000-8000-0000000000e1';
const _codigo =
    'c6900000-0000-4000-8000-0000000000e2.'
    'abababababababababababababababababababababababababababababababab';
const _web = 'https://app.aportaya.161.97.85.216.sslip.io';

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
    LectorDeQrEnImagen? lector,
    List<String>? visitadas,
  }) async {
    final sesion = Sesion(AlmacenEnMemoria());
    await sesion.abrir(acceso: _token(habilitada: habilitada));
    addTearDown(sesion.dispose);
    final compartir = _CompartirFalso();
    tester.view.physicalSize = const Size(412, 1800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final enrutador = GoRouter(
      routes: [
        GoRoute(path: '/', builder: (c, s) => pantalla),
        GoRoute(
          path: '/g/:codigo',
          builder: (c, s) {
            visitadas?.add(s.uri.path);
            return const Scaffold(body: Text('destino-unirse-al-grupo'));
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
          compartirProvider.overrideWithValue(compartir),
          imagenDelQrProvider.overrideWithValue(
            (contenido, {titulo}) async => '/cache/compartir/qr-del-grupo.png',
          ),
          if (lector != null)
            lectorDeQrEnImagenProvider.overrideWithValue(lector),
        ],
        child: MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
          routerConfig: enrutador,
        ),
      ),
    );
    await asentar(tester);
    return compartir;
  }

  group('organizador', () {
    void enlaceCreado(DioAdapter adaptador) => adaptador
      ..onPost(
        '/grupos/$_grupo/enlace-del-grupo',
        (s) => s.reply(201, {
          'enlaceId': _codigo.split('.').first,
          'codigo': _codigo,
          'expiraEn': '2026-10-26T20:00:00Z',
        }),
      )
      ..onPost(
        '/grupos/enlace-del-grupo/consultar',
        (s) => s.reply(200, {
          'grupoId': _grupo,
          'nombre': 'Del barrio',
          'montoAporte': '500.00',
          'moneda': 'BOB',
          'periodicidad': 'MENSUAL',
          'cuposLibres': 2,
          'expiraEn': '2026-10-26T20:00:00Z',
        }),
        data: Matchers.any,
      );

    testWidgets(
      'correcto: al entrar se crea el enlace y se comparte el QR como imagen',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        enlaceCreado(adaptador);
        final compartir = await montar(
          tester,
          const PantallaEnlaceDelGrupo(
            grupoId: _grupo,
            nombre: 'Del barrio',
            appWeb: _web,
          ),
          dio,
        );

        // Sin tocar «Crear»: el enlace ya está y se ven solo las dos formas.
        expect(
          find.byKey(const ValueKey('qr-enlace-del-grupo')),
          findsOneWidget,
        );
        expect(find.text('Por QR'), findsWidgets);
        expect(find.text('Por enlace'), findsWidgets);
        expect(find.text('Invitar a un número en particular'), findsNothing);

        await tester.tap(find.text('Compartir QR por WhatsApp'));
        await asentar(tester);
        expect(compartir.imagenRuta, '/cache/compartir/qr-del-grupo.png');
        expect(compartir.texto, contains('Unirse a un grupo'));
        expect(compartir.texto, contains('«Del barrio»'));
      },
    );

    testWidgets('límite: el enlace se comparte como texto y se puede copiar', (
      tester,
    ) async {
      final (:dio, :adaptador) = dioSimulado();
      enlaceCreado(adaptador);
      final compartir = await montar(
        tester,
        const PantallaEnlaceDelGrupo(grupoId: _grupo, appWeb: _web),
        dio,
      );
      expect(find.textContaining('/g/$_codigo'), findsOneWidget);
      await tester.tap(find.text('Compartir enlace por WhatsApp'));
      await asentar(tester);
      expect(compartir.texto, contains('$_web/g/$_codigo'));
      expect(compartir.imagenRuta, isNull);
    });

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
      expect(find.textContaining('solo quien lo organiza'), findsOneWidget);
      expect(find.byKey(const ValueKey('qr-enlace-del-grupo')), findsNothing);
      // Queda el botón para reintentar, sin ruedita girando para siempre.
      expect(find.text('Crear enlace del grupo'), findsOneWidget);
    });

    testWidgets('la solicitud muestra solo el nombre completo, y se acepta', (
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
              'nombreVisible': 'Rosa Quispe',
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
      expect(find.text('Rosa Quispe'), findsOneWidget);
      // Sin teléfono ni correo (decisión del dueño 2026-10-20).
      expect(find.textContaining('+591'), findsNothing);
      expect(find.textContaining('@'), findsNothing);
      expect(
        find.textContaining('Pidió con el enlace del grupo'),
        findsOneWidget,
      );
      await tester.tap(find.text('Aceptar'));
      await asentar(tester);
      expect(find.textContaining('ya tiene su cupo'), findsOneWidget);
    });
  });

  group('unirse a un grupo con el QR o el enlace', () {
    testWidgets('correcto: la imagen del QR del grupo lleva a pedir unirse', (
      tester,
    ) async {
      final (:dio, adaptador: _) = dioSimulado();
      final visitadas = <String>[];
      await montar(
        tester,
        const PantallaUnirseAUnGrupo(appWeb: _web),
        dio,
        lector: const _LectorFalso(LecturaConQr('$_web/g/$_codigo')),
        visitadas: visitadas,
      );
      await tester.tap(find.text('Subir la imagen del QR'));
      await asentar(tester);
      expect(find.text('destino-unirse-al-grupo'), findsOneWidget);
      expect(visitadas, ['/g/$_codigo']);
    });

    testWidgets('límite: una imagen sin QR lo dice y no navega', (
      tester,
    ) async {
      final (:dio, adaptador: _) = dioSimulado();
      final visitadas = <String>[];
      await montar(
        tester,
        const PantallaUnirseAUnGrupo(appWeb: _web),
        dio,
        lector: const _LectorFalso(LecturaSinQr()),
        visitadas: visitadas,
      );
      await tester.tap(find.text('Subir la imagen del QR'));
      await asentar(tester);
      expect(find.textContaining('No encontramos un QR'), findsOneWidget);
      expect(visitadas, isEmpty);
    });

    testWidgets('límite: cerrar la galería sin elegir no muestra error', (
      tester,
    ) async {
      final (:dio, adaptador: _) = dioSimulado();
      await montar(
        tester,
        const PantallaUnirseAUnGrupo(appWeb: _web),
        dio,
        lector: const _LectorFalso(LecturaCancelada()),
      );
      await tester.tap(find.text('Subir la imagen del QR'));
      await asentar(tester);
      expect(find.textContaining('No encontramos'), findsNothing);
      expect(find.textContaining('no es de un grupo'), findsNothing);
    });

    testWidgets('inválido: un QR de otro sitio no se acepta', (tester) async {
      final (:dio, adaptador: _) = dioSimulado();
      final visitadas = <String>[];
      await montar(
        tester,
        const PantallaUnirseAUnGrupo(appWeb: _web),
        dio,
        lector: const _LectorFalso(
          LecturaConQr('https://otro-sitio.com/g/$_codigo'),
        ),
        visitadas: visitadas,
      );
      await tester.tap(find.text('Subir la imagen del QR'));
      await asentar(tester);
      expect(find.text('Ese QR no es de un grupo de Aporteo.'), findsOneWidget);
      expect(visitadas, isEmpty);
    });

    testWidgets('correcto: el enlace pegado lleva a pedir unirse', (
      tester,
    ) async {
      final (:dio, adaptador: _) = dioSimulado();
      final visitadas = <String>[];
      await montar(
        tester,
        const PantallaUnirseAUnGrupo(appWeb: _web),
        dio,
        visitadas: visitadas,
      );
      await tester.enterText(find.byType(TextField), '  $_web/g/$_codigo ');
      await tester.tap(find.text('Buscar el grupo'));
      await asentar(tester);
      expect(visitadas, ['/g/$_codigo']);
    });

    testWidgets('inválido: un enlace vacío o ajeno se marca en el campo', (
      tester,
    ) async {
      final (:dio, adaptador: _) = dioSimulado();
      final visitadas = <String>[];
      await montar(
        tester,
        const PantallaUnirseAUnGrupo(appWeb: _web),
        dio,
        visitadas: visitadas,
      );
      await tester.tap(find.text('Buscar el grupo'));
      await asentar(tester);
      expect(find.textContaining('Pegá el enlace'), findsOneWidget);
      await tester.enterText(find.byType(TextField), 'https://wa.me/123');
      await tester.tap(find.text('Buscar el grupo'));
      await asentar(tester);
      expect(
        find.textContaining('no es de un grupo de Aporteo'),
        findsOneWidget,
      );
      expect(visitadas, isEmpty);
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

    testWidgets(
      'correcto: ve el grupo, el aviso de qué datos ve el organizador, y pide',
      (tester) async {
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
        expect(find.textContaining('tu nombre completo'), findsOneWidget);
        await tester.tap(find.text('Pedir unirme'));
        await asentar(tester);
        expect(find.text('Listo, le avisamos al organizador'), findsOneWidget);
      },
    );

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

class _LectorFalso implements LectorDeQrEnImagen {
  const _LectorFalso(this.lectura);
  final LecturaDeImagen lectura;

  @override
  Future<LecturaDeImagen> elegirYLeer() async => lectura;
}

class _CompartirFalso implements Compartir {
  String? texto;
  String? imagenRuta;

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

  @override
  Future<bool> imagen({
    required String ruta,
    required String texto,
    bool porWhatsApp = true,
  }) async {
    imagenRuta = ruta;
    this.texto = texto;
    return true;
  }
}
