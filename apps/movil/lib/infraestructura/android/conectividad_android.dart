import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:dio/dio.dart';

import '../../dominio/puertos/conectividad.dart';

/// Estar «conectado a wifi» no es tener internet (regla del shell): además del
/// reporte del sistema operativo, **sondea el gateway** con una petición liviana.
class ConectividadAndroid implements Conectividad {
  ConectividadAndroid({Dio? sonda}) : _sonda = sonda ?? _sondaPorOmision();

  final Dio _sonda;
  final Connectivity _sistema = Connectivity();

  /// Sonda liviana al gateway, fuera de `dominio/cliente.dart` a propósito: sin
  /// bearer ni interceptores, solo pregunta si hay señal — no pasa por vista.
  static Dio _sondaPorOmision() {
    final opciones = BaseOptions(connectTimeout: const Duration(seconds: 3));
    return Dio(opciones); // permitido: puerto Conectividad, no es dominio/
  }

  @override
  Stream<bool> get cambios => _sistema.onConnectivityChanged.asyncMap(
    (resultados) async =>
        !resultados.contains(ConnectivityResult.none) && await hayConexion(),
  );

  @override
  Future<bool> hayConexion() async {
    final estado = await _sistema.checkConnectivity();
    if (estado.contains(ConnectivityResult.none)) return false;
    try {
      final r = await _sonda.get<void>('http://localhost:4010/api/v1/version');
      return r.statusCode != null && r.statusCode! < 500;
    } catch (_) {
      // Sin respuesta del gateway: se toma como sin conexión útil, aunque el
      // sistema operativo reporte wifi.
      return false;
    }
  }
}
