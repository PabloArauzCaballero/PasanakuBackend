import 'package:aportaya_cliente_aportes/aportaya_cliente_aportes.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';

/// CU-21 · cobrar el aporte del período. El dinero lo mueve `nucleo-financiero`
/// (invariante 12); esta pantalla solo aplica el pago a la obligación. `esNuevo` en
/// `false` es un reintento con la MISMA clave — no un cobro duplicado.
class CobrarAporte extends AsyncNotifier<SalidaCobro?> {
  CobrarAporte(this.obligacionId);
  final String obligacionId;

  String get _formularioId => 'cu21-cobrar-$obligacionId';

  @override
  Future<SalidaCobro?> build() async => null;

  Future<void> enviar({
    required String monto,
    required String canal,
    required String referenciaProveedor,
  }) async {
    if (state.isLoading) return;
    state = const AsyncLoading<SalidaCobro?>();
    final dio = ref.read(dioProvider);
    final clave = ref
        .read(idempotenciaProvider.notifier)
        .claveDe(_formularioId);
    try {
      final r = await DefaultApi(dio).cobrarAporte(
        obligacionId: obligacionId,
        idempotencyKey: clave,
        entradaCobro: EntradaCobro(
          monto: Dinero(monto: monto, moneda: DineroMonedaEnum.BOB),
          canal: EntradaCobroCanalEnum.values.byName(canal),
          referenciaProveedor: referenciaProveedor,
        ),
      );
      ref.read(idempotenciaProvider.notifier).cerrar(_formularioId);
      state = AsyncData(r.data);
    } catch (e) {
      state = AsyncError(errorDeDominio(e), StackTrace.current);
    }
  }
}

final cobrarAporteProvider =
    AsyncNotifierProvider.family<CobrarAporte, SalidaCobro?, String>(
      CobrarAporte.new,
    );
