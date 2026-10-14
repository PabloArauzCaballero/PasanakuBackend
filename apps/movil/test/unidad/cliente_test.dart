import 'package:dio/dio.dart';
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
      expect(error.mensaje, 'No tenés acceso a esto.');
      expect(error.trazaId, 't-1');
    },
  );

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
