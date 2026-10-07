import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';
import 'estado_sesion.dart' show mensajeDeError;

/// Cliente del codigo de correo previo al alta. El codigo nunca se valida en la app.
class VerificacionCorreo {
  VerificacionCorreo(this._ref);
  final Ref _ref;

  static const _formulario = 'cu01-verificacion-correo';

  Future<VerificacionSolicitada> solicitar(
    String correo, {
    bool nueva = false,
  }) async {
    final idempotencia = _ref.read(idempotenciaProvider.notifier);
    if (nueva) idempotencia.cerrar(_formulario);
    try {
      final respuesta = await DefaultApi(
        _ref.read(dioProvider),
      ).solicitarVerificacionCorreo(
        idempotencyKey: idempotencia.claveDe(_formulario),
        solicitudVerificacionCorreo: SolicitudVerificacionCorreo(correo: correo),
      );
      final cuerpo = respuesta.data;
      if (cuerpo == null) {
        throw StateError('El servidor no devolvio la verificacion.');
      }
      return VerificacionSolicitada(
        id: cuerpo.verificacionId,
        destino: cuerpo.destinoEnmascarado,
      );
    } on DioException catch (e) {
      if (e.response != null) idempotencia.cerrar(_formulario);
      throw VerificacionCorreoException(mensajeDeError(errorDeDominio(e)));
    }
  }

  void reiniciar() =>
      _ref.read(idempotenciaProvider.notifier).cerrar(_formulario);

  Future<void> confirmar({
    required String id,
    required String correo,
    required String codigo,
  }) async {
    try {
      await DefaultApi(_ref.read(dioProvider)).confirmarVerificacionCorreo(
        verificacionId: id,
        confirmacionVerificacionCorreo: ConfirmacionVerificacionCorreo(
          correo: correo,
          codigo: codigo,
        ),
      );
    } on DioException catch (e) {
      throw VerificacionCorreoException(mensajeDeError(errorDeDominio(e)));
    }
  }
}

class VerificacionSolicitada {
  const VerificacionSolicitada({required this.id, required this.destino});
  final String id;
  final String destino;
}

class VerificacionCorreoException implements Exception {
  const VerificacionCorreoException(this.mensaje);
  final String mensaje;
}

final verificacionCorreoProvider = Provider<VerificacionCorreo>(
  VerificacionCorreo.new,
);
