import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';

/// CU-68 · postular a un grupo ("Pedir mi cupo", D-15). El botón nunca dice
/// "Unirme": pedir el cupo no lo ocupa. La API no expone (todavía) un estado de la
/// postulación —crea y devuelve `puntaje`+`motivos`, sin `estado`— así que la
/// pantalla de confirmación (D-15 "Tu pedido de cupo") no puede mostrar si ya te
/// aceptaron sin volver a consultar algo que **no existe**: no hay
/// `GET /grupos/{id}/postulaciones/{id}` ni `GET /grupos?participante=`. Huecos
/// declarados en el informe del carril.
///
/// Un provider por grupo (family): el mismo participante puede estar mirando el
/// pedido de cupo de más de un grupo a la vez.
class PostularAlGrupo extends AsyncNotifier<SalidaPostulacion?> {
  PostularAlGrupo(this.grupoId);
  final String grupoId;

  String get _formularioId => 'cu68-postular-$grupoId';

  @override
  Future<SalidaPostulacion?> build() async => null;

  Future<void> enviar({
    required String grupoId,
    required int cuposSolicitados,
    String? mensaje,
  }) async {
    if (state.isLoading) return;
    state = const AsyncLoading<SalidaPostulacion?>();
    final dio = ref.read(dioProvider);
    final clave = ref
        .read(idempotenciaProvider.notifier)
        .claveDe(_formularioId);
    try {
      final r = await DefaultApi(dio).postularAlGrupo(
        grupoId: grupoId,
        idempotencyKey: clave,
        entradaPostulacion: EntradaPostulacion(
          cuposSolicitados: cuposSolicitados,
          mensaje: mensaje,
        ),
      );
      ref.read(idempotenciaProvider.notifier).cerrar(_formularioId);
      state = AsyncData(r.data);
    } catch (e) {
      state = AsyncError(errorDeDominio(e), StackTrace.current);
    }
  }
}

final postularAlGrupoProvider =
    AsyncNotifierProvider.family<PostularAlGrupo, SalidaPostulacion?, String>(
      PostularAlGrupo.new,
    );
