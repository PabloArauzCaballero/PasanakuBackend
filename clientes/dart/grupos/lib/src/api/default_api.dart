//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

import 'dart:async';

// ignore: unused_import
import 'dart:convert';
import 'package:aportaya_cliente_grupos/src/deserialize.dart';
import 'package:dio/dio.dart';

import 'package:aportaya_cliente_grupos/src/model/actividad_en_grupos.dart';
import 'package:aportaya_cliente_grupos/src/model/alias_resuelto.dart';
import 'package:aportaya_cliente_grupos/src/model/compromiso_de_sorteo.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_acuerdo.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_compromiso.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_grupo.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_invitacion.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_permuta.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_postulacion.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_retiro.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_revelacion.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_traspaso.dart';
import 'package:aportaya_cliente_grupos/src/model/entrada_voto.dart';
import 'package:aportaya_cliente_grupos/src/model/error.dart';
import 'package:aportaya_cliente_grupos/src/model/paquete_del_sorteo.dart';
import 'package:aportaya_cliente_grupos/src/model/revelacion_de_sorteo.dart';
import 'package:aportaya_cliente_grupos/src/model/salida_acuerdo.dart';
import 'package:aportaya_cliente_grupos/src/model/salida_grupo.dart';
import 'package:aportaya_cliente_grupos/src/model/salida_invitacion.dart';
import 'package:aportaya_cliente_grupos/src/model/salida_permuta.dart';
import 'package:aportaya_cliente_grupos/src/model/salida_plazo_habil.dart';
import 'package:aportaya_cliente_grupos/src/model/salida_postulacion.dart';
import 'package:aportaya_cliente_grupos/src/model/salida_retiro.dart';
import 'package:aportaya_cliente_grupos/src/model/salida_traspaso.dart';

class DefaultApi {

  final Dio _dio;

  const DefaultApi(this._dio);

