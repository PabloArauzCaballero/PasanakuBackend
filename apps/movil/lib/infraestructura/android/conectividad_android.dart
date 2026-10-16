import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:dio/dio.dart';

import '../../dominio/cliente.dart' show baseDelGateway;
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
      // El mismo gateway contra el que habla la app (--dart-define=API). Con la URL
      // del Prism de desarrollo escrita aca, en un telefono o en la web `localhost`
      // es la propia maquina del usuario y la app se creia siempre sin conexion.
      final r = await _sonda.get<void>('$baseDelGateway/version');
      return r.statusCode != null && r.statusCode! < 500;
    } catch (_) {
      // Sin respuesta del gateway: se toma como sin conexión útil, aunque el
      // sistema operativo reporte wifi.
      return false;
    }
  }
}
