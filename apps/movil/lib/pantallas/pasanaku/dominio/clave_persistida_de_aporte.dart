import 'dart:convert';

import 'package:aportaya_diseno/errores.dart';
import 'package:crypto/crypto.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/puertos/almacen_seguro.dart';
import '../../../proveedores/idempotencia.dart';
import '../../../proveedores/sesion.dart';

/// Conserva la clave del CU-21 entre cierres de proceso. Sin una copia duradera,
/// un timeout tras cobrar y antes de recibir la respuesta podría generar otra
/// clave al reabrir la app y crear un segundo intento de pago.
class ClavePersistidaDeAporte {
  ClavePersistidaDeAporte(this._almacen, this._idempotencia);

  static const _noVerificable =
      'No podemos comprobar si este aporte ya se envió. '
      'Consultá el estado con soporte antes de intentarlo de nuevo.';

  final AlmacenSeguro _almacen;
  final Idempotencia _idempotencia;

  String _claveLocal(String obligacionId) => 'idempotencia.cu21.$obligacionId';
  String _formularioId(String obligacionId) => 'cu21-cobrar-$obligacionId';

  Future<String> abrir({
    required String obligacionId,
    required String monto,
    required String canal,
    required String referenciaProveedor,
  }) async {
    final huella = sha256
        .convert(utf8.encode(jsonEncode([monto, canal, referenciaProveedor])))
        .toString();
    final anterior = await _almacen.leer(_claveLocal(obligacionId));
    if (anterior != null) {
      try {
        final registro = jsonDecode(anterior) as Map<String, dynamic>;
        final clave = registro['clave'];
        final huellaAnterior = registro['huella'];
        if (clave is! String || clave.isEmpty || huellaAnterior is! String) {
          throw const ErrorDeAportePendiente(_noVerificable);
        }
        if (huellaAnterior != huella) {
          throw const ErrorDeAportePendiente(
            'Hay un intento anterior de aporte con otros datos. Usá la misma '
            'referencia y el mismo canal; si no los recordás, consultá a '
            'soporte antes de volver a pagar.',
          );
        }
        return clave;
      } on FormatException {
        throw const ErrorDeAportePendiente(_noVerificable);
      } on TypeError {
        throw const ErrorDeAportePendiente(_noVerificable);
      }
    }

    final nueva = _idempotencia.claveDe(_formularioId(obligacionId));
    // El guardado termina ANTES del POST. Si falla, no se inicia un cobro cuya
    // clave no sobreviviría a un cierre o a una respuesta perdida.
    await _almacen.guardar(
      _claveLocal(obligacionId),
      jsonEncode({'clave': nueva, 'huella': huella}),
    );
    return nueva;
  }

  Future<void> confirmar(String obligacionId) async {
    await _almacen.borrar(_claveLocal(obligacionId));
    _idempotencia.cerrar(_formularioId(obligacionId));
  }
}

class ErrorDeAportePendiente implements Exception, ErrorPresentable {
  const ErrorDeAportePendiente(this.mensaje);

  @override
  final String mensaje;

  @override
  String? get trazaId => null;

  @override
  bool get sinConexion => false;
}

final clavePersistidaDeAporteProvider = Provider<ClavePersistidaDeAporte>(
  (ref) => ClavePersistidaDeAporte(
    ref.watch(almacenSeguroProvider),
    ref.read(idempotenciaProvider.notifier),
  ),
);
