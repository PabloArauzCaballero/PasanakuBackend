//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

import 'dart:async';

// ignore: unused_import
import 'dart:convert';
import 'package:aportaya_cliente_cumplimiento/src/deserialize.dart';
import 'package:dio/dio.dart';

import 'package:aportaya_cliente_cumplimiento/src/model/contrato_vigente.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/entrada_aceptacion.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/entrada_diligencia.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/entrada_incidente.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/entrada_pep.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/entrada_riesgo_operativo.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/error.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/salida_aceptacion.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/salida_alcance.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/salida_diligencia.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/salida_incidente.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/salida_pep.dart';
import 'package:aportaya_cliente_cumplimiento/src/model/salida_riesgo_operativo.dart';

class DefaultApi {

  final Dio _dio;

  const DefaultApi(this._dio);

  /// Aceptar el contrato de adhesion y el tarifario
  /// Vive aca y no en identidad —aunque la ficha del CU apunte alla— porque contrato_adhesion y aceptacion_contrato estan en el esquema de cumplimiento, y el esquema tiene precedencia sobre la ficha. Append-only: publicar una version nueva no toca las aceptaciones viejas.
  ///
  /// Parameters:
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [contratoId]
  /// * [entradaAceptacion]
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaAceptacion] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaAceptacion>> aceptarContrato({
    required String idempotencyKey,
    required String contratoId,
    required EntradaAceptacion entradaAceptacion,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/cumplimiento/contratos/{contratoId}/aceptaciones'.replaceAll('{' r'contratoId' '}', contratoId.toString());
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
_bodyData=jsonEncode(entradaAceptacion);
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

    SalidaAceptacion? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaAceptacion, SalidaAceptacion>(rawData, 'SalidaAceptacion', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaAceptacion>(
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

  /// Declaracion PEP y beneficiario final
  /// Mira tambien a los beneficiarios finales: un titular que no es PEP pero cuya estructura de control termina en un PEP es exactamente el caso que la norma quiere ver.
  ///
  /// Parameters:
  /// * [usuarioId]
  /// * [entradaPep]
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaPep] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaPep>> declararPep({
    required String usuarioId,
    required EntradaPep entradaPep,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/cumplimiento/usuarios/{usuarioId}/pep'.replaceAll('{' r'usuarioId' '}', usuarioId.toString());
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
_bodyData=jsonEncode(entradaPep);
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

    SalidaPep? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaPep, SalidaPep>(rawData, 'SalidaPep', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaPep>(
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

  /// Elevar el nivel de debida diligencia
  /// Subir de nivel desbloquea topes mas altos, asi que la puerta es igual de dura que los topes que abre. Nunca baja: un destino que no supera al nivel actual se rechaza en vez de aplicarse.
  ///
  /// Parameters:
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [usuarioId]
  /// * [entradaDiligencia]
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaDiligencia] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaDiligencia>> elevarDiligencia({
    required String idempotencyKey,
    required String usuarioId,
    required EntradaDiligencia entradaDiligencia,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/cumplimiento/usuarios/{usuarioId}/diligencia'.replaceAll('{' r'usuarioId' '}', usuarioId.toString());
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
_bodyData=jsonEncode(entradaDiligencia);
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

    SalidaDiligencia? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaDiligencia, SalidaDiligencia>(rawData, 'SalidaDiligencia', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaDiligencia>(
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

  /// Los contratos de adhesion que rigen hoy
  /// **Ruta publica, y tiene que serlo.** Se consulta durante el alta, cuando por definicion todavia no hay sesion: quien se registra tiene que poder leer que contrato esta aceptando ANTES de existir como usuario. Lo que devuelve es lo mismo que ya esta publicado en el sitio web —codigo, version, url del documento y su hash—; no hay dato de nadie aca. Sin esto la app no puede armar &#x60;aceptaContratos&#x60; de CU-01, que son UUID: los ids son &#x60;gen_random_uuid()&#x60; por entorno y no se pueden cablear.
  ///
  /// Parameters:
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [List<ContratoVigente>] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<List<ContratoVigente>>> listarContratosVigentes({
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/cumplimiento/contratos/vigentes';
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

    List<ContratoVigente>? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<List<ContratoVigente>, ContratoVigente>(rawData, 'List<ContratoVigente>', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<List<ContratoVigente>>(
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

  /// Deja constancia de la notificacion a los titulares afectados
  /// Se notifica con lo que se sabe, sin esperar al numero final: un aviso tardio y exacto le sirve menos a la persona afectada que uno a tiempo y aproximado. Sobre un incidente que no afecto datos personales se rechaza: asustar a gente sin motivo gasta la credibilidad del canal para cuando de verdad haga falta.
  ///
  /// Parameters:
  /// * [incidenteId]
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
  Future<Response<void>> notificarTitulares({
    required String incidenteId,
    required String idempotencyKey,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/cumplimiento/seguridad/incidentes/{incidenteId}/notificaciones'.replaceAll('{' r'incidenteId' '}', incidenteId.toString());
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

  /// Abre el expediente de un incidente de seguridad y arranca sus relojes
  /// Contener rapido, reportar en plazo y notificar a los titulares cuando hay datos personales afectados. Son TRES relojes distintos corriendo en paralelo, y los tres se guardan: recalcularlos al consultar los haria moverse solos, y bastaria cambiar la politica el mes que viene para que un incidente de hace tres meses apareciera reportado en plazo cuando no lo estuvo. Sin responsable de seguridad designado y activo NO se abre el incidente. No es burocracia: sin designacion no hay a quien le corran los plazos ni quien firme ante el organismo. El campo datosPersonalesAfectados es lo que DECLARA quien reporta, y lo puede sobrescribir el activo: si el inventario dice que ese activo contiene datos personales, los contiene. Quien esta apagando el incendio a las tres de la manana no esta en condiciones de determinar el alcance. El enlace con el contrato del proveedor se resuelve POR EL ACTIVO: la tabla incidente_seguridad no tiene columna propia para el, y agregarsela duplicaria un dato que ya vive en el inventario de activos. Vive en cumplimiento y no en auditoria, donde lo pone su ficha, porque las tablas que escribe estan en el esquema cumplimiento (invariante 11).
  ///
  /// Parameters:
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaIncidente]
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaIncidente] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaIncidente>> registrarIncidente({
    required String idempotencyKey,
    required EntradaIncidente entradaIncidente,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/cumplimiento/seguridad/incidentes';
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
_bodyData=jsonEncode(entradaIncidente);
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

    SalidaIncidente? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaIncidente, SalidaIncidente>(rawData, 'SalidaIncidente', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaIncidente>(
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

  /// Registra un evento de riesgo operativo y su plan de accion
  /// Ponerle numero a lo que cuesta hacer las cosas mal, con las categorias y factores que exige la norma, y cerrar el circulo con un plan de accion con responsable y plazo. Una base de perdidas sin plan de accion es una coleccion de anecdotas caras. Un evento SIN perdida monetaria se registra igual, con &#x60;perdidaBruta&#x60; en cero: la frecuencia tambien es informacion, y filtrar las casi-perdidas por no costar plata deja el analisis ciego justo para lo que todavia no salio caro. La &#x60;perdidaNeta&#x60; que devuelve es la que calculo el MOTOR: la columna es GENERATED. La aplicacion no la reemplaza, la lee de vuelta (&#x60;R-RIS-02&#x60;). La ruta va bajo &#x60;/cumplimiento&#x60; y no bajo &#x60;/riesgos&#x60; como dice la ficha del caso de uso: &#x60;/riesgos&#x60; no es un prefijo reservado de este servicio, y una ruta fuera de los prefijos es un rechazo automatico. Este caso de uso vive en &#x60;cumplimiento&#x60; y no en &#x60;auditoria&#x60; —donde lo pone su ficha— porque las cuatro tablas que escribe estan en el esquema &#x60;cumplimiento&#x60;, y &#x60;svc_auditoria&#x60; no tiene GRANT sobre el. Donde la ficha y el modelo no coinciden, manda el modelo (invariante 11).
  ///
  /// Parameters:
  /// * [idempotencyKey] - Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
  /// * [entradaRiesgoOperativo]
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaRiesgoOperativo] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaRiesgoOperativo>> registrarRiesgoOperativo({
    required String idempotencyKey,
    required EntradaRiesgoOperativo entradaRiesgoOperativo,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/cumplimiento/riesgos/eventos';
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
_bodyData=jsonEncode(entradaRiesgoOperativo);
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

    SalidaRiesgoOperativo? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaRiesgoOperativo, SalidaRiesgoOperativo>(rawData, 'SalidaRiesgoOperativo', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaRiesgoOperativo>(
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

  /// Deja constancia de que el incidente se reporto al organismo
  /// Se reporta IGUAL aunque el plazo haya vencido: llegar tarde es una observacion, no reportar es un incumplimiento. Lo que no se hace es disimularlo: el vencimiento queda visible y el control diario abre el hallazgo.
  ///
  /// Parameters:
  /// * [incidenteId]
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
  Future<Response<void>> reportarIncidente({
    required String incidenteId,
    required String idempotencyKey,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/cumplimiento/seguridad/incidentes/{incidenteId}/reportes'.replaceAll('{' r'incidenteId' '}', incidenteId.toString());
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

  /// Verificar el alcance de la licencia
  /// Lo consultan los otros servicios antes de habilitar una operacion. Deniega por omision: sin licencia legible, sin alcance o sin sandbox, la respuesta es no. Un fallo de lectura no puede traducirse en «segui». No lleva Idempotency-Key porque no tiene efecto: solo responde.
  ///
  /// Parameters:
  /// * [servicio]
  /// * [usuarioId]
  /// * [cancelToken] - A [CancelToken] that can be used to cancel the operation
  /// * [headers] - Can be used to add additional headers to the request
  /// * [extras] - Can be used to add flags to the request
  /// * [validateStatus] - A [ValidateStatus] callback that can be used to determine request success based on the HTTP status of the response
  /// * [onSendProgress] - A [ProgressCallback] that can be used to get the send progress
  /// * [onReceiveProgress] - A [ProgressCallback] that can be used to get the receive progress
  ///
  /// Returns a [Future] containing a [Response] with a [SalidaAlcance] as data
  /// Throws [DioException] if API call or serialization fails
  Future<Response<SalidaAlcance>> verificarAlcance({
    required String servicio,
    String? usuarioId,
    CancelToken? cancelToken,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ValidateStatus? validateStatus,
    ProgressCallback? onSendProgress,
    ProgressCallback? onReceiveProgress,
  }) async {
    final _path = r'/licencia/alcance';
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
      r'servicio': servicio,
      if (usuarioId != null) r'usuarioId': usuarioId,
    };

    final _response = await _dio.request<Object>(
      _path,
      options: _options,
      queryParameters: _queryParameters,
      cancelToken: cancelToken,
      onSendProgress: onSendProgress,
      onReceiveProgress: onReceiveProgress,
    );

    SalidaAlcance? _responseData;

    try {
final rawData = _response.data;
_responseData = rawData == null ? null : deserialize<SalidaAlcance, SalidaAlcance>(rawData, 'SalidaAlcance', growable: true);
    } catch (error, stackTrace) {
      throw DioException(
        requestOptions: _response.requestOptions,
        response: _response,
        type: DioExceptionType.unknown,
        error: error,
        stackTrace: stackTrace,
      );
    }

    return Response<SalidaAlcance>(
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
