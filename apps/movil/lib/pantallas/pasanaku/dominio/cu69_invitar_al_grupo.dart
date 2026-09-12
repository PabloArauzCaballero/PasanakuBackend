import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';

/// CU-69 · invitar a un contacto. El mensaje que se envía lo redacta el backend con
/// una plantilla aprobada (CU-80): esta pantalla nunca compone el texto de la
/// invitación ni revela datos de otros integrantes (regla del CU, flujo 3).
class InvitarAlGrupo extends AsyncNotifier<SalidaInvitacion?> {
  static String formularioId(String grupoId) => 'cu69-invitar-$grupoId';

  @override
  Future<SalidaInvitacion?> build() async => null;

  Future<void> enviar({
    required String grupoId,
    required String telefonoInvitado,
    required EntradaInvitacionCanalEnum canal,
    String? nombreSugerido,
  }) async {
    if (state.isLoading) return;
    state = const AsyncLoading<SalidaInvitacion?>();
    final dio = ref.read(dioProvider);
    final clave = ref
        .read(idempotenciaProvider.notifier)
        .claveDe(formularioId(grupoId));
    try {
      final r = await DefaultApi(dio).invitarAlGrupo(
        grupoId: grupoId,
        idempotencyKey: clave,
        entradaInvitacion: EntradaInvitacion(
          telefonoInvitado: telefonoInvitado,
          canal: canal,
          nombreSugerido: nombreSugerido,
        ),
      );
      ref.read(idempotenciaProvider.notifier).cerrar(formularioId(grupoId));
      state = AsyncData(r.data);
    } catch (e) {
      state = AsyncError(errorDeDominio(e), StackTrace.current);
    }
  }
}

final invitarAlGrupoProvider =
    AsyncNotifierProvider<InvitarAlGrupo, SalidaInvitacion?>(
      InvitarAlGrupo.new,
    );
