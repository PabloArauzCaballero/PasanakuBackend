import 'dart:convert';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:aportaya_movil/pantallas/identidad/continuar_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/verificacion_correo.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// Continuar un alta a medias desde el login (2026-10-30, pedido de Pablo).
///
/// El servidor está simulado con el adaptador de Dio (doble declarado): el contrato es
/// `POST /usuarios/verificaciones/correo` y su confirmación, que devuelve el borrador.
/// Falta verificarlo contra el servicio `identidad` real.
const _correo = 'carlos@example.com';
const _id = 'dddddddd-0000-4000-8000-000000000003';
const _confirmacion = '/usuarios/verificaciones/correo/$_id/confirmacion';

/// Lo que deja el teléfono si la persona confirmó el código y cerró la app. Al volver
/// y escribir el correo, `actualizarDatos` lo olvida (`notifier_alta.dart:21-26`).
Future<AlmacenEnMemoria> _almacenConCodigoYaConfirmado() async {
  final almacen = AlmacenEnMemoria();
  await almacen.guardar(
    VerificacionCorreo.claveGuardada,
    jsonEncode({
      'correo': _correo,
      'clave': 'clave-sintetica',
      'id': _id,
      'destino': 'ca***@example.com',
      'expiraEn': DateTime.now()
          .toUtc()
          .add(const Duration(hours: 1))
          .toIso8601String(),
      'confirmada': true,
    }),
  );
  return almacen;
}

void main() {
  late DioAdapter adaptador;
  late ProviderContainer c;
  var pedidosDeCodigo = 0;

  Future<void> preparar() async {
    final (:dio, adaptador: a) = dioSimulado();
    adaptador = a;
    pedidosDeCodigo = 0;
    // Se cuenta con un interceptor: el manejador del adaptador corre una vez al
    // registrarse, no en cada pedido.
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (o, h) {
          if (o.method == 'POST' &&
              o.path == '/usuarios/verificaciones/correo') {
            pedidosDeCodigo++;
          }
          h.next(o);
        },
      ),
    );
    adaptador.onPost(
      '/usuarios/verificaciones/correo',
      (s) => s.reply(202, {
        'verificacionId': _id,
        'destinoEnmascarado': 'ca***@example.com',
        'expiraEn': '2099-01-01T00:00:00Z',
      }),
      data: Matchers.any,
    );
    c = ProviderContainer(
      overrides: [
        almacenSeguroProvider.overrideWithValue(
          await _almacenConCodigoYaConfirmado(),
        ),
        dioProvider.overrideWithValue(dio),
      ],
    );
    addTearDown(c.dispose);
    c
        .read(altaProvider.notifier)
        .actualizarDatos(const DatosPersonales(correo: _correo));
  }

  test(
    'al escribir el correo se olvida el código guardado: siempre se pide uno '
    'nuevo, y confirmarlo es lo que trae el borrador',
    () async {
      await preparar();
      await c.read(altaProvider.notifier).solicitarCodigoCorreo();
      expect(pedidosDeCodigo, 1);
      expect(c.read(altaProvider).codigoConfirmado, false);
    },
  );

  test('correcto: continuar pide código nuevo y al confirmarlo vuelve al paso '
      'pendiente con lo cargado', () async {
    await preparar();
    adaptador.onPost(
      _confirmacion,
      (s) => s.reply(200, {
        'verificada': true,
        'onboardingIncompleto': true,
        'borrador': {
          'correo': _correo,
          'nombres': 'Carlos',
          'numeroDocumento': '9876543',
          'pasoPendiente': 'datos',
        },
      }),
      data: Matchers.any,
    );
    final alta = c.read(altaProvider.notifier);
    await alta.solicitarCodigoCorreo(nuevo: true);
    expect(pedidosDeCodigo, 1);
    expect(c.read(altaProvider).codigoConfirmado, false);
    await alta.confirmarCodigoCorreo('654321');
    final estado = c.read(altaProvider);
    expect(estado.paso, PasoAlta.datos);
    expect(estado.datos.nombres, 'Carlos');
    expect(estado.datos.numeroDocumento, '9876543');
  });

  test('límite: sin borrador en el servidor, el código queda confirmado y el '
      'alta sigue desde el principio', () async {
    await preparar();
    adaptador.onPost(
      _confirmacion,
      (s) => s.reply(200, {'verificada': true, 'onboardingIncompleto': false}),
      data: Matchers.any,
    );
    final alta = c.read(altaProvider.notifier);
    await alta.solicitarCodigoCorreo(nuevo: true);
    await alta.confirmarCodigoCorreo('654321');
    final estado = c.read(altaProvider);
    expect(estado.codigoConfirmado, true);
    expect(estado.paso, PasoAlta.cuenta);
    expect(estado.datos.nombres, isEmpty);
  });

  test('inválido: código rechazado deja el error y no inventa datos', () async {
    await preparar();
    adaptador.onPost(
      _confirmacion,
      (s) => s.reply(422, {
        'codigo': 'AP-CU01-09',
        'mensaje': 'El código no coincide',
        'trazaId': 'traza-continuar',
      }),
      data: Matchers.any,
    );
    final alta = c.read(altaProvider.notifier);
    await alta.solicitarCodigoCorreo(nuevo: true);
    await alta.confirmarCodigoCorreo('000000');
    final estado = c.read(altaProvider);
    expect(estado.errorCodigo, isNotNull);
    expect(estado.codigoConfirmado, false);
    expect(estado.paso, PasoAlta.cuenta);
    expect(estado.datos.nombres, isEmpty);
  });

  Future<GoRouter> abrirIngreso(WidgetTester tester) async {
    tester.view.physicalSize = const Size(1170, 2532);
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.reset);
    final router = crearEnrutador(sesion: await sesionAnonimaDePrueba());
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
          routerConfig: router,
        ),
      ),
    );
    router.go('/ingreso');
    await tester.pumpAndSettle();
    return router;
  }

  testWidgets('el login ofrece continuar el registro y lleva al alta en modo '
      'continuar, con el aviso', (tester) async {
    final router = await abrirIngreso(tester);
    final enlace = find.text(TextosContinuarAlta.enlace);
    await tester.ensureVisible(enlace);
    await tester.tap(enlace);
    await tester.pumpAndSettle();
    expect(router.state.uri.toString(), rutaContinuarAlta);
    expect(find.text(TextosContinuarAlta.avisoTitulo), findsOneWidget);
  });

  testWidgets('el alta normal no muestra el aviso de continuar', (
    tester,
  ) async {
    final router = await abrirIngreso(tester);
    router.go('/registro');
    await tester.pumpAndSettle();
    expect(find.text(TextosContinuarAlta.avisoTitulo), findsNothing);
  });
}
