import 'package:aportaya_cliente_entregas/aportaya_cliente_entregas.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/sesion.dart';
import 'subasta_del_cupo.dart';

/// Retirar mi cupo de la subasta mientras nadie ofreció. Cancelar dos veces es la
/// misma cancelación (el servidor responde la subasta ya retirada), así que no lleva
/// clave de idempotencia.
class RetirarSubasta extends AsyncNotifier<VistaDeSubasta?> {
  @override
  Future<VistaDeSubasta?> build() async {
    ref.watch(generacionDeSesionProvider);
    return null;
  }

  Future<void> retirar(String subastaId, String cupoId) async {
    if (state.isLoading) return;
    state = const AsyncLoading<VistaDeSubasta?>();
    try {
      final r = await DefaultApi(
        ref.read(dioProvider),
      ).cancelarSubastaDeCupo(subastaId: subastaId);
      state = AsyncData(r.data);
    } catch (e) {
      state = AsyncError(errorDeDominio(e), StackTrace.current);
    } finally {
      ref.invalidate(subastaDelCupoProvider(cupoId));
    }
  }
}

final retirarSubastaProvider =
    AsyncNotifierProvider.autoDispose<RetirarSubasta, VistaDeSubasta?>(
      RetirarSubasta.new,
    );
