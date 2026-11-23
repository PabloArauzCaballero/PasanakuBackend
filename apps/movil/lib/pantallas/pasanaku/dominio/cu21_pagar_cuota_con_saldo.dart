import 'package:aportaya_cliente_aportes/aportaya_cliente_aportes.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';
import '../../../proveedores/sesion.dart';

/// CU-21 · pagar la cuota con el saldo de la billetera (F1.L1.M11, P-06).
///
/// La app no dicta nada del cobro: el monto es el pendiente que el servidor lee de la
/// obligación, el canal es el saldo y la referencia la fija el servidor. Por eso la
/// petición no lleva campos (`{}`), solo la clave de idempotencia: 201 cobró, 200 es la
/// respuesta de un reintento con la misma clave (no un cobro doble).
class PagarCuotaConSaldo extends AsyncNotifier<SalidaCobro?> {
  PagarCuotaConSaldo(this.obligacionId);
  final String obligacionId;

  String get _formularioId => 'cu21-pagar-con-saldo-$obligacionId';

  @override
  Future<SalidaCobro?> build() async {
    ref.watch(generacionDeSesionProvider);
    return null;
  }

  Future<void> pagar() async {
    if (state.isLoading) return; // el botón ya está bloqueado; refuerzo
    state = const AsyncLoading<SalidaCobro?>();
    final idempotencia = ref.read(idempotenciaProvider.notifier);
    final clave = idempotencia.claveDe(_formularioId);
    try {
      // El cliente generado serializa siempre el cuerpo (sin él viajaría el literal
      // `null`); el contrato acepta «ausente o {}», y {} es el caso que valida el esquema.
      final r = await DefaultApi(ref.read(dioProvider)).pagarCuotaConSaldo(
        obligacionId: obligacionId,
        idempotencyKey: clave,
        body: const <String, Object?>{},
      );
      idempotencia.cerrar(_formularioId);
      state = AsyncData(r.data);
    } catch (e) {
      // F1.L6.M12: un 4xx libera la clave; un timeout o un 5xx la conserva.
      idempotencia.trasElFallo(_formularioId, e);
      state = AsyncError(errorDeDominio(e), StackTrace.current);
    }
  }
}

final pagarCuotaConSaldoProvider =
    AsyncNotifierProvider.family<PagarCuotaConSaldo, SalidaCobro?, String>(
      PagarCuotaConSaldo.new,
    );
