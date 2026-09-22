import 'dart:math';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart' show kReleaseMode;
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/errores.dart';
import '../proveedores/sesion.dart';
import 'configuracion.dart';

/// **Una sola base URL: el gateway.** El prefijo enruta al servicio; la app no
/// conoce catorce direcciones.
///
/// **Ya no hay valor por omisión** (H3.S3.M2 / H5.S3.M2): tenía
/// `http://localhost/api/v1`, y en un build de release sin
/// `--dart-define=API=...` ese valor es **el propio teléfono de quien instaló la
/// app** — el mismo hallazgo que los `gateway.ts` de backoffice y web. Ahora, sin el
/// define, `resultadoGateway.valida` es `false` y no hay URL: `main.dart` muestra la
/// pantalla de bloqueo (`pantallas/arranque/pantalla_configuracion_invalida.dart`) en
/// vez de construir `AppAportaYa`, así que `dioProvider` nunca llega a leerse
/// (Riverpod es perezoso: un provider que nadie lee, no se crea).
///
/// El simulado (Prism) sigue disponible para pruebas de contrato en debug/profile:
/// `--dart-define=API=http://localhost:4010/api/v1`. Desde el emulador de Android la
/// máquina anfitriona es `10.0.2.2`; desde un teléfono real, la IP de la red local —
/// ver `entregables/defines-por-plataforma.md`.
const String _apiCruda = String.fromEnvironment('API');

/// Los hosts propios compilados (D-A6, Q-J4): infra los carga en el despliegue con
/// `--dart-define=HOSTS_PERMITIDOS=api.aportaya.bo,otro.host`. Vacío en debug: ahí
/// `validarGateway` admite loopback sin necesitar la lista.
const String _hostsPermitidosCrudo = String.fromEnvironment('HOSTS_PERMITIDOS');

final List<String> hostsPermitidosGateway = _hostsPermitidosCrudo.isEmpty
    ? const []
    : _hostsPermitidosCrudo.split(',');

/// El resultado completo de validar la configuración de arranque — `main.dart` lo lee
/// antes de decidir si construye `AppAportaYa` o la pantalla de bloqueo.
final ResultadoValidacionGateway resultadoGateway = validarGateway(
  _apiCruda.isEmpty ? null : _apiCruda,
  release: kReleaseMode,
  hostsPermitidos: hostsPermitidosGateway,
);

/// `null` cuando la configuración es inválida — nunca un valor por omisión que apunte
/// a algún host.
String? get baseDelGateway => resultadoGateway.url;

/// La única salida a la red de la app. Ningún widget crea un `Dio`.
///
/// Si `baseDelGateway` es `null` este provider no debería leerse nunca: `main.dart`
/// ya cortó antes de construir el árbol que lo necesita. Si por algún camino nuevo
/// se leyera igual, lanza en vez de crear un cliente apuntando a ningún lado —
/// fail-fast, no un cliente roto en silencio.
final dioProvider = Provider<Dio>((ref) {
  final base = baseDelGateway;
  if (base == null) {
    throw StateError(
      'dioProvider leído con configuración de gateway inválida '
      '(${resultadoGateway.motivo}). No se crea el cliente HTTP.',
    );
  }
  final dio = Dio(
    BaseOptions(
      baseUrl: base,
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
