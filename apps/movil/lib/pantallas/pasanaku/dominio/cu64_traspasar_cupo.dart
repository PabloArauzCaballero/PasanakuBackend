import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../dominio/errores.dart';
import '../../../proveedores/idempotencia.dart';
import '../../../proveedores/sesion.dart';

/// Lo que se manda en un traspaso (CU-64). El turno y la deuda vencida no viajan: no
/// hay campo para ellos porque el servidor no los mueve.
typedef PedidoDeTraspaso = ({
  String grupoId,
  String cupoId,
  String usuarioEntranteId,
  String motivo,
  String? acuerdoId,
});

/// CU-64 · traspasar el cupo a otro integrante del grupo.
///
/// Una clave de idempotencia por **intento**: si la red falla, el reintento con los
/// mismos datos reusa la clave (el servidor reconoce la operación y no traspasa dos
/// veces). Si el servidor contestó un rechazo de regla (422), ese intento terminó: el
/// siguiente —por ejemplo, después de saldar un aporte— es otra operación y lleva otra
/// clave. Cambiar a quién o el motivo también es otro intento: el formulario se nombra
/// con el contenido, así una clave nunca viaja con dos cuerpos distintos.
class TraspasarCupo extends AsyncNotifier<SalidaTraspaso?> {
  static String formularioDe(PedidoDeTraspaso p) =>
      'cu64-traspasar-${p.cupoId}-${p.usuarioEntranteId}-'
      '${Object.hash(p.motivo, p.acuerdoId)}';

  @override
  Future<SalidaTraspaso?> build() async {
    ref.watch(generacionDeSesionProvider);
    return null;
  }

  Future<void> enviar(PedidoDeTraspaso pedido) async {
    if (state.isLoading) return;
    final claves = ref.read(idempotenciaProvider.notifier);
    final formulario = formularioDe(pedido);
    state = const AsyncLoading<SalidaTraspaso?>();
    try {
      final r = await DefaultApi(ref.read(dioProvider)).traspasarCupo(
        grupoId: pedido.grupoId,
        cupoId: pedido.cupoId,
        idempotencyKey: claves.claveDe(formulario),
        entradaTraspaso: EntradaTraspaso(
          usuarioEntranteId: pedido.usuarioEntranteId,
          motivo: pedido.motivo,
          acuerdoId: pedido.acuerdoId,
        ),
      );
      claves.cerrar(formulario);
      state = AsyncData(r.data);
    } catch (e) {
      final error = errorDeDominio(e);
      if (error is ErrorDeApi && error.estado == 422) claves.cerrar(formulario);
      state = AsyncError(error, StackTrace.current);
    }
  }
}

final traspasarCupoProvider =
    AsyncNotifierProvider.autoDispose<TraspasarCupo, SalidaTraspaso?>(
      TraspasarCupo.new,
    );
