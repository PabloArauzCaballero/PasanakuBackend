//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

import 'dart:async';

// ignore: unused_import
import 'dart:convert';
import 'package:aportaya_cliente_organizador/src/deserialize.dart';
import 'package:dio/dio.dart';

import 'package:aportaya_cliente_organizador/src/model/entrada_apelacion.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_aprobacion.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_contrato.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_ejecucion.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_evaluacion.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_postulacion.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_programacion.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_regla.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_resolucion.dart';
import 'package:aportaya_cliente_organizador/src/model/entrada_sancion.dart';
import 'package:aportaya_cliente_organizador/src/model/error.dart';
import 'package:aportaya_cliente_organizador/src/model/firmar_contrato_request.dart';
import 'package:aportaya_cliente_organizador/src/model/habilitacion.dart';
import 'package:aportaya_cliente_organizador/src/model/rescindir_contrato_request.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_apelacion.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_contrato.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_ejecucion.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_evaluacion.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_firma.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_habilitacion.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_postulacion.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_programacion.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_regla.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_rescision.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_resolucion.dart';
import 'package:aportaya_cliente_organizador/src/model/salida_sancion.dart';

class DefaultApi {

  final Dio _dio;

  const DefaultApi(this._dio);

  /// Encender la regla
  /// Se revuelve a comprobar la coherencia y la prioridad: dos reglas activas con el mismo disparador y prioridad hacen que el orden de ejecucion dependa de como la base devuelva las filas.
  ///
  /// Parameters:
  /// * [reglaId] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaRegla] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaRegla>> activarRegla({ 
    required String reglaId,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/automatizacion/reglas/{reglaId}/activacion'.replaceAll('{' r'reglaId' '}', reglaId.toString());
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

