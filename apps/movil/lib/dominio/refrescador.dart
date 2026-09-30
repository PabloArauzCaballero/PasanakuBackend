import 'package:dio/dio.dart';

import '../proveedores/sesion.dart';

/// Un solo refresco en vuelo por vez: N peticiones que reciben `401` a la vez
/// comparten el mismo `Future` en vez de disparar un `POST /sesion/refrescar` cada
/// una. Sale por un `Dio` propio, sin los interceptores de la app: si pasara por
/// `_TrazaYSesion`, un `401` del propio refresco reentraría a este mismo mecanismo.
class Refrescador {
  Refrescador(this._sesion, this._dioSinInterceptores);
  final Sesion _sesion;
  final Dio _dioSinInterceptores;
  Future<bool>? _enVuelo;

  Future<bool> refrescar() => _enVuelo ??= _hacerRefresco().whenComplete(() {
    _enVuelo = null;
  });

  Future<bool> _hacerRefresco() async {
    final refresco = await _sesion.tokenDeRefresco();
    if (refresco == null) return false;
    try {
      final r = await _dioSinInterceptores.post<Map<String, dynamic>>(
        '/sesion/refrescar',
        data: {'refresco': refresco},
      );
      final acceso = r.data?['acceso'] as String?;
      final nuevo = r.data?['refresco'] as String?;
      if (acceso == null || nuevo == null) return false;
      await _sesion.guardar(acceso: acceso, refresco: nuevo);
      return true;
    } on DioException {
      return false;
    }
  }
}
