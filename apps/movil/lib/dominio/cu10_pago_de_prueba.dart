import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/cliente.dart';
import '../proveedores/idempotencia.dart';
import '../proveedores/sesion.dart';
import 'cu13_consultar_saldo.dart';

/// CU-10 · «Simular pago» de un QR **de prueba**. Solo existe donde el servidor tiene el
/// proveedor de prueba (TEST); en producción responde 404 y, además, ningún QR llega con
/// origen PRUEBA, así que el botón no aparece.
///
/// El servidor acredita por el camino real (mismo asiento, idempotente por orden). Acá no se
/// suma nada en memoria: el saldo nuevo es `saldoDespues`, y después se relee el saldo
/// (`saldoProvider`) para que la billetera muestre lo mismo que el libro.
class PagarRecargaDePrueba extends AsyncNotifier<SalidaAcreditacion?> {
  static String formularioId(String ordenId) => 'cu10-pago-de-prueba-$ordenId';

  @override
  Future<SalidaAcreditacion?> build() async {
    ref.watch(generacionDeSesionProvider);
    return null;
  }

  Future<void> pagar({
    required String cuentaId,
    required String ordenId,
  }) async {
    if (state.isLoading) return; // el botón ya está bloqueado; refuerzo
    state = const AsyncLoading<SalidaAcreditacion?>();
    final dio = ref.read(dioProvider);
    final clave = ref
        .read(idempotenciaProvider.notifier)
        .claveDe(formularioId(ordenId));
    try {
      final r = await DefaultApi(
        dio,
      ).pagarRecargaDePrueba(ordenId: ordenId, idempotencyKey: clave);
      ref.read(idempotenciaProvider.notifier).cerrar(formularioId(ordenId));
      state = AsyncData(r.data);
      ref.invalidate(saldoProvider(cuentaId));
    } catch (e) {
      state = AsyncError(errorDeDominio(e), StackTrace.current);
    }
  }
}

/// Se libera al salir de la pantalla: la próxima recarga empieza sin el pago de la anterior.
final pagarRecargaDePruebaProvider =
    AsyncNotifierProvider.autoDispose<
      PagarRecargaDePrueba,
      SalidaAcreditacion?
    >(PagarRecargaDePrueba.new);