    SalidaRegla? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaRegla, SalidaRegla>(rawData, 'SalidaRegla', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaRegla>(
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

  /// Registrar el resultado de una ejecucion
  /// La accion real la ejecuta el servicio dueno del efecto; aca se registra que paso. Ejecutarla desde este servicio seria escribir esquemas ajenos (invariante 11).
  ///
  /// Parameters:
  /// * [tareaId] 
  /// * [entradaEjecucion] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaEjecucion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaEjecucion>> anotarEjecucion({ 
    required String tareaId,
    required EntradaEjecucion entradaEjecucion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/automatizacion/tareas/{tareaId}/ejecuciones'.replaceAll('{' r'tareaId' '}', tareaId.toString());
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
_bodyData=jsonEncode(entradaEjecucion);
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

    SalidaEjecucion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaEjecucion, SalidaEjecucion>(rawData, 'SalidaEjecucion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaEjecucion>(
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

  /// Presentar la apelacion
  /// La sancion pasa a APELADA y no se puede dar por cumplida. La FILA de la apelacion se escribe al resolverla: la base no admite una apelacion abierta (hueco H-7 del informe del carril).
  ///
  /// Parameters:
  /// * [sancionId] 
  /// * [entradaApelacion] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaApelacion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaApelacion>> apelarSancion({ 
    required String sancionId,
    required EntradaApelacion entradaApelacion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/organizadores/sanciones/{sancionId}/apelacion'.replaceAll('{' r'sancionId' '}', sancionId.toString());
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
_bodyData=jsonEncode(entradaApelacion);
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

    SalidaApelacion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaApelacion, SalidaApelacion>(rawData, 'SalidaApelacion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaApelacion>(
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

  /// Aprobar la postulacion y crear al organizador
  /// Quien aprueba no puede ser quien postulo (R-SEG-04). El organizador nace en CAPACITACION_PENDIENTE: aprobar y entregar el acceso al fondo en el mismo acto saltaria el paso donde se le explica que responsabilidad esta tomando.
  ///
  /// Parameters:
  /// * [solicitudId] 
  /// * [entradaAprobacion] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaHabilitacion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaHabilitacion>> aprobarPostulacion({ 
    required String solicitudId,
    required EntradaAprobacion entradaAprobacion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/organizadores/postulaciones/{solicitudId}/aprobacion'.replaceAll('{' r'solicitudId' '}', solicitudId.toString());
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
_bodyData=jsonEncode(entradaAprobacion);
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

    SalidaHabilitacion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaHabilitacion, SalidaHabilitacion>(rawData, 'SalidaHabilitacion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaHabilitacion>(
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

  /// La confirmacion humana que exige una accion sensible
  /// 
  ///
  /// Parameters:
  /// * [tareaId] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaProgramacion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaProgramacion>> aprobarTarea({ 
    required String tareaId,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/automatizacion/tareas/{tareaId}/aprobacion'.replaceAll('{' r'tareaId' '}', tareaId.toString());
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

    SalidaProgramacion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaProgramacion, SalidaProgramacion>(rawData, 'SalidaProgramacion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaProgramacion>(
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

  /// Si el organizador esta habilitado, y hasta donde
  /// La pregunta CU-20 antes de dejar crear un grupo: un organizador suspendido no abre pasanakus nuevos. &#x60;grupos&#x60; no puede leer este esquema (invariante 11). Devuelve tambien los limites del nivel porque quien crea el grupo los necesita en el mismo momento, y dos viajes para dos datos del mismo renglon es un viaje de mas.
  ///
  /// Parameters:
  /// * [organizadorId] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [Habilitacion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<Habilitacion>> consultarHabilitacion({ 
    required String organizadorId,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/organizadores/{organizadorId}/habilitacion'.replaceAll('{' r'organizadorId' '}', organizadorId.toString());
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

    Habilitacion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<Habilitacion, Habilitacion>(rawData, 'Habilitacion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<Habilitacion>(
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

  /// Definir una regla de automatizacion
  /// Las acciones que mueven plata ajena exigen confirmacion humana (R-ORG-06). La regla nace INACTIVA: publicarla y encenderla en el mismo acto no deja momento para revisar que la condicion diga lo que se cree que dice.
  ///
  /// Parameters:
  /// * [entradaRegla] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaRegla] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaRegla>> definirRegla({ 
    required EntradaRegla entradaRegla,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/automatizacion/reglas';
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
_bodyData=jsonEncode(entradaRegla);
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

    SalidaRegla? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaRegla, SalidaRegla>(rawData, 'SalidaRegla', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaRegla>(
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

  /// Emitir el contrato de organizador
  /// Un solo contrato vigente por organizador (R-ORG-02). Dos permiten invocar el texto que mas convenga ante un incumplimiento.
  ///
  /// Parameters:
  /// * [organizadorId] 
  /// * [entradaContrato] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaContrato] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaContrato>> emitirContrato({ 
    required String organizadorId,
    required EntradaContrato entradaContrato,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/organizadores/{organizadorId}/contratos'.replaceAll('{' r'organizadorId' '}', organizadorId.toString());
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
_bodyData=jsonEncode(entradaContrato);
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

    SalidaContrato? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaContrato, SalidaContrato>(rawData, 'SalidaContrato', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaContrato>(
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

  /// Evaluar el desempeno de un periodo
  /// Una evaluacion por organizador y periodo (R-ORG-04). El puntaje sale de metricas con su peso, y las metricas se guardan: si alguien va a perder su habilitacion por un numero, ese numero tiene que poder abrirse. El descenso se aplica; el ascenso queda SUGERIDO.
  ///
  /// Parameters:
  /// * [organizadorId] 
  /// * [entradaEvaluacion] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaEvaluacion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaEvaluacion>> evaluarDesempeno({ 
    required String organizadorId,
    required EntradaEvaluacion entradaEvaluacion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/organizadores/{organizadorId}/evaluaciones'.replaceAll('{' r'organizadorId' '}', organizadorId.toString());
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
_bodyData=jsonEncode(entradaEvaluacion);
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

    SalidaEvaluacion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaEvaluacion, SalidaEvaluacion>(rawData, 'SalidaEvaluacion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaEvaluacion>(
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

  /// Firmar el contrato
  /// Lo firma el propio organizador: una firma puesta por otro no obliga a nadie. Firmar dos veces no sobrescribe la evidencia de cuando y con que token se firmo.
  ///
  /// Parameters:
  /// * [contratoId] 
  /// * [firmarContratoRequest] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaFirma] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaFirma>> firmarContrato({ 
    required String contratoId,
    required FirmarContratoRequest firmarContratoRequest,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/organizadores/contratos/{contratoId}/firma'.replaceAll('{' r'contratoId' '}', contratoId.toString());
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
_bodyData=jsonEncode(firmarContratoRequest);
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

    SalidaFirma? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaFirma, SalidaFirma>(rawData, 'SalidaFirma', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaFirma>(
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

  /// Habilitar tras completar la capacitacion
  /// 
  ///
  /// Parameters:
  /// * [organizadorId] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaHabilitacion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaHabilitacion>> habilitarOrganizador({ 
    required String organizadorId,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/organizadores/{organizadorId}/habilitacion'.replaceAll('{' r'organizadorId' '}', organizadorId.toString());
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

    SalidaHabilitacion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaHabilitacion, SalidaHabilitacion>(rawData, 'SalidaHabilitacion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaHabilitacion>(
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

  /// Postular para administrar grupos
  /// Un organizador administra la plata de otros: la habilitacion es la ultima puerta antes de que alguien tenga acceso al fondo de un grupo entero. Los requisitos son catalogo, y un obligatorio SIN DATO no se da por cumplido.
  ///
  /// Parameters:
  /// * [entradaPostulacion] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaPostulacion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaPostulacion>> postularOrganizador({ 
    required EntradaPostulacion entradaPostulacion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/organizadores/postulaciones';
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
_bodyData=jsonEncode(entradaPostulacion);
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

    SalidaPostulacion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaPostulacion, SalidaPostulacion>(rawData, 'SalidaPostulacion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaPostulacion>(
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

  /// Programar una tarea automatizada
  /// Una tarea por clave de idempotencia (R-ORG-07). La clave es determinista —regla, grupo y minuto—: si dependiera del reloj de la corrida, cada reintento del planificador generaria una tarea nueva.
  ///
  /// Parameters:
  /// * [entradaProgramacion] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaProgramacion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaProgramacion>> programarTarea({ 
    required EntradaProgramacion entradaProgramacion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/automatizacion/tareas';
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
_bodyData=jsonEncode(entradaProgramacion);
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

    SalidaProgramacion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaProgramacion, SalidaProgramacion>(rawData, 'SalidaProgramacion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaProgramacion>(
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

  /// Rescindir el contrato
  /// No se rescinde con grupos en curso: irse dejando participantes a mitad del ciclo es lo que el contrato existe para que no pase sin consecuencias.
  ///
  /// Parameters:
  /// * [contratoId] 
  /// * [rescindirContratoRequest] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaRescision] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaRescision>> rescindirContrato({ 
    required String contratoId,
    required RescindirContratoRequest rescindirContratoRequest,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/organizadores/contratos/{contratoId}/rescision'.replaceAll('{' r'contratoId' '}', contratoId.toString());
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
_bodyData=jsonEncode(rescindirContratoRequest);
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

    SalidaRescision? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaRescision, SalidaRescision>(rawData, 'SalidaRescision', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaRescision>(
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

  /// Resolver la apelacion
  /// La resuelve quien NO aplico la sancion (R-ORG-05). Sin eso, apelar es pedirle a la misma persona que se desdiga.
  ///
  /// Parameters:
  /// * [sancionId] 
  /// * [entradaResolucion] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaResolucion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaResolucion>> resolverApelacion({ 
    required String sancionId,
    required EntradaResolucion entradaResolucion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/organizadores/sanciones/{sancionId}/resolucion'.replaceAll('{' r'sancionId' '}', sancionId.toString());
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
_bodyData=jsonEncode(entradaResolucion);
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

    SalidaResolucion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaResolucion, SalidaResolucion>(rawData, 'SalidaResolucion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaResolucion>(
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

  /// Aplicar una sancion
  /// Sin motivo escrito no hay sancion: «bajo desempeno» a secas no se puede apelar porque no dice contra que defenderse. El plazo para apelar se calcula al sancionar y no se recalcula despues.
  ///
  /// Parameters:
  /// * [organizadorId] 
  /// * [entradaSancion] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaSancion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaSancion>> sancionarOrganizador({ 
    required String organizadorId,
    required EntradaSancion entradaSancion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/organizadores/{organizadorId}/sanciones'.replaceAll('{' r'organizadorId' '}', organizadorId.toString());
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
_bodyData=jsonEncode(entradaSancion);
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

    SalidaSancion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaSancion, SalidaSancion>(rawData, 'SalidaSancion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaSancion>(
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
