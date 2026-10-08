import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:dio/dio.dart';

import '../../dominio/cliente.dart' show baseDelGateway;
import '../../dominio/puertos/conectividad.dart';

/// El sistema no confirmó ausencia de red, pero tampoco se pudo verificar el
/// gateway. No se debe presentar como "teléfono sin conexión" ni autorizar dinero.
class SondaNoComprobada implements Exception {
  const SondaNoComprobada();
}

/// Estar «conectado a wifi» no es tener internet (regla del shell): además del
/// reporte del sistema operativo, **sondea el gateway** con una petición liviana.
class ConectividadAndroid implements Conectividad {
  ConectividadAndroid({
    Dio? sonda,
    String? baseDeSonda,
    Future<List<ConnectivityResult>> Function()? comprobarRed,
    Stream<List<ConnectivityResult>>? cambiosDeRed,
    this.intervaloDeSonda = const Duration(seconds: 15),
    this.limiteSonda = const Duration(seconds: 3),
  }) : _sonda = sonda ?? _sondaPorOmision(),
       _baseDeSonda = baseDeSonda ?? baseDelGateway,
       _comprobarRed = comprobarRed ?? Connectivity().checkConnectivity,
       _cambiosDeRed = cambiosDeRed ?? Connectivity().onConnectivityChanged;

  final Dio _sonda;
  final String? _baseDeSonda;
  final Future<List<ConnectivityResult>> Function() _comprobarRed;
  final Stream<List<ConnectivityResult>> _cambiosDeRed;
  final Duration intervaloDeSonda;
  final Duration limiteSonda;

  /// Sonda liviana al gateway, fuera de `dominio/cliente.dart` a propósito: sin
  /// bearer ni interceptores, solo pregunta si hay señal — no pasa por vista.
  static Dio _sondaPorOmision() {
    final opciones = BaseOptions(connectTimeout: const Duration(seconds: 3));
    return Dio(opciones); // permitido: puerto Conectividad, no es dominio/
  }

  @override
  Stream<bool> get cambios {
    late final StreamController<bool> controlador;
    StreamSubscription<List<ConnectivityResult>>? suscripcion;
    Timer? reloj;
    var sondeando = false;

    Future<void> sondear() async {
      if (sondeando) return;
      sondeando = true;
      try {
        final conectado = await hayConexion();
        if (controlador.hasListener && !controlador.isClosed) {
          controlador.add(conectado);
        }
      } on Object catch (error, traza) {
        if (controlador.hasListener && !controlador.isClosed) {
          controlador.addError(error, traza);
        }
      } finally {
        sondeando = false;
      }
    }

    controlador = StreamController<bool>(
      onListen: () {
        suscripcion = _cambiosDeRed.listen((_) => sondear());
        // La red del SO puede seguir igual mientras el gateway se recupera.
        reloj = Timer.periodic(intervaloDeSonda, (_) => sondear());
      },
      onCancel: () async {
        reloj?.cancel();
        await suscripcion?.cancel();
      },
    );
    return controlador.stream.distinct();
  }

  @override
  Future<bool> hayConexion() async {
    final cancelacion = CancelToken();
    try {
      return await _comprobarConGateway(cancelacion).timeout(
        limiteSonda,
        onTimeout: () {
          cancelacion.cancel('La sonda excedió su tiempo límite.');
          throw const SondaNoComprobada();
        },
      );
    } catch (_) {
      // Solo ConnectivityResult.none demuestra que el teléfono está offline.
      // Un gateway lento o caído mantiene el comando pausado, pero es un error
      // de comprobación recuperable, no un diagnóstico de red del dispositivo.
      cancelacion.cancel();
      throw const SondaNoComprobada();
    }
  }

  Future<bool> _comprobarConGateway(CancelToken cancelacion) async {
    final estado = await _comprobarRed();
    if (cancelacion.isCancelled || estado.contains(ConnectivityResult.none)) {
      return false;
    }
    final base = _baseDeSonda;
    if (base == null) throw const SondaNoComprobada();
    // El mismo gateway contra el que habla la app (--dart-define=API). Con la URL
    // del Prism de desarrollo escrita aca, en un telefono o en la web `localhost`
    // es la propia maquina del usuario y la app se creia siempre sin conexion.
    // Cualquier HTTP demuestra que el gateway respondió. Un 500 es un fallo
    // del servidor, no «el teléfono está offline»; el GET real lo distinguirá.
    final respuesta = await _sonda.get<void>(
      '$base/version',
      options: Options(validateStatus: (_) => true),
      cancelToken: cancelacion,
    );
    if (respuesta.statusCode == null) throw const SondaNoComprobada();
    return true;
  }
}
