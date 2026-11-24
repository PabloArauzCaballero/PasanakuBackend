import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:aportaya_movil/dominio/refrescador.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// DEFECTO-003: `cliente.dart`, `renovar_acceso.dart` y `habilitacion_basica.dart` crean
/// cada uno su `Refrescador`. Si el single-flight vive en la instancia, dos refrescos a la
/// vez gastan el mismo token de refresco dos veces y el servidor revoca la sesión por
/// reuso. El refresco en vuelo se comparte por sesión, no por instancia.
class _Servidor implements HttpClientAdapter {
  _Servidor(this.respuesta);
  final int respuesta;
  int refrescos = 0;
  final _liberar = Completer<void>();
  void liberar() => _liberar.complete();

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    refrescos++;
    await _liberar.future;
    return ResponseBody.fromBytes(
      utf8.encode(jsonEncode({'acceso': 'nuevo', 'refresco': 'r2'})),
      respuesta,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

Dio _dio(_Servidor s) =>
    Dio(BaseOptions(baseUrl: 'https://pruebas.aportaya.bo/api/v1'))
      ..httpClientAdapter = s;

void main() {
  test(
    'correcto: dos Refrescador distintos sobre la misma sesión comparten un único refresco',
    () async {
      final sesion = Sesion(AlmacenEnMemoria());
      await sesion.abrir(acceso: 'viejo', refresco: 'r1');
      final servidor = _Servidor(200);

      final a = Refrescador(sesion, _dio(servidor)).refrescar();
      final b = Refrescador(sesion, _dio(servidor)).refrescar();
      await Future<void>.delayed(Duration.zero);
      servidor.liberar();

      expect(await Future.wait([a, b]), [true, true]);
      expect(servidor.refrescos, 1);
    },
  );

  test(
    'límite: terminado un refresco, el siguiente vuelve a salir (no queda pegado)',
    () async {
      final sesion = Sesion(AlmacenEnMemoria());
      await sesion.abrir(acceso: 'viejo', refresco: 'r1');
      final primero = _Servidor(200)..liberar();
      expect(await Refrescador(sesion, _dio(primero)).refrescar(), isTrue);

      final segundo = _Servidor(200)..liberar();
      expect(await Refrescador(sesion, _dio(segundo)).refrescar(), isTrue);
      expect(segundo.refrescos, 1);
    },
  );

  test(
    'inválido: si el refresco falla, los dos que esperaban reciben false con una sola llamada',
    () async {
      final sesion = Sesion(AlmacenEnMemoria());
      await sesion.abrir(acceso: 'viejo', refresco: 'r1');
      final servidor = _Servidor(500);

      final a = Refrescador(sesion, _dio(servidor)).refrescar();
      final b = Refrescador(sesion, _dio(servidor)).refrescar();
      await Future<void>.delayed(Duration.zero);
      servidor.liberar();

      expect(await Future.wait([a, b]), [false, false]);
      expect(servidor.refrescos, 1);
    },
  );

  test('sesiones distintas no comparten refresco', () async {
    final una = Sesion(AlmacenEnMemoria());
    await una.abrir(acceso: 'viejo', refresco: 'r1');
    final otra = Sesion(AlmacenEnMemoria());
    await otra.abrir(acceso: 'viejo', refresco: 'r9');
    final servidor = _Servidor(200);

    final a = Refrescador(una, _dio(servidor)).refrescar();
    final b = Refrescador(otra, _dio(servidor)).refrescar();
    await Future<void>.delayed(Duration.zero);
    servidor.liberar();

    await Future.wait([a, b]);
    expect(servidor.refrescos, 2);
  });
}
