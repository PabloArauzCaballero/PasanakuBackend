import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';

/// CU-62 · solicitar la permuta de dos turnos. Nace `PENDIENTE`: sin aceptación de
/// la contraparte no hay nada — esta pantalla nunca la da por hecha.
class SolicitarPermuta extends AsyncNotifier<SalidaPermuta?> {
  static const formularioId = 'cu62-solicitar-permuta';

  @override
  Future<SalidaPermuta?> build() async => null;

  Future<void> enviar({
    required String turnoOrigenId,
    required String turnoDestinoId,
    required String contraparteId,
    required String motivo,
  }) async {
    if (state.isLoading) return;
    state = const AsyncLoading<SalidaPermuta?>();
    final dio = ref.read(dioProvider);
    final clave = ref.read(idempotenciaProvider.notifier).claveDe(formularioId);
    try {
      final r = await DefaultApi(dio).solicitarPermuta(
        idempotencyKey: clave,
        entradaPermuta: EntradaPermuta(
          turnoOrigenId: turnoOrigenId,
          turnoDestinoId: turnoDestinoId,
          contraparteId: contraparteId,
          motivo: motivo,
        ),
      );
      ref.read(idempotenciaProvider.notifier).cerrar(formularioId);
      state = AsyncData(r.data);
    } catch (e) {
      state = AsyncError(errorDeDominio(e), StackTrace.current);
    }
  }
}

final solicitarPermutaProvider =
    AsyncNotifierProvider<SolicitarPermuta, SalidaPermuta?>(
      SolicitarPermuta.new,
    );
