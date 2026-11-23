import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/cliente.dart';
import '../proveedores/idempotencia.dart';
import '../proveedores/sesion.dart';

/// CU-11 · retirar saldo (cash-out). Primero se retiene el saldo, después se instruye
/// el pago (lo hace el servidor, fuera de esta transacción): la app solo pide el
/// retiro con `factorMfa` ya capturado por el puerto de biometría/OTP del carril M1.
class RetirarSaldo extends AsyncNotifier<SalidaRetiro?> {
  static String formularioId(String cuentaId) => 'cu11-retiro-$cuentaId';

  @override
  Future<SalidaRetiro?> build() async {
    ref.watch(generacionDeSesionProvider);
    return null;
  }

  Future<void> enviar({
    required String cuentaId,
    required String monto,
    required String instrumentoDestinoId,
    required String factorMfa,
  }) async {
    if (state.isLoading) return;
    state = const AsyncLoading<SalidaRetiro?>();
    final dio = ref.read(dioProvider);
    final clave = ref
        .read(idempotenciaProvider.notifier)
        .claveDe(
          formularioId(cuentaId),
          contenido: '$monto|$instrumentoDestinoId',
        );
    try {
      final r = await DefaultApi(dio).solicitarRetiro(
        idempotencyKey: clave,
        entradaRetiro: EntradaRetiro(
          cuentaBilleteraId: cuentaId,
          monto: Importe(monto: monto, moneda: ImporteMonedaEnum.BOB),
          instrumentoDestinoId: instrumentoDestinoId,
          factorMfa: factorMfa,
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

final retirarSaldoProvider = AsyncNotifierProvider<RetirarSaldo, SalidaRetiro?>(
  RetirarSaldo.new,
);
