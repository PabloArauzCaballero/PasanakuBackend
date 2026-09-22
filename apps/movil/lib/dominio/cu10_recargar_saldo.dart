import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/cliente.dart';
import '../proveedores/idempotencia.dart';

/// CU-10 · recargar saldo (cash-in). La orden queda `PENDIENTE`: la acreditación la
/// confirma el proveedor por webhook (flujo 3 del CU); acá solo se crea la orden con
/// su clave de idempotencia, nunca se ajusta el saldo en memoria.
///
/// Un provider por pantalla (no por cuenta, siguiendo `altaProvider` de M1): el
/// identificador del formulario de idempotencia sí es fijo por cuenta, así que un
/// reintento (doble toque, reconexión) reusa la MISMA clave y el servidor responde
/// sin duplicar (`R-BIL-06`, invariante 7).
class RecargarSaldo extends AsyncNotifier<SalidaRecarga?> {
  static String formularioId(String cuentaId) => 'cu10-recarga-$cuentaId';

  @override
  Future<SalidaRecarga?> build() async => null;

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
        .claveDe(formularioId(cuentaId));
    try {
      final r = await DefaultApi(dio).solicitarRecarga(
        idempotencyKey: clave,
        entradaRecarga: EntradaRecarga(
          cuentaBilleteraId: cuentaId,
          monto: Dinero(monto: monto, moneda: DineroMonedaEnum.BOB),
          medio: medio,
        ),
      );
      ref.read(idempotenciaProvider.notifier).cerrar(formularioId(cuentaId));
      state = AsyncData(r.data);
    } catch (e) {
      state = AsyncError(errorDeDominio(e), StackTrace.current);
    }
  }
}

final recargarSaldoProvider =
    AsyncNotifierProvider<RecargarSaldo, SalidaRecarga?>(RecargarSaldo.new);