  /// Fecha límite en días hábiles, con los días que se saltearon
  /// El resultado se GUARDA donde se use y no se recalcula: si despues se declara un feriado, ese plazo no se mueve. Devuelve tambien que dias se saltearon, porque un plazo que no se puede explicar no se puede defender. Nota de ruta: el caso de uso dice &#x60;/calendario&#x60;, que no esta entre los prefijos reservados de &#x60;grupos&#x60; (/grupos, /turnos, /acuerdos). Va bajo /grupos/calendario para no invadir un prefijo sin dueno.
  ///
  /// Parameters:
  /// * [desde] 
  /// * [dias] 
  /// * [alcance] 
  /// * [referenciaId] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaPlazoHabil] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaPlazoHabil>> calcularPlazoHabil({ 
    required DateTime desde,
    required int dias,
    required String alcance,
    String? referenciaId,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/grupos/calendario/calcular';
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
      r'desde': desde,
      r'dias': dias,
      r'alcance': alcance,
      if (referenciaId != null) r'referenciaId': referenciaId,
    };

    final _response = await _dio.request<Object>(
      _path,
      options: _options,
      queryParameters: _queryParameters,
      cancelToken: cancelToken,
      onSendProgress: onSendProgress,
      onReceiveProgress: onReceiveProgress,
    );

    SalidaPlazoHabil? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaPlazoHabil, SalidaPlazoHabil>(rawData, 'SalidaPlazoHabil', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaPlazoHabil>(
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

  /// Publicar el compromiso del sorteo
  /// Publica SOLO el hash de la semilla. Sin esa separacion, quien ejecuta el sorteo podria probar semillas hasta que salga el orden que le conviene: el compromiso es lo que convierte «confia en nosotros» en «verificalo vos».
  ///
  /// Parameters:
  /// * [grupoId] 
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaCompromiso] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [CompromisoDeSorteo] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<CompromisoDeSorteo>> comprometerSorteo({ 
    required String grupoId,
    required String idempotencyKey,
    EntradaCompromiso? entradaCompromiso,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/grupos/{grupoId}/sorteo'.replaceAll('{' r'grupoId' '}', grupoId.toString());
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
_bodyData=jsonEncode(entradaCompromiso);
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

    CompromisoDeSorteo? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<CompromisoDeSorteo, CompromisoDeSorteo>(rawData, 'CompromisoDeSorteo', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<CompromisoDeSorteo>(
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

  /// Si la persona participa hoy de algun grupo activo
  /// La pregunta &#x60;nucleo-financiero&#x60; antes de cerrar una billetera (CU-16): cerrar la cuenta de alguien que todavia debe aportar a un pasanaku deja al grupo sin de donde cobrarle.
  ///
  /// Parameters:
  /// * [usuarioId] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [ActividadEnGrupos] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<ActividadEnGrupos>> consultarActividad({ 
    required String usuarioId,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/grupos/participantes/{usuarioId}/actividad'.replaceAll('{' r'usuarioId' '}', usuarioId.toString());
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

    ActividadEnGrupos? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<ActividadEnGrupos, ActividadEnGrupos>(rawData, 'ActividadEnGrupos', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<ActividadEnGrupos>(
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

  /// El paquete publicado de un sorteo, para poder recomputarlo
  /// La pregunta &#x60;transparencia&#x60; para CU-61. Trae lo que hace falta para rehacer el sorteo desde afuera: el hash comprometido, la semilla revelada, las entropias, el metodo y el orden publicado. Antes del revelado la semilla viaja nula, y ese es el punto: comprometer y revelar existen para que nadie —nosotros incluidos— pueda elegir el resultado despues de conocerlo.
  ///
  /// Parameters:
  /// * [sorteoId] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [PaqueteDelSorteo] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<PaqueteDelSorteo>> consultarPaqueteDelSorteo({ 
    required String sorteoId,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/grupos/sorteos/{sorteoId}/paquete'.replaceAll('{' r'sorteoId' '}', sorteoId.toString());
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

    PaqueteDelSorteo? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<PaqueteDelSorteo, PaqueteDelSorteo>(rawData, 'PaqueteDelSorteo', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<PaqueteDelSorteo>(
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

  /// Crear el grupo y disparar el congelamiento del tarifario
  /// El grupo nace BORRADOR y emite &#x60;grupos.grupo_creado&#x60;; &#x60;tarifas&#x60; congela el precio y &#x60;nucleo-financiero&#x60; abre la cuenta del grupo al consumirlo. Pasa a ABIERTO_A_INSCRIPCION cuando los dos respondieron. Que nazca en borrador no es un rodeo: un grupo sin precio congelado no puede recibir a nadie, porque el precio es justamente lo que cada participante acepta al firmar el reglamento. Dejarlo abierto antes seria pedirle a alguien que se comprometa a un costo que todavia no existe. La cuenta del grupo tiene por titular al GRUPO y nunca al organizador (R-GRP-04): si fuera del organizador, la plata del grupo estaria a nombre de una persona.
  ///
  /// Parameters:
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaGrupo] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaGrupo] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaGrupo>> crearGrupo({ 
    required String idempotencyKey,
    required EntradaGrupo entradaGrupo,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/grupos';
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
_bodyData=jsonEncode(entradaGrupo);
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

    SalidaGrupo? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaGrupo, SalidaGrupo>(rawData, 'SalidaGrupo', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaGrupo>(
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

  /// Invitar a un contacto, con token de un solo uso
  /// Si el destinatario esta suprimido NO se envia nada y se responde como si hubiera salido bien: decir «esa persona pidio no recibir mensajes» ya cuenta algo de ella a quien no tiene por que saberlo. El mensaje dice quien invita y a que grupo, con el monto y la periodicidad — nadie deberia aceptar un compromiso de dinero sin saber cual— y no revela datos de los otros integrantes. El token lo emite &#x60;identidad&#x60;: aca se recibe su identificador.
  ///
  /// Parameters:
  /// * [grupoId] 
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaInvitacion] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaInvitacion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaInvitacion>> invitarAlGrupo({ 
    required String grupoId,
    required String idempotencyKey,
    required EntradaInvitacion entradaInvitacion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/grupos/{grupoId}/invitaciones'.replaceAll('{' r'grupoId' '}', grupoId.toString());
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
_bodyData=jsonEncode(entradaInvitacion);
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

    SalidaInvitacion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaInvitacion, SalidaInvitacion>(rawData, 'SalidaInvitacion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaInvitacion>(
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

  /// Postular a un grupo, con el motivo legible del puntaje
  /// El postulante ve POR QUE se le propuso ese grupo —monto compatible, gente de su zona, riesgo parecido— y nunca los datos de los demas. Un puntaje sin explicacion es un numero que nadie puede discutir, y un emparejamiento que nadie puede discutir es uno que se acepta por resignacion. Los pesos del criterio son datos CON VIGENCIA, no constantes: hay que poder explicar con que pesos se armo un grupo de hace seis meses.
  ///
  /// Parameters:
  /// * [grupoId] 
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
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
  Future<Response<SalidaPostulacion>> postularAlGrupo({ 
    required String grupoId,
    required String idempotencyKey,
    required EntradaPostulacion entradaPostulacion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/grupos/{grupoId}/postulaciones'.replaceAll('{' r'grupoId' '}', grupoId.toString());
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

  /// Proponer un acuerdo y abrir su votación
  /// Ningun efecto colectivo se ejecuta sin acuerdo aprobado, y el efecto va en la MISMA transaccion que la aprobacion: si el efecto falla, el acuerdo no queda aprobado sin su efecto — que seria un grupo convencido de que decidio algo que no ocurrio.
  ///
  /// Parameters:
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaAcuerdo] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaAcuerdo] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaAcuerdo>> proponerAcuerdo({ 
    required String idempotencyKey,
    required EntradaAcuerdo entradaAcuerdo,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/acuerdos';
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
_bodyData=jsonEncode(entradaAcuerdo);
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

    SalidaAcuerdo? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaAcuerdo, SalidaAcuerdo>(rawData, 'SalidaAcuerdo', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaAcuerdo>(
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

  /// A que persona apunta un alias
  /// La pregunta &#x60;nucleo-financiero&#x60; para transferir a un alias (CU-12). El alias vive en &#x60;grupos.participante&#x60; y ese esquema no se lee desde afuera (invariante 11). Devuelve el identificador de la persona y nada mas — ni el nombre, ni el telefono, ni la cuenta. La cuenta la resuelve quien la posee: &#x60;grupos&#x60; no conoce &#x60;nucleo_financiero.cuenta_billetera&#x60;, y esta ruta no es un directorio.
  ///
  /// Parameters:
  /// * [alias] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [AliasResuelto] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<AliasResuelto>> resolverAlias({ 
    required String alias,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/grupos/participantes/alias/{alias}'.replaceAll('{' r'alias' '}', alias.toString());
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

    AliasResuelto? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<AliasResuelto, AliasResuelto>(rawData, 'AliasResuelto', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<AliasResuelto>(
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

  /// Revelar la semilla y crear los turnos
  /// Verifica que SHA256(semilla || entropias) sea el hash publicado y recien entonces crea los turnos, todo en una transaccion: o hay turnos completos o no hay ninguno. Si el hash no cierra, el sorteo queda ANULADO — jamas se publica un resultado que no verifica.
  ///
  /// Parameters:
  /// * [grupoId] 
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaRevelacion] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [RevelacionDeSorteo] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<RevelacionDeSorteo>> revelarSorteo({ 
    required String grupoId,
    required String idempotencyKey,
    required EntradaRevelacion entradaRevelacion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/grupos/{grupoId}/sorteo/revelacion'.replaceAll('{' r'grupoId' '}', grupoId.toString());
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
_bodyData=jsonEncode(entradaRevelacion);
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

    RevelacionDeSorteo? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<RevelacionDeSorteo, RevelacionDeSorteo>(rawData, 'RevelacionDeSorteo', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<RevelacionDeSorteo>(
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

  /// Solicitar la permuta de dos turnos
  /// Sin aceptacion de la contraparte no hay nada: la solicitud nace PENDIENTE. Si alguna parte tiene deuda no se permuta — adelantar el cobro de un moroso lo paga el grupo entero.
  ///
  /// Parameters:
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaPermuta] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaPermuta] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaPermuta>> solicitarPermuta({ 
    required String idempotencyKey,
    required EntradaPermuta entradaPermuta,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/turnos/permutas';
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
_bodyData=jsonEncode(entradaPermuta);
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

    SalidaPermuta? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaPermuta, SalidaPermuta>(rawData, 'SalidaPermuta', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaPermuta>(
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

  /// Solicitar el retiro y ver la posición exacta antes de confirmar
  /// El numero se calcula y se MUESTRA ANTES de confirmar, con su desglose. Un retiro donde el monto aparece despues es un retiro que alguien va a discutir, y con razon. Acreedora sin reemplazo se liquida AL CIERRE DEL CICLO, no antes: sacar plata de la bolsa a mitad de camino perjudica a los que faltan cobrar.
  ///
  /// Parameters:
  /// * [grupoId] 
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaRetiro] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaRetiro] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaRetiro>> solicitarRetiro({ 
    required String grupoId,
    required String idempotencyKey,
    required EntradaRetiro entradaRetiro,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/grupos/{grupoId}/retiros'.replaceAll('{' r'grupoId' '}', grupoId.toString());
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
_bodyData=jsonEncode(entradaRetiro);
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

    SalidaRetiro? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaRetiro, SalidaRetiro>(rawData, 'SalidaRetiro', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaRetiro>(
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

  /// Traspasar un cupo a otro participante
  /// El TURNO no se toca: la posicion en el calendario es del cupo, no de la persona. Si se moviera, traspasar seria una forma de adelantar el propio turno y el sorteo dejaria de significar nada. Y la deuda no viaja con el cupo: las obligaciones vencidas se quedan con quien las genero. Sin eso, un cupo seria una forma de venderle a alguien la deuda de otro.
  ///
  /// Parameters:
  /// * [grupoId] 
  /// * [cupoId] 
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaTraspaso] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaTraspaso] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaTraspaso>> traspasarCupo({ 
    required String grupoId,
    required String cupoId,
    required String idempotencyKey,
    required EntradaTraspaso entradaTraspaso,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/grupos/{grupoId}/cupos/{cupoId}/traspasos'.replaceAll('{' r'grupoId' '}', grupoId.toString()).replaceAll('{' r'cupoId' '}', cupoId.toString());
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
_bodyData=jsonEncode(entradaTraspaso);
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

    SalidaTraspaso? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaTraspaso, SalidaTraspaso>(rawData, 'SalidaTraspaso', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaTraspaso>(
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

  /// Emitir el voto, con el peso de los cupos
  /// El afectado por el acuerdo vota y su voto QUEDA REGISTRADO, como abstencion con peso cero. Impedirle votar lo dejaria sin constancia de que se presento; dejarlo ponderar convertiria el quorum en una formalidad en los dos casos donde mas importa.
  ///
  /// Parameters:
  /// * [acuerdoId] 
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaVoto] 
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaAcuerdo] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaAcuerdo>> votarAcuerdo({ 
    required String acuerdoId,
    required String idempotencyKey,
    required EntradaVoto entradaVoto,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/acuerdos/{acuerdoId}/votos'.replaceAll('{' r'acuerdoId' '}', acuerdoId.toString());
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
_bodyData=jsonEncode(entradaVoto);
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

    SalidaAcuerdo? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaAcuerdo, SalidaAcuerdo>(rawData, 'SalidaAcuerdo', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaAcuerdo>(
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
