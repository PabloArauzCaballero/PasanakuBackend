//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

import 'dart:async';

// ignore: unused_import
import 'dart:convert';
import 'package:aportaya_cliente_identidad/src/deserialize.dart';
import 'package:dio/dio.dart';

import 'package:aportaya_cliente_identidad/src/model/archivo_del_expediente.dart';
import 'package:aportaya_cliente_identidad/src/model/decision_de_verificacion.dart';
import 'package:aportaya_cliente_identidad/src/model/enlace_de_foto.dart';
import 'package:aportaya_cliente_identidad/src/model/entrada_autenticacion.dart';
import 'package:aportaya_cliente_identidad/src/model/entrada_registro.dart';
import 'package:aportaya_cliente_identidad/src/model/entrada_titularidad.dart';
import 'package:aportaya_cliente_identidad/src/model/entrada_token_de_invitacion.dart';
import 'package:aportaya_cliente_identidad/src/model/error.dart';
import 'package:aportaya_cliente_identidad/src/model/expediente_en_revision.dart';
import 'package:aportaya_cliente_identidad/src/model/salida_autenticacion.dart';
import 'package:aportaya_cliente_identidad/src/model/salida_registro.dart';
import 'package:aportaya_cliente_identidad/src/model/salida_titularidad.dart';
import 'package:aportaya_cliente_identidad/src/model/salida_token_de_invitacion.dart';
import 'package:aportaya_cliente_identidad/src/model/usuario_encontrado.dart';

class DefaultApi {

  final Dio _dio;

  const DefaultApi(this._dio);

  /// Autenticar con MFA y registrar dispositivo
  /// Ruta publica: es el momento en que todavia no hay sesion. NO lleva Idempotency-Key a proposito — cada intento es un hecho distinto y queda registrado en intento_autenticacion, incluidos los fallidos.
  ///
  /// Parameters:
  /// * [entradaAutenticacion] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaAutenticacion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaAutenticacion>> autenticar({ 
    required EntradaAutenticacion entradaAutenticacion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/sesiones';
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
_bodyData=jsonEncode(entradaAutenticacion);
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

    SalidaAutenticacion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaAutenticacion, SalidaAutenticacion>(rawData, 'SalidaAutenticacion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaAutenticacion>(
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

  /// Si ese telefono ya tiene cuenta
  /// La pregunta &#x60;grupos&#x60; antes de invitar (CU-69): invitar a quien ya es participante es ruido, y para saberlo hay que poder ir del telefono al usuario. Devuelve el identificador y nada mas —ni nombre, ni estado, ni cuando entro—. Exige permiso: una ruta que dice si un telefono tiene cuenta es, sin permiso, una forma de enumerar clientes.
  ///
  /// Parameters:
  /// * [telefono] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [UsuarioEncontrado] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<UsuarioEncontrado>> buscarPorTelefono({ 
    required String telefono,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/usuarios/por-telefono';
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
      r'telefono': telefono,
    };

    final _response = await _dio.request<Object>(
      _path,
      options: _options,
      queryParameters: _queryParameters,
      cancelToken: cancelToken,
      onSendProgress: onSendProgress,
      onReceiveProgress: onReceiveProgress,
    );

