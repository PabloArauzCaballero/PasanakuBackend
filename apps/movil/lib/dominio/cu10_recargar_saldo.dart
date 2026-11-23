import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/cliente.dart';
import '../proveedores/idempotencia.dart';
import '../proveedores/sesion.dart';

/// CU-10 · recargar saldo (cash-in). La orden queda `PENDIENTE`: la acreditación la
/// confirma el proveedor por webhook (flujo 3 del CU); acá solo se crea la orden con
/// su clave de idempotencia, nunca se ajusta el saldo en memoria.
///
/// La clave es por intento (F1.L6.M12): cuenta + importe + medio. Reenviar lo mismo
/// (doble toque, reconexión, timeout) reusa la MISMA clave y el servidor responde sin
/// duplicar (`R-BIL-06`, invariante 7). Si el servidor rechaza (4xx) la clave se
/// libera, y corregir el importe es otro intento con otra clave.
class RecargarSaldo extends AsyncNotifier<SalidaRecarga?> {
  static String formularioId(String cuentaId) => 'cu10-recarga-$cuentaId';

  @override
  Future<SalidaRecarga?> build() async {
    ref.watch(generacionDeSesionProvider);
    return null;
  }

  Future<void> enviar({
    required String cuentaId,
    required String monto,
    required String medio,
  }) async {
    if (state.isLoading) return; // el botón ya está bloqueado; refuerzo
    state = const AsyncLoading<SalidaRecarga?>();
    final dio = ref.read(dioProvider);
    final clave = ref
        .read(idempotenciaProvider.notifier)
        .claveDe(formularioId(cuentaId), contenido: '$monto|$medio');
    try {
      final r = await DefaultApi(dio).solicitarRecarga(
        idempotencyKey: clave,
        entradaRecarga: EntradaRecarga(
          cuentaBilleteraId: cuentaId,
          monto: Importe(monto: monto, moneda: ImporteMonedaEnum.BOB),
          medio: medio,
        ),
      );
      ref.read(idempotenciaProvider.notifier).cerrar(formularioId(cuentaId));
      state = AsyncData(r.data);
    } catch (e) {
      // F1.L6.M12: un 4xx libera la clave (corregir y reenviar es otro intento);
      // un timeout o un 5xx la conserva (el reintento no duplica).
      ref
          .read(idempotenciaProvider.notifier)
          .trasElFallo(formularioId(cuentaId), e);
      state = AsyncError(errorDeDominio(e), StackTrace.current);
    }
  }
}

final recargarSaldoProvider =
    AsyncNotifierProvider<RecargarSaldo, SalidaRecarga?>(RecargarSaldo.new);
