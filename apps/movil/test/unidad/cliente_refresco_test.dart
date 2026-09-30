import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';

/// Adaptador propio, no `http_mock_adapter`: su `Recording._invocationIndex` es un
/// campo mutable único por instancia y no distingue de forma confiable diez peticiones
/// concurrentes al mismo path con la misma firma salvo el header `Authorization` — se
/// probó y el "último registro gana" hacía que el escenario de la carrera no se
/// pudiera armar de forma determinista. Este adaptador es mínimo y determinista: cada
/// petición se resuelve mirando su propio `RequestOptions`, sin estado compartido entre
/// peticiones.
class _AdaptadorDeCarrera implements HttpClientAdapter {
  int refrescos = 0;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    if (options.path == '/sesion/refrescar') {
      refrescos++;
      return _json(200, {'acceso': 'nuevo', 'refresco': 'r2'});
    }
    final auth = options.headers['Authorization'] as String?;
    if (auth == 'Bearer viejo') return _json(401, {});
    if (auth == 'Bearer nuevo') {
      final indice = options.path.split('-').last;
      return _json(200, {'ok': int.parse(indice)});
    }
    throw StateError('petición inesperada: ${options.path} auth=$auth');
  }

  ResponseBody _json(int status, Map<String, dynamic> cuerpo) {
    return ResponseBody.fromBytes(
      utf8.encode(jsonEncode(cuerpo)),
      status,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

/// Variante donde el refresco siempre falla (500): cada `/recurso-i` responde 401
/// siempre (nunca hay token válido que probar).
class _AdaptadorDeCarreraConFalloDeRefresco implements HttpClientAdapter {
  int refrescos = 0;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    if (options.path == '/sesion/refrescar') {
      refrescos++;
      return _json(500, {});
    }
    return _json(401, {});
  }

  ResponseBody _json(int status, Map<String, dynamic> cuerpo) => ResponseBody.fromBytes(
    utf8.encode(jsonEncode(cuerpo)),
    status,
    headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    },
  );

  @override
  void close({bool force = false}) {}
}

/// Diez `401` concurrentes deben compartir un único refresco (single-flight), igual
/// que su contraparte en el backoffice (`sesion.interceptor.spec.ts`, H2 de PR11).
/// Hoy `_TrazaYSesion._refrescar()` llama `POST /sesion/refrescar` una vez por cada
/// error, sin compartir nada entre peticiones concurrentes.
void main() {
  test(
    'diez 401 concurrentes con token vencido comparten un único refresco',
    () async {
      final sesion = Sesion(AlmacenEnMemoria());
      await sesion.guardar(acceso: 'viejo', refresco: 'r1');

      final dio = Dio(BaseOptions(baseUrl: baseDelGateway));
      final adaptador = _AdaptadorDeCarrera();
      dio.httpClientAdapter = adaptador;
      instalarInterceptores(dio, sesion);

      final resultados = await Future.wait([
        for (var i = 0; i < 10; i++) dio.get<Map<String, dynamic>>('/recurso-$i'),
      ]);

      expect(resultados, hasLength(10));
      expect(resultados.map((r) => r.data?['ok']), containsAll(List.generate(10, (i) => i)));
      expect(
        adaptador.refrescos,
        1,
        reason: 'se esperaba 1 refresh, salieron ${adaptador.refrescos}',
      );
    },
  );

  test(
    'si el refresco falla, la sesión se cierra una vez y las N fallan con su error original',
    () async {
      final sesion = Sesion(AlmacenEnMemoria());
      await sesion.guardar(acceso: 'viejo', refresco: 'r1');

      final dio = Dio(BaseOptions(baseUrl: baseDelGateway));
      final adaptador = _AdaptadorDeCarreraConFalloDeRefresco();
      dio.httpClientAdapter = adaptador;
      instalarInterceptores(dio, sesion);

      final resultados = await Future.wait([
        for (var i = 0; i < 4; i++)
          dio.get<Map<String, dynamic>>('/recurso-$i').then<Object>((r) => r).catchError((Object e) => e),
      ]);

      expect(adaptador.refrescos, 1, reason: 'cero segundos refresh');
      for (final r in resultados) {
        expect(r, isA<DioException>());
        expect((r as DioException).response?.statusCode, 401, reason: 'error original, no el 500 del refresco');
      }
      expect(await sesion.tokenDeAcceso(), isNull, reason: 'sesión cerrada');
    },
  );
}
