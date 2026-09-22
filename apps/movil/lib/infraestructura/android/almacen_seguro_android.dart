import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../dominio/puertos/almacen_seguro.dart';

/// Keystore con cifrado por hardware cuando existe. Se escribe primero (ADR-036).
class AlmacenSeguroAndroid implements AlmacenSeguro {
  AlmacenSeguroAndroid([FlutterSecureStorage? almacen])
    : _almacen =
          almacen ?? const FlutterSecureStorage(aOptions: AndroidOptions());

  final FlutterSecureStorage _almacen;

  @override
  Future<String?> leer(String clave) => _almacen.read(key: clave);

  @override
  Future<void> guardar(String clave, String valor) =>
      _almacen.write(key: clave, value: valor);

  @override
  Future<void> borrar(String clave) => _almacen.delete(key: clave);
}
