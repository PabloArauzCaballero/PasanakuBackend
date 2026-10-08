import 'dart:convert';

import 'package:crypto/crypto.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../proveedores/idempotencia.dart';
import '../proveedores/sesion.dart';
import 'error_de_operacion_de_dinero.dart';
import 'puertos/almacen_seguro.dart';

/// Conserva la clave de una operación monetaria antes del POST, incluso si la
/// app se cierra mientras espera la respuesta. Nunca persiste el factor MFA.
class ClavePersistidaDeBilletera {
  ClavePersistidaDeBilletera(this._almacen, this._idempotencia);

  static const _noVerificable =
      'No podemos comprobar si esta operación ya se envió. '
      'Consultá su estado con soporte antes de intentarlo de nuevo.';

  final AlmacenSeguro _almacen;
  final Idempotencia _idempotencia;

  String _claveLocal(String formularioId) => 'idempotencia.$formularioId';

  Future<String> abrir({
    required String formularioId,
    required List<String> datos,
  }) async {
    final huella = sha256.convert(utf8.encode(jsonEncode(datos))).toString();
    final anterior = await _almacen.leer(_claveLocal(formularioId));
    if (anterior != null) {
      try {
        final registro = jsonDecode(anterior) as Map<String, dynamic>;
        final clave = registro['clave'];
        final huellaAnterior = registro['huella'];
        if (registro['version'] != 1 ||
            clave is! String ||
            clave.isEmpty ||
            huellaAnterior is! String) {
          throw const ErrorDeOperacionPendiente(_noVerificable);
        }
        if (huellaAnterior != huella) {
          throw const ErrorDeOperacionPendiente(
            'Hay una operación anterior con otros datos. Consultá su estado '
            'con soporte antes de iniciar otra operación.',
          );
        }
        return clave;
      } on FormatException {
        throw const ErrorDeOperacionPendiente(_noVerificable);
      } on TypeError {
        throw const ErrorDeOperacionPendiente(_noVerificable);
      }
    }

    final nueva = _idempotencia.claveDe(formularioId);
    // Si el almacén falla, no se envía una operación cuya clave se perdería.
    await _almacen.guardar(
      _claveLocal(formularioId),
      jsonEncode({'version': 1, 'clave': nueva, 'huella': huella}),
    );
    return nueva;
  }

  Future<void> confirmar(String formularioId) async {
    await _almacen.borrar(_claveLocal(formularioId));
    _idempotencia.cerrar(formularioId);
  }
}

final clavePersistidaDeBilleteraProvider = Provider<ClavePersistidaDeBilletera>(
  (ref) => ClavePersistidaDeBilletera(
    ref.watch(almacenSeguroProvider),
    ref.read(idempotenciaProvider.notifier),
  ),
);
