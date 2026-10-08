//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

import 'dart:async';

// ignore: unused_import
import 'dart:convert';
import 'package:aportaya_cliente_tarifas/src/deserialize.dart';
import 'package:dio/dio.dart';

import 'package:aportaya_cliente_tarifas/src/model/aceptar_cotizacion200_response.dart';
import 'package:aportaya_cliente_tarifas/src/model/entrada_cobro.dart';
import 'package:aportaya_cliente_tarifas/src/model/entrada_cotizacion.dart';
import 'package:aportaya_cliente_tarifas/src/model/entrada_devengo.dart';
import 'package:aportaya_cliente_tarifas/src/model/entrada_devolucion.dart';
import 'package:aportaya_cliente_tarifas/src/model/entrada_factura.dart';
import 'package:aportaya_cliente_tarifas/src/model/entrada_liquidacion.dart';
import 'package:aportaya_cliente_tarifas/src/model/entrada_publicacion.dart';
import 'package:aportaya_cliente_tarifas/src/model/entrada_segmento.dart';
import 'package:aportaya_cliente_tarifas/src/model/error.dart';
import 'package:aportaya_cliente_tarifas/src/model/poner_tarifario_vigente200_response.dart';
import 'package:aportaya_cliente_tarifas/src/model/resolver_precio_request.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_cobro.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_cotizacion.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_devengo.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_devolucion.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_factura.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_liquidacion.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_publicacion.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_segmento.dart';
import 'package:aportaya_cliente_tarifas/src/model/salida_segmento_aplicado.dart';
import 'package:aportaya_cliente_tarifas/src/model/tarifario_vigente.dart';

class DefaultApi {

  final Dio _dio;

  const DefaultApi(this._dio);

  /// Registrar que el usuario acepto el precio que vio
  /// Sin esto no hay evidencia de consentimiento. Una cotizacion vencida no se acepta: el precio que se acepta tiene que ser el que se mostro.
  ///
  /// Parameters:
  /// * [cotizacionId] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [AceptarCotizacion200Response] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<AceptarCotizacion200Response>> aceptarCotizacion({ 
    required String cotizacionId,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/comisiones/cotizaciones/{cotizacionId}/aceptacion'.replaceAll('{' r'cotizacionId' '}', cotizacionId.toString());
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

