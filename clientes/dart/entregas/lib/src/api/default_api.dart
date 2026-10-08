//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

import 'dart:async';

// ignore: unused_import
import 'dart:convert';
import 'package:aportaya_cliente_entregas/src/deserialize.dart';
import 'package:dio/dio.dart';

import 'package:aportaya_cliente_entregas/src/model/designar_cuenta_principal200_response.dart';
import 'package:aportaya_cliente_entregas/src/model/disponibilidad.dart';
import 'package:aportaya_cliente_entregas/src/model/ejecutar_entrega_request.dart';
import 'package:aportaya_cliente_entregas/src/model/entrada_liquidacion.dart';
import 'package:aportaya_cliente_entregas/src/model/entrada_orden_desembolso.dart';
import 'package:aportaya_cliente_entregas/src/model/entrada_registro_cuenta.dart';
import 'package:aportaya_cliente_entregas/src/model/entrada_respuesta_desembolso.dart';
import 'package:aportaya_cliente_entregas/src/model/error.dart';
import 'package:aportaya_cliente_entregas/src/model/salida_autorizacion.dart';
import 'package:aportaya_cliente_entregas/src/model/salida_ejecucion_entrega.dart';
import 'package:aportaya_cliente_entregas/src/model/salida_intento_desembolso.dart';
import 'package:aportaya_cliente_entregas/src/model/salida_liquidacion.dart';
import 'package:aportaya_cliente_entregas/src/model/salida_orden_desembolso.dart';
import 'package:aportaya_cliente_entregas/src/model/salida_registro_cuenta.dart';
import 'package:aportaya_cliente_entregas/src/model/salida_verificacion_cuenta.dart';
import 'package:aportaya_cliente_entregas/src/model/verificar_cuenta_destino_request.dart';

class DefaultApi {

  final Dio _dio;

  const DefaultApi(this._dio);

