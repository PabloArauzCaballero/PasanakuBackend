import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/puertos/almacen_seguro.dart';
import '../infraestructura/plataforma.dart';

const _claveAcceso = 'sesion.acceso';
const _claveRefresco = 'sesion.refresco';
const _claveHuella = 'dispositivo.huella';

/// Se sobreescribe en pruebas con un almacén en memoria.
final almacenSeguroProvider = Provider<AlmacenSeguro>(
  (_) => almacenSeguroDeLaPlataforma(),
);

/// La sesión: tokens en el almacén seguro, nunca en `SharedPreferences`.
class Sesion {
  Sesion(this._almacen);
  final AlmacenSeguro _almacen;

  Future<String?> tokenDeAcceso() => _almacen.leer(_claveAcceso);
  Future<String?> tokenDeRefresco() => _almacen.leer(_claveRefresco);

  Future<void> guardar({
    required String acceso,
    required String refresco,
  }) async {
    await _almacen.guardar(_claveAcceso, acceso);
    await _almacen.guardar(_claveRefresco, refresco);
  }

  /// La huella de **la instalación**, no de la persona: sobrevive al cierre de
  /// sesión a propósito, porque es lo que permite que el teléfono siga siendo «de
  /// confianza» para quien vuelve a entrar.
  Future<String?> leerHuella() => _almacen.leer(_claveHuella);
  Future<void> guardarHuella(String valor) =>
      _almacen.guardar(_claveHuella, valor);

  Future<void> cerrar() async {
    await _almacen.borrar(_claveAcceso);
    await _almacen.borrar(_claveRefresco);
  }
}

final sesionProvider = Provider<Sesion>(
  (ref) => Sesion(ref.watch(almacenSeguroProvider)),
);
