import 'package:dio/dio.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/errores.dart';

/// El interceptor de errores, sin widgets: un 4xx llega como ErrorDeApi traducido y
/// una caída de red como ErrorDeRed, sin código de seguimiento.
void main() {
  test(
    'un 403 llega traducido, con traza y sin el mensaje del backend',
    () async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        '/x',
        (s) => s.reply(403, {
          'codigo': 'AP-SEG-03',
          'mensaje': 'detalle',
          'trazaId': 't-1',
        }),
      );
      Object? capturado;
      try {
        await dio.get<Map<String, dynamic>>('/x');
      } catch (e) {
        capturado = errorDeDominio(e);
      }
      expect(capturado, isA<ErrorDeApi>());
      final error = capturado! as ErrorDeApi;
      expect(
        error.mensaje,
        'Esta acción todavía no está disponible para tu cuenta.',
      );
      expect(error.trazaId, 't-1');
    },
  );

  Future<ErrorDeApi> rechazo403(String metodo, String ruta) async {
    final (:dio, :adaptador) = dioSimulado();
    final cuerpo = {'codigo': 'AP-SEG-03', 'mensaje': 'x', 'trazaId': 't-2'};
    if (metodo == 'POST') {
      adaptador.onPost(ruta, (s) => s.reply(403, cuerpo), data: Matchers.any);
    } else {
      adaptador.onGet(ruta, (s) => s.reply(403, cuerpo));
    }
    try {
      metodo == 'POST'
          ? await dio.post<Map<String, dynamic>>(ruta, data: const {})
          : await dio.get<Map<String, dynamic>>(ruta);
    } catch (e) {
      return errorDeDominio(e) as ErrorDeApi;
    }
    fail('se esperaba un 403');
  }

  test('403 al crear grupo explica que hace falta ser organizador', () async {
    final error = await rechazo403('POST', '/grupos');
    expect(error.mensaje, contains('habilitado como organizador'));
    expect(error.trazaId, 't-2');
  });

  test(
    '403 al consultar el expediente dice que la cuenta está en verificación',
    () async {
      final error = await rechazo403('GET', '/usuarios/u-1/verificacion');
      expect(error.mensaje, startsWith('Tu cuenta está en verificación'));
    },
  );

  test('el 403 de otra ruta no hereda esos textos', () async {
    final error = await rechazo403('GET', '/grupos/g-1');
    expect(
      error.mensaje,
      'Esta acción todavía no está disponible para tu cuenta.',
    );
  });

  test(
    'un 422 de cuenta repetida dice cuál dato ya tiene cuenta (detalle.campo)',
    () async {
      // Regresión (2026-10-15): el correo repetido salía como «algo salió mal de
      // nuestro lado», y cuando se arregló el backend iba a decir «ese celular».
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        '/usuarios',
        (s) => s.reply(422, {
          'codigo': 'AP-CU01-03',
          'mensaje': 'Ya hay una cuenta con ese correo.',
          'detalle': {'campo': 'correo'},
          'trazaId': 't-2',
        }),
        data: Matchers.any,
      );
      Object? capturado;
      try {
        await dio.post<Map<String, dynamic>>('/usuarios', data: {});
      } catch (e) {
        capturado = errorDeDominio(e);
      }
      final error = capturado! as ErrorDeApi;
      expect(error.campoDelServidor, 'correo');
      expect(error.mensaje, startsWith('Ese correo ya tiene una cuenta.'));
    },
  );

  test('cada AP-BD-0n tiene su texto y ninguno culpa al servidor', () {
    for (final codigo in [
      'AP-BD-01',
      'AP-BD-02',
      'AP-BD-03',
      'AP-BD-04',
      'AP-BD-05',
      'AP-BD-06',
    ]) {
      expect(
        mensajeDe(codigo, estado: 409),
        isNot(contains('de nuestro lado')),
        reason: codigo,
      );
    }
  });

  test('sin red llega como ErrorDeRed', () async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      '/x',
      (s) => s.throws(
        0,
        DioException.connectionError(
          requestOptions: RequestOptions(path: '/x'),
          reason: 'sin red',
        ),
      ),
    );
    Object? capturado;
    try {
      await dio.get<Map<String, dynamic>>('/x');
    } catch (e) {
      capturado = errorDeDominio(e);
    }
    expect(capturado, isA<ErrorDeRed>());
  });

  test('traduce códigos de correo con su significado contextual', () async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onPost(
      '/usuarios/verificaciones/correo/123/confirmacion',
      (s) => s.reply(422, {
        'codigo': 'AP-CU01-09',
        'mensaje': 'detalle interno',
        'trazaId': 't-correo',
      }),
    );

    Object? capturado;
    try {
      await dio.post<void>('/usuarios/verificaciones/correo/123/confirmacion');
    } catch (e) {
      capturado = errorDeDominio(e);
    }

    expect(capturado, isA<ErrorDeApi>());
    final error = capturado! as ErrorDeApi;
    expect(
      error.mensaje,
      'Ese código de correo no coincide o ya no está disponible. Revisalo o pedí otro.',
    );
    expect(error.mensaje, isNot(contains('documento')));
  });

  test('traduce el requisito de confirmar correo al registrar', () async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onPost(
      '/usuarios',
      (s) => s.reply(422, {
        'codigo': 'AP-CU01-11',
        'mensaje': 'detalle interno',
        'trazaId': 't-alta',
      }),
    );

    Object? capturado;
    try {
      await dio.post<void>('/usuarios');
    } catch (e) {
      capturado = errorDeDominio(e);
    }

    expect(capturado, isA<ErrorDeApi>());
    expect(
      (capturado! as ErrorDeApi).mensaje,
      'Confirmá el código de correo antes de crear la cuenta.',
    );
  });

  test(
    'el código de documento vencido mantiene su significado en el alta',
    () async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        '/usuarios',
        (s) => s.reply(422, {
          'codigo': 'AP-CU01-07',
          'mensaje': 'detalle interno',
          'trazaId': 't-documento',
        }),
      );

      Object? capturado;
      try {
        await dio.post<void>('/usuarios');
      } catch (e) {
        capturado = errorDeDominio(e);
      }

      expect(capturado, isA<ErrorDeApi>());
      final error = capturado! as ErrorDeApi;
      expect(
        error.mensaje,
        'Tu carnet está vencido. Para abrir la cuenta hace falta uno vigente.',
      );
      expect(error.campo, CampoDeErrorApi.fechaExpiracion);
    },
  );
}
