import 'package:dio/dio.dart';

import '../proveedores/sesion.dart';

/// Un solo refresco en vuelo por vez: N peticiones que reciben `401` a la vez
/// comparten el mismo `Future` en vez de disparar un `POST /sesion/refrescar` cada
/// una. Sale por un `Dio` propio, sin los interceptores de la app: si pasara por
/// `_TrazaYSesion`, un `401` del propio refresco reentraría a este mismo mecanismo.
///
/// El refresco en vuelo se comparte **por sesión**, no por instancia (DEFECTO-003):
/// `cliente.dart`, `renovar_acceso.dart` y `habilitacion_basica.dart` crean cada uno su
/// `Refrescador`, y dos refrescos simultáneos gastarían el mismo token de refresco dos
/// veces, lo que el servidor castiga revocando la sesión por reuso.
class Refrescador {
  Refrescador(this._sesion, this._dioSinInterceptores);
  final Sesion _sesion;
  final Dio _dioSinInterceptores;
  static final Expando<Future<bool>> _enVueloPorSesion = Expando();

  Future<bool> refrescar() {
    final enVuelo = _enVueloPorSesion[_sesion];
    if (enVuelo != null) return enVuelo;
    final nuevo = _hacerRefresco().whenComplete(() {
      _enVueloPorSesion[_sesion] = null;
    });
    _enVueloPorSesion[_sesion] = nuevo;
    return nuevo;
  }

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
      await _sesion.renovar(acceso: acceso, refresco: nuevo);
      return true;
    } on DioException {
      return false;
    }
  }
}
