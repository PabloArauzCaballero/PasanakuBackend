import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../proveedores/conexion.dart';
import '../dominio/cliente.dart';
import 'clave_persistida_de_billetera.dart';
import 'error_de_operacion_de_dinero.dart';

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
    try {
      exigirConexionConfirmada(ref);
      final dio = ref.read(dioProvider);
      final claves = ref.read(clavePersistidaDeBilleteraProvider);
      final id = formularioId(cuentaId);
      final clave = await claves.abrir(formularioId: id, datos: [monto, medio]);
      exigirConexionConfirmada(ref);
      final r = await DefaultApi(dio).solicitarRecarga(
        idempotencyKey: clave,
        entradaRecarga: EntradaRecarga(
          cuentaBilleteraId: cuentaId,
          monto: Dinero(monto: monto, moneda: DineroMonedaEnum.BOB),
          medio: medio,
        ),
      );
      final salida = r.data;
      if (salida == null) {
        throw StateError('El servidor no devolvió el estado de la recarga.');
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

final recargarSaldoProvider =
    AsyncNotifierProvider<RecargarSaldo, SalidaRecarga?>(RecargarSaldo.new);
