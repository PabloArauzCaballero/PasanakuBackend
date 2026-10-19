import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';

/// Envía la fecha civil en el formato OpenAPI `date` (yyyy-MM-dd), sin hora ni zona.
///
/// El modelo Dart generado serializa `DateTime` con hora (`toIso8601String()`), pero
/// el contrato y el servidor esperan una fecha civil. Esta operación mantiene el
/// cliente Dio compartido y evita que una corrección legítima del KYC sea rechazada
/// por convertirla en fecha-hora.
class ActualizarFechaExpiracionDocumento {
  ActualizarFechaExpiracionDocumento(this._ref);

  final Ref _ref;

  Future<void> ejecutar({
    required String usuarioId,
    required DateTime fecha,
  }) async {
    final fechaCivil =
        '${fecha.year.toString().padLeft(4, '0')}-'
        '${fecha.month.toString().padLeft(2, '0')}-'
        '${fecha.day.toString().padLeft(2, '0')}';
    await _ref
        .read(dioProvider)
        .put<void>(
          '/usuarios/$usuarioId/documento/fecha-expiracion',
          data: {'fechaExpiracionDocumento': fechaCivil},
        );
  }
}

final actualizarFechaExpiracionDocumentoProvider =
    Provider<ActualizarFechaExpiracionDocumento>(
      ActualizarFechaExpiracionDocumento.new,
    );