    AceptarCotizacion200Response? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<AceptarCotizacion200Response, AceptarCotizacion200Response>(rawData, 'AceptarCotizacion200Response', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<AceptarCotizacion200Response>(
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

  /// Registrar el resultado de un intento de cobro
  /// El cobro lo ejecuta quien mueve el dinero (invariante 12). Aca se anota que paso: tres fallos y el devengo va a cuenta por cobrar, sin borrarse. La deuda no desaparece porque el debito falle.
  ///
  /// Parameters:
  /// * [devengoId] 
  /// * [entradaCobro] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaCobro] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaCobro>> anotarCobroDeComision({ 
    required String devengoId,
    required EntradaCobro entradaCobro,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/comisiones/devengos/{devengoId}/cobros'.replaceAll('{' r'devengoId' '}', devengoId.toString());
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
_bodyData=jsonEncode(entradaCobro);
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

    SalidaCobro? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaCobro, SalidaCobro>(rawData, 'SalidaCobro', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaCobro>(
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

  /// Cerrar el mes de ingresos
  /// Un resultado mensual reproducible desde los devengos, no desde una planilla aparte. Los dias sin cerrar, las excepciones abiertas y el saldo del mayor llegan como entrada: viven en otros esquemas y este servicio no los lee (invariante 11).
  ///
  /// Parameters:
  /// * [periodo] 
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
  Future<Response<SalidaLiquidacion>> cerrarLiquidacion({ 
    required String periodo,
    required EntradaLiquidacion entradaLiquidacion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/tarifas/liquidaciones/{periodo}/cierre'.replaceAll('{' r'periodo' '}', periodo.toString());
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

  /// Cual es el tarifario vigente de un codigo, ahora
  /// La pregunta CU-20 al crear un grupo: sin tarifario vigente no se abre nada (R-CON-07), porque el precio es lo que cada participante acepta al firmar el reglamento. Que no haya ninguno es una respuesta valida —significa que no se puede operar— y por eso &#x60;vigente&#x60; puede venir en falso en vez de fallar.
  ///
  /// Parameters:
  /// * [codigo] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [TarifarioVigente] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<TarifarioVigente>> consultarTarifarioVigente({ 
    required String codigo,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/tarifas/vigentes/{codigo}'.replaceAll('{' r'codigo' '}', codigo.toString());
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

    TarifarioVigente? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<TarifarioVigente, TarifarioVigente>(rawData, 'TarifarioVigente', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<TarifarioVigente>(
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

  /// Calcular y guardar la comision de una operacion
  /// Que el usuario vea el numero final ANTES de aceptar, y que ese numero quede guardado con su desglose. El reclamo tipico no es «me cobraron de mas»: es «nadie me aviso». Si la operacion pertenece a un grupo con tarifa congelada, manda el snapshot (R-TAR-07).
  ///
  /// Parameters:
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaCotizacion] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaCotizacion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaCotizacion>> cotizarComision({ 
    required String idempotencyKey,
    required EntradaCotizacion entradaCotizacion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/comisiones/cotizaciones';
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
_bodyData=jsonEncode(entradaCotizacion);
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

    SalidaCotizacion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaCotizacion, SalidaCotizacion>(rawData, 'SalidaCotizacion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaCotizacion>(
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

  /// Definir un segmento comercial
  /// El criterio es JSON evaluable, no texto libre: «se le aplico porque es buen cliente» no es una explicacion que sirva ante un reclamo. Un criterio que se apoya en datos sensibles o categorias protegidas se rechaza.
  ///
  /// Parameters:
  /// * [entradaSegmento] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaSegmento] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaSegmento>> crearSegmento({ 
    required EntradaSegmento entradaSegmento,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/tarifas/segmentos';
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
_bodyData=jsonEncode(entradaSegmento);
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

    SalidaSegmento? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaSegmento, SalidaSegmento>(rawData, 'SalidaSegmento', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaSegmento>(
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

  /// Reconocer el ingreso cuando ocurre el hecho generador
  /// Separa ganar de cobrar. El devengo es append-only: su estado corriente se DERIVA de los cargos y las devoluciones, no de un UPDATE sobre la fila.
  ///
  /// Parameters:
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaDevengo] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaDevengo] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaDevengo>> devengarComision({ 
    required String idempotencyKey,
    required EntradaDevengo entradaDevengo,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/comisiones/devengos';
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
_bodyData=jsonEncode(entradaDevengo);
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

    SalidaDevengo? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaDevengo, SalidaDevengo>(rawData, 'SalidaDevengo', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaDevengo>(
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

  /// Devolver una comision y emitir la nota de credito
  /// La plata y el papel van atados. La factura NUNCA se edita (R-TAR-10): se emite una nota de credito con su propio CUF. El limite se mide contra lo ya devuelto.
  ///
  /// Parameters:
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [devengoId] 
  /// * [entradaDevolucion] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaDevolucion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaDevolucion>> devolverComision({ 
    required String idempotencyKey,
    required String devengoId,
    required EntradaDevolucion entradaDevolucion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/comisiones/{devengoId}/devoluciones'.replaceAll('{' r'devengoId' '}', devengoId.toString());
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
_bodyData=jsonEncode(entradaDevolucion);
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

    SalidaDevolucion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaDevolucion, SalidaDevolucion>(rawData, 'SalidaDevolucion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaDevolucion>(
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

  /// Emitir el documento fiscal de una comision cobrada
  /// La caida del servicio de impuestos NO detiene la operacion: se emite bajo contingencia con su plazo guardado. La consulta al tercero ocurre fuera de la transaccion (invariante 6).
  ///
  /// Parameters:
  /// * [entradaFactura] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaFactura] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaFactura>> emitirFactura({ 
    required EntradaFactura entradaFactura,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/facturas';
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
_bodyData=jsonEncode(entradaFactura);
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

    SalidaFactura? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaFactura, SalidaFactura>(rawData, 'SalidaFactura', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaFactura>(
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

  /// Poner en vigencia un tarifario cuyo preaviso ya se cumplio
  /// 
  ///
  /// Parameters:
  /// * [tarifarioId] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [PonerTarifarioVigente200Response] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<PonerTarifarioVigente200Response>> ponerTarifarioVigente({ 
    required String tarifarioId,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/tarifas/tarifarios/{tarifarioId}/vigencia'.replaceAll('{' r'tarifarioId' '}', tarifarioId.toString());
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

    PonerTarifarioVigente200Response? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<PonerTarifarioVigente200Response, PonerTarifarioVigente200Response>(rawData, 'PonerTarifarioVigente200Response', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<PonerTarifarioVigente200Response>(
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

  /// Publicar la version siguiente de un tarifario
  /// Un incremento no entra sin preaviso cumplido (R-TAR-08). El anterior no se borra: queda SUSTITUIDO con su vigencia cerrada, porque poder decir que se cobraba en una fecha pasada es lo unico que responde un reclamo viejo.
  ///
  /// Parameters:
  /// * [entradaPublicacion] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaPublicacion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaPublicacion>> publicarTarifario({ 
    required EntradaPublicacion entradaPublicacion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/tarifas/tarifarios';
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
_bodyData=jsonEncode(entradaPublicacion);
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

    SalidaPublicacion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaPublicacion, SalidaPublicacion>(rawData, 'SalidaPublicacion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaPublicacion>(
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

  /// Que segmento rige para un usuario, y por que
  /// No escribe: es una consulta. Si falta un hecho para evaluar el criterio, NO se adivina — se cotiza al precio base y se dice cual falto. Adivinar a favor regala plata y adivinar en contra cobra de mas.
  ///
  /// Parameters:
  /// * [resolverPrecioRequest] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaSegmentoAplicado] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaSegmentoAplicado>> resolverPrecio({ 
    required ResolverPrecioRequest resolverPrecioRequest,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/tarifas/precios/resolver';
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
_bodyData=jsonEncode(resolverPrecioRequest);
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

    SalidaSegmentoAplicado? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaSegmentoAplicado, SalidaSegmentoAplicado>(rawData, 'SalidaSegmentoAplicado', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaSegmentoAplicado>(
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
