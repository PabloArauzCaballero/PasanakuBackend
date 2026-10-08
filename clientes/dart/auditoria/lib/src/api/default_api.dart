//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

import 'dart:async';

// ignore: unused_import
import 'dart:convert';
import 'package:aportaya_cliente_auditoria/src/deserialize.dart';
import 'package:dio/dio.dart';

import 'package:aportaya_cliente_auditoria/src/model/entrada_derechos.dart';
import 'package:aportaya_cliente_auditoria/src/model/entrada_reporte.dart';
import 'package:aportaya_cliente_auditoria/src/model/error.dart';
import 'package:aportaya_cliente_auditoria/src/model/salida_derechos.dart';
import 'package:aportaya_cliente_auditoria/src/model/salida_reporte.dart';
import 'package:aportaya_cliente_auditoria/src/model/salida_tablero.dart';

class DefaultApi {

  final Dio _dio;

  const DefaultApi(this._dio);

  /// Cuenta una descarga del archivo y dice si todavia se puede entregar
  /// No devuelve el archivo: lo autoriza. La descarga se cuenta ANTES de entregar porque contar despues significa no contar la vez que fallo a mitad, y el tope deja de ser un tope. Un enlace que se descarga indefinidamente deja de ser una entrega y pasa a ser una copia publicada. Agotado el tope, hay que volver a pedir el reporte, con justificacion nueva — que es exactamente el rastro que una auditoria busca.
  ///
  /// Parameters:
  /// * [exportacionId] 
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future]
  /// Throws [DioException] if API call or serialization fails
  Future<Response<void>> descargarExportacion({ 
    required String exportacionId,
    required String idempotencyKey,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/reportes/exportaciones/{exportacionId}/descargas'.replaceAll('{' r'exportacionId' '}', exportacionId.toString());
    final _options = Options(
      method: r'POST',
      headers: <String, dynamic>{
        r'Idempotency-Key': idempotencyKey,
        ...?headers,
      },
      extra: <String, dynamic>{
        'secure': <Map<String, String>>[],
        ...?extra,
      },
      validateStatus: validateStatus,
    );

    final _response = await _dio.request<Object>(
      _path,
      options: _options,
      cancelToken: cancelToken,
      onSendProgress: onSendProgress,
      onReceiveProgress: onReceiveProgress,
    );

    return _response;
  }

  /// Ejecuta un reporte definido, deja huella y opcionalmente lo exporta
  /// Que sacar informacion del sistema sea una operacion con permiso, huella y vencimiento, y no una consulta suelta que alguien corre contra produccion y manda por correo. Los parametros van por LISTA BLANCA contra &#x60;parametros_esperados&#x60;: lo que no esta declarado no entra, y el valor viaja como ligadura — nunca concatenado en la consulta. La alternativa, aceptar lo que venga y filtrar lo peligroso, es como se escriben las inyecciones: siempre falta un caso. El &#x60;hashResultado&#x60; es la huella canonica del resultado. Dos ejecuciones con los mismos parametros y los mismos datos dan el mismo hash, y eso permite probar que un reporte entregado no fue alterado despues. Un reporte sin filas se entrega igual, con cero: «no hubo» es una respuesta, y para los regulatorios es la obligatoria (&#x60;R-UIF-06&#x60;). El estado terminal exitoso es COMPLETADA, que es lo que admite &#x60;ck_ejecucion_reporte_estado&#x60;. Donde el texto del caso de uso dice LISTA y la tabla dice COMPLETADA, manda la tabla: es la que rechaza.
  ///
  /// Parameters:
  /// * [definicionId] 
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaReporte] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaReporte] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaReporte>> ejecutarReporte({ 
    required String definicionId,
    required String idempotencyKey,
    required EntradaReporte entradaReporte,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/reportes/{definicionId}/ejecuciones'.replaceAll('{' r'definicionId' '}', definicionId.toString());
    final _options = Options(
      method: r'POST',
      headers: <String, dynamic>{
        r'Idempotency-Key': idempotencyKey,
        ...?headers,
      },
      extra: <String, dynamic>{
        'secure': <Map<String, String>>[],
        ...?extra,
      },
      contentType: 'application/json',
      validateStatus: validateStatus,
    );

    dynamic _bodyData;

    try {
_bodyData=jsonEncode(entradaReporte);
    } catch(error, stackTrace) {
      throw DioException(
         requestOptions: _options.compose(
          _dio.options,
          _path,
        ),
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    final _response = await _dio.request<Object>(
      _path,
      data: _bodyData,
      options: _options,
      cancelToken: cancelToken,
      onSendProgress: onSendProgress,
      onReceiveProgress: onReceiveProgress,
    );

    SalidaReporte? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaReporte, SalidaReporte>(rawData, 'SalidaReporte', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaReporte>(
      data: _responseData,
      headers: _response.headers,
      isRedirect: _response.isRedirect,
      requestOptions: _response.requestOptions,
      redirects: _response.redirects,
      statusCode: _response.statusCode,
      statusMessage: _response.statusMessage,
      extra: _response.extra,
    );
  }

  /// Atiende un derecho del titular sobre sus datos personales
  /// Acceso, rectificacion, oposicion, portabilidad y cancelacion. El plazo legal se calcula al abrir y SE GUARDA: recalcularlo al consultar lo haria moverse solo, y basta que alguien declare un feriado despues para que el vencimiento cambie sin que nadie lo decida. La cancelacion NO elige entre el derecho y la obligacion de conservar: reparte. Borra lo que ya vencio, seudonimiza lo que la ley obliga a guardar, y devuelve la lista de lo que quedo con su base legal. Responder «no se puede borrar nada» seria mentirle al titular; borrar todo seria un incumplimiento. Por eso el estado puede ser PARCIAL: decir ATENDIDA sobre una solicitud que no se cumplio entera es la forma mas rapida de que el reclamo vuelva con la razon del otro lado.
  ///
  /// Parameters:
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaDerechos] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaDerechos] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaDerechos>> ejercerDerechos({ 
    required String idempotencyKey,
    required EntradaDerechos entradaDerechos,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/auditoria/derechos';
    final _options = Options(
      method: r'POST',
      headers: <String, dynamic>{
        r'Idempotency-Key': idempotencyKey,
        ...?headers,
      },
      extra: <String, dynamic>{
        'secure': <Map<String, String>>[],
        ...?extra,
      },
      contentType: 'application/json',
      validateStatus: validateStatus,
    );

    dynamic _bodyData;

    try {
_bodyData=jsonEncode(entradaDerechos);
    } catch(error, stackTrace) {
      throw DioException(
         requestOptions: _options.compose(
          _dio.options,
          _path,
        ),
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    final _response = await _dio.request<Object>(
      _path,
      data: _bodyData,
      options: _options,
      cancelToken: cancelToken,
      onSendProgress: onSendProgress,
      onReceiveProgress: onReceiveProgress,
    );

    SalidaDerechos? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaDerechos, SalidaDerechos>(rawData, 'SalidaDerechos', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaDerechos>(
      data: _responseData,
      headers: _response.headers,
      isRedirect: _response.isRedirect,
      requestOptions: _response.requestOptions,
      redirects: _response.redirects,
      statusCode: _response.statusCode,
      statusMessage: _response.statusMessage,
      extra: _response.extra,
    );
  }

  /// Los indicadores del periodo, con su meta, su variacion y su serie
  /// [CONTRATO] Que la direccion mire siempre los mismos numeros, calculados de la misma forma: un indicador es una DEFINICION, no una consulta, y si dos lugares lo recalculan ya hay dos indicadores. Se lee de &#x60;auditoria.indicador_kpi&#x60;, que es donde el calculo los deja. Esta operacion NO calcula: publica. La &#x60;dimension&#x60; es la del modelo —GLOBAL, POR_GRUPO, POR_ORGANIZADOR— y no la que enumera el texto de CU-98. Donde el caso de uso y la tabla no coinciden, manda la tabla: es la que rechaza.
  ///
  /// Parameters:
  /// * [periodo] - Formato AAAA, AAAA-MM o AAAA-MM-DD, segun la granularidad
  /// * [dimension] 
  /// * [dimensionId] - Obligatorio salvo en GLOBAL, donde no corresponde
  /// * [periodosDeSerie] - Cuantos periodos anteriores traer para el grafico. Cero trae solo el actual.
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaTablero] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaTablero>> publicarTablero({ 
    required String periodo,
    required String dimension,
    String? dimensionId,
    int? periodosDeSerie = 6,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/indicadores';
    final _options = Options(
      method: r'GET',
      headers: <String, dynamic>{
        ...?headers,
      },
      extra: <String, dynamic>{
        'secure': <Map<String, String>>[],
        ...?extra,
      },
      validateStatus: validateStatus,
    );

    final _queryParameters = <String, dynamic>{
      r'periodo': periodo,
      r'dimension': dimension,
      if (dimensionId != null) r'dimensionId': dimensionId,
      if (periodosDeSerie != null) r'periodosDeSerie': periodosDeSerie,
    };

    final _response = await _dio.request<Object>(
      _path,
      options: _options,
      queryParameters: _queryParameters,
      cancelToken: cancelToken,
      onSendProgress: onSendProgress,
      onReceiveProgress: onReceiveProgress,
    );

    SalidaTablero? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaTablero, SalidaTablero>(rawData, 'SalidaTablero', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaTablero>(
      data: _responseData,
      headers: _response.headers,
      isRedirect: _response.isRedirect,
      requestOptions: _response.requestOptions,
      redirects: _response.redirects,
      statusCode: _response.statusCode,
      statusMessage: _response.statusMessage,
      extra: _response.extra,
    );
  }

}
