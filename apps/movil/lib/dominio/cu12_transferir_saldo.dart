import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/cliente.dart';
import '../proveedores/idempotencia.dart';

/// CU-12 · transferir saldo entre billeteras. El dinero no sale del sistema: cambia
/// de bolsillo. Igual exige clave de idempotencia (invariante 7): una transferencia
/// repetida por reintento de red no puede acreditar dos veces al destino.
class TransferirSaldo extends AsyncNotifier<SalidaTransferencia?> {
  static String formularioId(String cuentaId) => 'cu12-transferencia-$cuentaId';

  @override
  Future<SalidaTransferencia?> build() async => null;

  Future<void> enviar({
    required String cuentaOrigenId,
    required String monto,
    required EntradaTransferenciaDestinoTipoEnum tipoDestino,
    required String valorDestino,
    required String concepto,
  }) async {
    if (state.isLoading) return;
    state = const AsyncLoading<SalidaTransferencia?>();
    final dio = ref.read(dioProvider);
    final clave = ref
        .read(idempotenciaProvider.notifier)
        .claveDe(formularioId(cuentaOrigenId));
    try {
      final r = await DefaultApi(dio).transferirSaldo(
        idempotencyKey: clave,
        entradaTransferencia: EntradaTransferencia(
          cuentaOrigenId: cuentaOrigenId,
          destino: EntradaTransferenciaDestino(
            tipo: tipoDestino,
            valor: valorDestino,
          ),
          monto: Dinero(monto: monto, moneda: DineroMonedaEnum.BOB),
          concepto: concepto,
        ),
      );
      ref
          .read(idempotenciaProvider.notifier)
          .cerrar(formularioId(cuentaOrigenId));
      state = AsyncData(r.data);
    } catch (e) {
      state = AsyncError(errorDeDominio(e), StackTrace.current);
    }
  }
}

final transferirSaldoProvider =
    AsyncNotifierProvider<TransferirSaldo, SalidaTransferencia?>(
      TransferirSaldo.new,
    );
