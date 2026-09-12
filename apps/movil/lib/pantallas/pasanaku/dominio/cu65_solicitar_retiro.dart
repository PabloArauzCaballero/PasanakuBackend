import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';

/// CU-65 · solicitar el retiro de un grupo. La posición (`ACREEDORA`, `DEUDORA`,
/// `NEUTRA`) la calcula `grupos`; esta pantalla la muestra tal como llega, nunca la
/// deduce del lado del cliente.
class SolicitarRetiro extends AsyncNotifier<SalidaRetiro?> {
  static String formularioId(String grupoId) => 'cu65-retiro-$grupoId';

  @override
  Future<SalidaRetiro?> build() async => null;

  Future<void> enviar({
    required String grupoId,
    required String participanteId,
    required String motivo,
    bool aceptaPlanDePago = false,
  }) async {
    if (state.isLoading) return;
    state = const AsyncLoading<SalidaRetiro?>();
    final dio = ref.read(dioProvider);
    final clave = ref
        .read(idempotenciaProvider.notifier)
        .claveDe(formularioId(grupoId));
    try {
      final r = await DefaultApi(dio).solicitarRetiro(
        grupoId: grupoId,
        idempotencyKey: clave,
        entradaRetiro: EntradaRetiro(
          participanteId: participanteId,
          motivo: motivo,
          aceptaPlanDePago: aceptaPlanDePago,
        ),
      );
      ref.read(idempotenciaProvider.notifier).cerrar(formularioId(grupoId));
      state = AsyncData(r.data);
    } catch (e) {
      state = AsyncError(errorDeDominio(e), StackTrace.current);
    }
  }
}

final solicitarRetiroProvider =
    AsyncNotifierProvider<SolicitarRetiro, SalidaRetiro?>(SolicitarRetiro.new);
