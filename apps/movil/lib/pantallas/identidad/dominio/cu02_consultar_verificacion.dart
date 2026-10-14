import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';

/// CU-02 · `GET /usuarios/{usuarioId}/verificacion` — sin sesión obligatoria, igual
/// que subir el documento: quien recién se registró todavía no puede abrir sesión.
///
/// Pero el `usuarioId` de la URL no autoriza nada: sin sesión propia lo que acredita
/// que es SU expediente es el `Token-De-Alta` que devolvió ESE alta (el mismo que ya
/// usa la subida de fotos). Sin él —o con el de otra persona— el servidor responde
/// 403, exista o no el usuario.
class ConsultarVerificacion {
  ConsultarVerificacion(this._ref);
  final Ref _ref;

  Future<EstadoDeVerificacion> ejecutar(
    String usuarioId, {
    String? tokenDeAlta,
  }) async {
    final r = await DefaultApi(_ref.read(dioProvider))
        .consultarEstadoDeVerificacion(
          usuarioId: usuarioId,
          tokenDeAlta: tokenDeAlta,
        );
    return r.data!;
  }
}

final consultarVerificacionProvider = Provider<ConsultarVerificacion>(
  ConsultarVerificacion.new,
);
