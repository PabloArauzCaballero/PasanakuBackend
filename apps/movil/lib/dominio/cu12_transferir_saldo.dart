import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../proveedores/conexion.dart';
import '../dominio/cliente.dart';
import 'clave_persistida_de_billetera.dart';
import 'error_de_operacion_de_dinero.dart';

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
    try {
      exigirConexionConfirmada(ref);
      final dio = ref.read(dioProvider);
      final claves = ref.read(clavePersistidaDeBilleteraProvider);
      final id = formularioId(cuentaOrigenId);
      final clave = await claves.abrir(
        formularioId: id,
        datos: [monto, tipoDestino.name, valorDestino, concepto],
      );
      exigirConexionConfirmada(ref);
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
      final salida = r.data;
      if (salida == null) {
        throw StateError(
          'El servidor no devolvió el estado de la transferencia.',
        );
      }
      state = AsyncData(salida);
      try {
        await claves.confirmar(id);
      } on Object {
        // El éxito remoto no se convierte en error por una limpieza local fallida.
      }
    } catch (e) {
      state = AsyncError(errorDeOperacionDeDinero(e), StackTrace.current);
    }
  }
}

final transferirSaldoProvider =
    AsyncNotifierProvider<TransferirSaldo, SalidaTransferencia?>(
      TransferirSaldo.new,
    );
