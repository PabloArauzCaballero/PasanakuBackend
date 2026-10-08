import 'package:aportaya_diseno/errores.dart';
import 'package:dio/dio.dart';

import 'cliente.dart';
import 'errores.dart';

/// Un timeout o un 5xx no demuestra que el servidor no haya aceptado dinero.
/// El reintento visible conserva la clave persistida de la misma operación.
Object errorDeOperacionDeDinero(Object error) {
  // Un timeout no prueba que falte conexión: el servidor podría haber recibido
  // el POST y estar tardando en responder. Ambos resultados son inciertos, pero
  // solo un fallo de conexión muestra la variante offline de la UI.
  if (error is DioException && error.response == null) {
    return ErrorDeResultadoIncierto(
      sinConexion: error.type == DioExceptionType.connectionError,
    );
  }
  final causa = errorDeDominio(error);
  if (causa is ErrorDeRed) {
    return const ErrorDeResultadoIncierto(sinConexion: true);
  }
  if (causa is ErrorDeApi && causa.estado >= 500) {
    return ErrorDeResultadoIncierto(trazaId: causa.trazaId);
  }
  if (causa is ErrorDeApi && causa.estado == 409) {
    return const ErrorDeOperacionPendiente(
      'Esta operación ya existe o cambió. Consultá su estado antes de volver '
      'a operar.',
    );
  }
  return causa;
}

class ErrorDeResultadoIncierto implements Exception, ErrorPresentable {
  const ErrorDeResultadoIncierto({this.trazaId, this.sinConexion = false});

  @override
  final String? trazaId;

  @override
  final bool sinConexion;

  @override
  String get mensaje =>
      'No pudimos confirmar el resultado de esta operación. '
      'Volvé a intentar desde esta pantalla con los mismos datos. '
      'Si saliste, consultá su estado antes de iniciar otra operación.';
}

class ErrorDeOperacionPendiente implements Exception, ErrorPresentable {
  const ErrorDeOperacionPendiente(this.mensaje);

  @override
  final String mensaje;

  @override
  String? get trazaId => null;

  @override
  bool get sinConexion => false;
}

/// La sonda aún no autorizó iniciar un POST monetario. No implica que haya
/// existido un intento remoto ni debe presentarse como resultado incierto.
class ErrorDeConexionNoConfirmada implements Exception, ErrorPresentable {
  const ErrorDeConexionNoConfirmada._(this.mensaje, this.sinConexion);

  const ErrorDeConexionNoConfirmada.comprobando()
    : this._(
        'Estamos comprobando la conexión. No enviamos la operación.',
        false,
      );

  const ErrorDeConexionNoConfirmada.fallida()
    : this._(
        'No pudimos comprobar la conexión. No enviamos la operación; '
        'volvé a comprobarla antes de intentar de nuevo.',
        false,
      );

  const ErrorDeConexionNoConfirmada.sinRed()
    : this._(
        'No hay conexión. No enviamos la operación; '
        'volvé a intentar cuando se recupere.',
        true,
      );

  @override
  final String mensaje;

  @override
  final bool sinConexion;

  @override
  String? get trazaId => null;
}
