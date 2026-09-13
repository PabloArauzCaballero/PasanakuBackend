import 'dart:math';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/errores.dart';
import '../proveedores/sesion.dart';

/// **Una sola base URL: el gateway.** El prefijo enruta al servicio; la app no
/// conoce catorce direcciones.
///
/// Por omisión apunta al **backend de verdad** en la máquina de desarrollo: NGINX
/// publica el 80, y el gateway enruta `/api/v1/<prefijo>` al servicio que reservó ese
/// prefijo. Antes apuntaba a Prism, el simulado, y con eso la app «funcionaba» sin
/// que existiera un backend detrás: cualquier celular y cualquier contraseña entraban.
///
/// El simulado sigue disponible para pruebas de contrato, pero hay que pedirlo:
/// `--dart-define=API=http://localhost:4010/api/v1`. Desde el emulador de Android la
/// máquina es `10.0.2.2`; desde un teléfono real, la IP de la red local.
const String baseDelGateway = String.fromEnvironment(
  'API',
  defaultValue: 'http://localhost/api/v1',
);

/// La única salida a la red de la app. Ningún widget crea un `Dio`.
final dioProvider = Provider<Dio>((ref) {
  final dio = Dio(
    BaseOptions(
      baseUrl: baseDelGateway,
      connectTimeout: const Duration(seconds: 8),
      receiveTimeout: const Duration(seconds: 12),
      headers: {'Accept': 'application/json'},
    ),
  );
  instalarInterceptores(dio, ref.read(sesionProvider));
  return dio;
});

/// Los mismos interceptores en la app y en las pruebas: lo que se prueba es la
/// traducción de errores y la traza reales, no un doble.
void instalarInterceptores(Dio dio, Sesion sesion) {
  dio.interceptors.add(_TrazaYSesion(sesion, dio));
  dio.interceptors.add(_TraduccionDeErrores());
}

/// `x-request-id` en cada petición, bearer desde el almacén seguro, y **un** refresco
/// con **un** reintento ante `401`. Si falla, sesión cerrada, no bucle.
class _TrazaYSesion extends Interceptor {
  _TrazaYSesion(this._sesion, this._dio);
  final Sesion _sesion;
  final Dio _dio;

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    options.headers['x-request-id'] ??= _traza();
    final token = await _sesion.tokenDeAcceso();
    if (token != null) options.headers['Authorization'] = 'Bearer $token';
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final yaReintentada = err.requestOptions.extra['reintentada'] == true;
    if (err.response?.statusCode != 401 || yaReintentada) {
      return handler.next(err);
    }
    final refrescada = await _refrescar();
    if (!refrescada) {
      await _sesion.cerrar();
      return handler.next(err);
    }
    final opciones = err.requestOptions..extra['reintentada'] = true;
    try {
      handler.resolve(await _dio.fetch(opciones));
    } on DioException catch (e) {
      if (e.response?.statusCode == 401) {
        await _sesion.cerrar();
      }
      handler.next(e);
    }
  }

  Future<bool> _refrescar() async {
    final refresco = await _sesion.tokenDeRefresco();
    if (refresco == null) return false;
    try {
      final r = await _dio.post<Map<String, dynamic>>(
        '/sesion/refrescar',
        data: {'refresco': refresco},
        options: Options(extra: {'reintentada': true}),
      );
      final acceso = r.data?['acceso'] as String?;
      final nuevo = r.data?['refresco'] as String?;
      if (acceso == null || nuevo == null) return false;
      await _sesion.guardar(acceso: acceso, refresco: nuevo);
      return true;
    } on DioException {
      return false;
    }
  }
}

/// Un error de red no llegó al backend: no lleva código de seguimiento. Un error de
/// API llega con `AP-CU<NN>-<nn>` y se traduce por catálogo, nunca se muestra crudo.
class _TraduccionDeErrores extends Interceptor {
  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    final respuesta = err.response;
    if (respuesta == null) {
      return handler.reject(
        DioException(
          requestOptions: err.requestOptions,
          error: ErrorDeRed(),
          type: err.type,
        ),
      );
    }
    final cuerpo = respuesta.data;
    final mapa = cuerpo is Map<String, dynamic>
        ? cuerpo
        : const <String, dynamic>{};
    final traza =
        (mapa['trazaId'] as String?) ??
        (err.requestOptions.headers['x-request-id'] as String? ?? '');
    handler.reject(
      DioException(
        requestOptions: err.requestOptions,
        response: respuesta,
        type: err.type,
        error: ErrorDeApi(
          codigo: (mapa['codigo'] as String?) ?? 'AP-DESCONOCIDO',
          trazaId: traza,
          estado: respuesta.statusCode ?? 0,
        ),
      ),
    );
  }
}

String _traza() {
  final r = Random();
  return List.generate(
    16,
    (_) => r.nextInt(256).toRadixString(16).padLeft(2, '0'),
  ).join();
}

/// Desenvuelve el `DioException` para que la pantalla vea `ErrorDeApi` o `ErrorDeRed`.
Object errorDeDominio(Object e) =>
    e is DioException && e.error != null ? e.error! : e;
