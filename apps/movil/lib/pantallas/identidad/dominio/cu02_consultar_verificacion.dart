import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';

/// CU-02 · `GET /usuarios/{usuarioId}/verificacion` — pública, igual que subir el
/// documento: quien recién se registró todavía no puede abrir sesión.
class ConsultarVerificacion {
  ConsultarVerificacion(this._ref);
  final Ref _ref;

  Future<EstadoDeVerificacion> ejecutar(String usuarioId) async {
    final r = await DefaultApi(
      _ref.read(dioProvider),
    ).consultarEstadoDeVerificacion(usuarioId: usuarioId);
    return r.data!;
  }
}

final consultarVerificacionProvider = Provider<ConsultarVerificacion>(
  ConsultarVerificacion.new,
);
