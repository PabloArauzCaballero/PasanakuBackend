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
}
