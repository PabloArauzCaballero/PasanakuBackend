part of 'cliente.dart';

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
    final codigo = mapa['codigo'] as String? ?? 'AP-DESCONOCIDO';
    final traza =
        (mapa['trazaId'] as String?) ??
        (err.requestOptions.headers['x-request-id'] as String? ?? '');
    registrarRechazoApi(
      metodo: err.requestOptions.method,
      ruta: err.requestOptions.path,
      estado: respuesta.statusCode ?? 0,
      codigo: codigo,
      traza: traza,
    );
    handler.reject(
      DioException(
        requestOptions: err.requestOptions,
        response: respuesta,
        type: err.type,
        error: ErrorDeApi(
          codigo: codigo,
          trazaId: traza,
          estado: respuesta.statusCode ?? 0,
          contexto: _contextoError(err.requestOptions),
          campoDelServidor: campoDelDetalle(mapa['detalle']),
        ),
      ),
    );
  }
}

ContextoErrorApi _contextoError(RequestOptions request) {
  final path = Uri.parse(request.path).path;
  if (path.contains('/usuarios/verificaciones/correo')) {
    return ContextoErrorApi.verificacionCorreo;
  }
  if (request.method.toUpperCase() == 'POST' && path.endsWith('/usuarios')) {
    return ContextoErrorApi.registro;
  }
  if (request.method.toUpperCase() == 'POST' && path.endsWith('/grupos')) {
    return ContextoErrorApi.crearGrupo;
  }
  if (path.startsWith('/recargas-de-prueba/')) {
    return ContextoErrorApi.pagoDeRecargaDePrueba;
  }
  if (RegExp(r'/usuarios/[^/]+/verificacion$').hasMatch(path)) {
    return ContextoErrorApi.estadoDeVerificacion;
  }
  return ContextoErrorApi.general;
}