  /// Registrar lo que contesto el proveedor
  /// La llamada al proveedor la hace quien orquesta, FUERA de esta transaccion (invariante 6). Un error definitivo NO se reintenta: insistir contra una cuenta que no existe solo demora el momento en que alguien mira el caso, mientras la plata del beneficiario sigue retenida.
  ///
  /// Parameters:
  /// * [ordenId] 
  /// * [entradaRespuestaDesembolso] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaIntentoDesembolso] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaIntentoDesembolso>> anotarRespuestaDesembolso({ 
    required String ordenId,
    required EntradaRespuestaDesembolso entradaRespuestaDesembolso,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/desembolsos/{ordenId}/respuesta'.replaceAll('{' r'ordenId' '}', ordenId.toString());
    final _options = Options(
      method: r'POST',
      headers: <String, dynamic>{
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
_bodyData=jsonEncode(entradaRespuestaDesembolso);
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

    SalidaIntentoDesembolso? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaIntentoDesembolso, SalidaIntentoDesembolso>(rawData, 'SalidaIntentoDesembolso', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaIntentoDesembolso>(
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

  /// Autorizar la entrega
  /// Una validacion bloqueante sin resolver la frena. Saltearlas para «no demorar la entrega» es como se entrega mal.
  ///
  /// Parameters:
  /// * [entregaId] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaAutorizacion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaAutorizacion>> autorizarEntrega({ 
    required String entregaId,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/entregas/{entregaId}/autorizacion'.replaceAll('{' r'entregaId' '}', entregaId.toString());
    final _options = Options(
      method: r'POST',
      headers: <String, dynamic>{
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

    SalidaAutorizacion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaAutorizacion, SalidaAutorizacion>(rawData, 'SalidaAutorizacion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaAutorizacion>(
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

  /// Si la cuenta puede recibir plata ahora, y si no, cuanto falta
  /// Se dice CUANTO falta, no solo que no se puede: negar sin explicar convierte una medida de seguridad en una falla del sistema.
  ///
  /// Parameters:
  /// * [cuentaId] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [Disponibilidad] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<Disponibilidad>> consultarDisponibilidad({ 
    required String cuentaId,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/cuentas-bancarias/{cuentaId}/disponibilidad'.replaceAll('{' r'cuentaId' '}', cuentaId.toString());
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

    final _response = await _dio.request<Object>(
      _path,
      options: _options,
      cancelToken: cancelToken,
      onSendProgress: onSendProgress,
      onReceiveProgress: onReceiveProgress,
    );

    Disponibilidad? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<Disponibilidad, Disponibilidad>(rawData, 'Disponibilidad', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<Disponibilidad>(
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

  /// Designar la cuenta principal
  /// Una sola principal por usuario (R-BIL-17). Dos harian que el destino de un retiro dependiera del orden en que la base devuelve las filas.
  ///
  /// Parameters:
  /// * [cuentaId] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [DesignarCuentaPrincipal200Response] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<DesignarCuentaPrincipal200Response>> designarCuentaPrincipal({ 
    required String cuentaId,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/cuentas-bancarias/{cuentaId}/principal'.replaceAll('{' r'cuentaId' '}', cuentaId.toString());
    final _options = Options(
      method: r'POST',
      headers: <String, dynamic>{
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

    DesignarCuentaPrincipal200Response? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<DesignarCuentaPrincipal200Response, DesignarCuentaPrincipal200Response>(rawData, 'DesignarCuentaPrincipal200Response', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<DesignarCuentaPrincipal200Response>(
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

  /// Ejecutar la entrega
  /// Quien autoriza NO ejecuta (R-SEG-04). Una sola persona que hace las dos cosas puede sacar el fondo entero de un grupo sin que nadie mas lo vea pasar.
  ///
  /// Parameters:
  /// * [entregaId] 
  /// * [ejecutarEntregaRequest] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaEjecucionEntrega] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaEjecucionEntrega>> ejecutarEntrega({ 
    required String entregaId,
    required EjecutarEntregaRequest ejecutarEntregaRequest,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/entregas/{entregaId}/ejecucion'.replaceAll('{' r'entregaId' '}', entregaId.toString());
    final _options = Options(
      method: r'POST',
      headers: <String, dynamic>{
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
_bodyData=jsonEncode(ejecutarEntregaRequest);
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

    SalidaEjecucionEntrega? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaEjecucionEntrega, SalidaEjecucionEntrega>(rawData, 'SalidaEjecucionEntrega', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaEjecucionEntrega>(
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

  /// Emitir la orden de desembolso
  /// Una orden VIVA por entrega (R-DES-01) y ninguna a cuenta sin verificar (R-DES-02). Dos ordenes de la misma entrega es pagar dos veces, y no se descubre hasta que alguien concilia el extracto.
  ///
  /// Parameters:
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaOrdenDesembolso] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaOrdenDesembolso] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaOrdenDesembolso>> emitirOrdenDesembolso({ 
    required String idempotencyKey,
    required EntradaOrdenDesembolso entradaOrdenDesembolso,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/desembolsos';
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
_bodyData=jsonEncode(entradaOrdenDesembolso);
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

    SalidaOrdenDesembolso? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaOrdenDesembolso, SalidaOrdenDesembolso>(rawData, 'SalidaOrdenDesembolso', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaOrdenDesembolso>(
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

  /// Liquidar la entrega del turno
  /// Una entrega por turno (R-GRP-01) y el neto NUNCA negativo: que las deducciones superen la bolsa significa que el beneficiario terminaria debiendo por cobrar su turno. Cada deduccion viaja con su tipo y su origen.
  ///
  /// Parameters:
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaLiquidacion] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaLiquidacion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaLiquidacion>> liquidarEntrega({ 
    required String idempotencyKey,
    required EntradaLiquidacion entradaLiquidacion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/entregas';
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
_bodyData=jsonEncode(entradaLiquidacion);
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

    SalidaLiquidacion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaLiquidacion, SalidaLiquidacion>(rawData, 'SalidaLiquidacion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaLiquidacion>(
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

  /// Registrar una cuenta bancaria de destino
  /// El numero NUNCA se guarda en claro (R-SEG-01): la fila lleva el cifrado, un hash con pimienta para buscar, y una version enmascarada. Y la cuenta esta a nombre de quien la registra (R-SEG-02): una cuenta de destino ajena es la forma mas simple de sacar plata de un grupo hacia afuera.
  ///
  /// Parameters:
  /// * [entradaRegistroCuenta] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaRegistroCuenta] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaRegistroCuenta>> registrarCuentaDestino({ 
    required EntradaRegistroCuenta entradaRegistroCuenta,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/cuentas-bancarias';
    final _options = Options(
      method: r'POST',
      headers: <String, dynamic>{
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
_bodyData=jsonEncode(entradaRegistroCuenta);
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

    SalidaRegistroCuenta? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaRegistroCuenta, SalidaRegistroCuenta>(rawData, 'SalidaRegistroCuenta', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaRegistroCuenta>(
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

  /// Verificar la titularidad de la cuenta
  /// Verificada no es utilizable: empieza la ventana de enfriamiento y su fin se GUARDA. Si alguien toma una cuenta ajena, lo primero que hace es cambiar el destino y retirar; la ventana le da al titular real el tiempo de enterarse.
  ///
  /// Parameters:
  /// * [cuentaId] 
  /// * [verificarCuentaDestinoRequest] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaVerificacionCuenta] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaVerificacionCuenta>> verificarCuentaDestino({ 
    required String cuentaId,
    required VerificarCuentaDestinoRequest verificarCuentaDestinoRequest,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/cuentas-bancarias/{cuentaId}/verificacion'.replaceAll('{' r'cuentaId' '}', cuentaId.toString());
    final _options = Options(
      method: r'POST',
      headers: <String, dynamic>{
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
_bodyData=jsonEncode(verificarCuentaDestinoRequest);
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

    SalidaVerificacionCuenta? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaVerificacionCuenta, SalidaVerificacionCuenta>(rawData, 'SalidaVerificacionCuenta', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaVerificacionCuenta>(
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