    UsuarioEncontrado? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<UsuarioEncontrado, UsuarioEncontrado>(rawData, 'UsuarioEncontrado', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<UsuarioEncontrado>(
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

  /// El enlace de un solo uso con el que se invita a alguien a un grupo
  /// &#x60;token_verificacion&#x60; es de identidad y su politica —vigencia, canales, cuantos reenvios— esta sembrada bajo el proposito INVITACION_GRUPO. &#x60;grupos&#x60; no puede escribir esa tabla (invariante 11): pide el token y guarda su identificador. Se guarda el hash, nunca el token: quien tenga la base no puede reconstruir los enlaces vivos. El valor en claro se devuelve una sola vez, a quien lo va a enviar.
  ///
  /// Parameters:
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaTokenDeInvitacion] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaTokenDeInvitacion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaTokenDeInvitacion>> emitirTokenDeInvitacion({ 
    required String idempotencyKey,
    required EntradaTokenDeInvitacion entradaTokenDeInvitacion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/usuarios/tokens/invitacion';
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
_bodyData=jsonEncode(entradaTokenDeInvitacion);
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

    SalidaTokenDeInvitacion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaTokenDeInvitacion, SalidaTokenDeInvitacion>(rawData, 'SalidaTokenDeInvitacion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaTokenDeInvitacion>(
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

  /// La cola de expedientes esperando una decision humana
  /// El portal de riesgo. Devuelve quien espera, desde cuando y que fotos tiene cargadas — nunca las fotos en si, que se piden una por una y quedan en la bitacora como cualquier lectura de dato personal.
  ///
  /// Parameters:
  /// * [estado] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [List<ExpedienteEnRevision>] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<List<ExpedienteEnRevision>>> listarVerificaciones({ 
    String? estado,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/identidad/verificaciones';
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
      if (estado != null) r'estado': estado,
    };

    final _response = await _dio.request<Object>(
      _path,
      options: _options,
      queryParameters: _queryParameters,
      cancelToken: cancelToken,
      onSendProgress: onSendProgress,
      onReceiveProgress: onReceiveProgress,
    );

    List<ExpedienteEnRevision>? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<List<ExpedienteEnRevision>, ExpedienteEnRevision>(rawData, 'List<ExpedienteEnRevision>', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<List<ExpedienteEnRevision>>(
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

  /// Registro y apertura de billetera
  /// Una transaccion: usuario, KYC, debida diligencia, calificacion de riesgo, expediente y cuenta de billetera. Ruta publica: es el unico momento en que todavia no hay sesion.
  ///
  /// Parameters:
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaRegistro] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaRegistro] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaRegistro>> registrarUsuario({ 
    required String idempotencyKey,
    required EntradaRegistro entradaRegistro,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/usuarios';
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
_bodyData=jsonEncode(entradaRegistro);
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

    SalidaRegistro? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaRegistro, SalidaRegistro>(rawData, 'SalidaRegistro', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaRegistro>(
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

  /// Aprobar o rechazar la verificacion, a mano
  /// La decision es de una persona y queda con su nombre: &#x60;revisada_por&#x60; guarda quien decidio y &#x60;motivo_rechazo&#x60; por que. Rechazar sin motivo no se admite — a quien le rechazan la cuenta tiene derecho a saber por que.
  ///
  /// Parameters:
  /// * [verificacionId] 
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [decisionDeVerificacion] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [ExpedienteEnRevision] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<ExpedienteEnRevision>> resolverVerificacion({ 
    required String verificacionId,
    required String idempotencyKey,
    required DecisionDeVerificacion decisionDeVerificacion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/identidad/verificaciones/{verificacionId}/decision'.replaceAll('{' r'verificacionId' '}', verificacionId.toString());
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
_bodyData=jsonEncode(decisionDeVerificacion);
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

    ExpedienteEnRevision? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<ExpedienteEnRevision, ExpedienteEnRevision>(rawData, 'ExpedienteEnRevision', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<ExpedienteEnRevision>(
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

  /// Sube una foto del documento o la prueba de vida
  /// La foto NO se guarda en el disco del servicio: va al servidor de archivos y en la fila queda una clave de objeto (ADR-034). El tipo se decide leyendo los primeros bytes, no por la extension ni por el Content-Type que manda el cliente. Devuelve la clave y el SHA-256 para que el expediente quede trazable.
  ///
  /// Parameters:
  /// * [usuarioId] 
  /// * [idempotencyKey] 
  /// * [cara] - Que parte del expediente es esta foto.
  /// * [archivo] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [ArchivoDelExpediente] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<ArchivoDelExpediente>> subirDocumento({ 
    required String usuarioId,
    required String idempotencyKey,
    required String cara,
    required MultipartFile archivo,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/usuarios/{usuarioId}/documentos'.replaceAll('{' r'usuarioId' '}', usuarioId.toString());
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
      contentType: 'multipart/form-data',
      validateStatus: validateStatus,
    );

    dynamic _bodyData;

    try {

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

    ArchivoDelExpediente? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<ArchivoDelExpediente, ArchivoDelExpediente>(rawData, 'ArchivoDelExpediente', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<ArchivoDelExpediente>(
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

  /// Los bytes de una foto del expediente, servidos por el servicio dueno
  /// El binario, no un enlace. &#x60;verFotoDelExpediente&#x60; devuelve una URL prefirmada contra el almacen, y el almacen NO es publico: vive en la red interna, sin puerto publicado ni dominio, que es como lo quiere el ADR-034 —«el binario no se sirve nunca directo»—. Esa URL sirve entre servicios y es inservible en un navegador, que no resuelve &#x60;minio:9000&#x60;. Aca lo entrega el servicio dueno de la tabla, que valida sesion y permiso antes de leer un solo byte, y la lectura queda en la bitacora como cualquier otra lectura de dato personal. Son fotos de cedula de a una, no megabytes de video: proxearlas cuesta menos que abrir el almacen al mundo.
  ///
  /// Parameters:
  /// * [verificacionId] 
  /// * [cara] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [MultipartFile] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<MultipartFile>> verContenidoDeFoto({ 
    required String verificacionId,
    required String cara,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/identidad/verificaciones/{verificacionId}/fotos/{cara}/contenido'.replaceAll('{' r'verificacionId' '}', verificacionId.toString()).replaceAll('{' r'cara' '}', cara.toString());
    final _options = Options(
      method: r'GET',
      responseType: ResponseType.bytes,
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

    MultipartFile? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<MultipartFile, MultipartFile>(rawData, 'MultipartFile', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<MultipartFile>(
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

  /// Un enlace de vigencia corta para mirar una foto del expediente
  /// Devuelve una URL temporal, no el binario: asi el servidor no proxea megabytes y el enlace muere solo. Cada llamada es una lectura de dato personal y queda registrada.
  ///
  /// Parameters:
  /// * [verificacionId] 
  /// * [cara] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [EnlaceDeFoto] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<EnlaceDeFoto>> verFotoDelExpediente({ 
    required String verificacionId,
    required String cara,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/identidad/verificaciones/{verificacionId}/fotos/{cara}'.replaceAll('{' r'verificacionId' '}', verificacionId.toString()).replaceAll('{' r'cara' '}', cara.toString());
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

    EnlaceDeFoto? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<EnlaceDeFoto, EnlaceDeFoto>(rawData, 'EnlaceDeFoto', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<EnlaceDeFoto>(
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

  /// Si el nombre y el documento declarados son los del titular
  /// Contesta si coinciden; NO dice cuales son. Devolver el nombre y el documento guardados convertiria esta ruta en un directorio de clientes para cualquier servicio comprometido. El documento ademas se guarda como hash con pimienta: lo que se comparan son dos hashes, nunca dos numeros. La usa &#x60;entregas&#x60; antes de registrar una cuenta bancaria — R-SEG-02 exige que la cuenta sea de quien la registra, y ese dato vive aca.
  ///
  /// Parameters:
  /// * [usuarioId] 
  /// * [entradaTitularidad] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaTitularidad] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaTitularidad>> verificarTitularidad({ 
    required String usuarioId,
    required EntradaTitularidad entradaTitularidad,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/usuarios/{usuarioId}/titularidad/verificacion'.replaceAll('{' r'usuarioId' '}', usuarioId.toString());
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
_bodyData=jsonEncode(entradaTitularidad);
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

    SalidaTitularidad? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaTitularidad, SalidaTitularidad>(rawData, 'SalidaTitularidad', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaTitularidad>(
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
