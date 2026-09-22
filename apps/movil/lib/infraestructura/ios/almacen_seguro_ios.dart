import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../dominio/puertos/almacen_seguro.dart';

/// Keychain, accesible solo con el dispositivo desbloqueado y sin respaldo en iCloud:
/// se elige el modelo más restrictivo (ADR-036, «lo que no espera al pase»).
class AlmacenSeguroIos implements AlmacenSeguro {
  AlmacenSeguroIos([FlutterSecureStorage? almacen])
    : _almacen =
          almacen ??
          const FlutterSecureStorage(
            iOptions: IOSOptions(
              accessibility: KeychainAccessibility.unlocked_this_device,
            ),
          );

  final FlutterSecureStorage _almacen;

  @override
  Future<String?> leer(String clave) => _almacen.read(key: clave);

  @override
  Future<void> guardar(String clave, String valor) =>
      _almacen.write(key: clave, value: valor);

  @override
  Future<void> borrar(String clave) => _almacen.delete(key: clave);
}
