import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../proveedores/conexion.dart';
import '../dominio/cliente.dart';
import 'clave_persistida_de_billetera.dart';
import 'error_de_operacion_de_dinero.dart';

/// CU-11 · retirar saldo (cash-out). Primero se retiene el saldo, después se instruye
/// el pago (lo hace el servidor, fuera de esta transacción): la app solo pide el
/// retiro con `factorMfa` ya capturado por el puerto de biometría/OTP del carril M1.
class RetirarSaldo extends AsyncNotifier<SalidaRetiro?> {
  static String formularioId(String cuentaId) => 'cu11-retiro-$cuentaId';

  @override
  Future<SalidaRetiro?> build() async => null;

  Future<void> enviar({
    required String cuentaId,
    required String monto,
    required String instrumentoDestinoId,
    required String factorMfa,
  }) async {
    if (state.isLoading) return;
    state = const AsyncLoading<SalidaRetiro?>();
    try {
      exigirConexionConfirmada(ref);
      final dio = ref.read(dioProvider);
      final claves = ref.read(clavePersistidaDeBilleteraProvider);
      final id = formularioId(cuentaId);
      // El factor MFA es secreto y puede caducar: no se guarda ni se incluye
      // en la huella de la transacción monetaria.
      final clave = await claves.abrir(
        formularioId: id,
        datos: [monto, instrumentoDestinoId],
      );
      exigirConexionConfirmada(ref);
      final r = await DefaultApi(dio).solicitarRetiro(
        idempotencyKey: clave,
        entradaRetiro: EntradaRetiro(
          cuentaBilleteraId: cuentaId,
          monto: Dinero(monto: monto, moneda: DineroMonedaEnum.BOB),
          instrumentoDestinoId: instrumentoDestinoId,
          factorMfa: factorMfa,
        ),
      );
      final salida = r.data;
      if (salida == null) {
        throw StateError('El servidor no devolvió el estado del retiro.');
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

final retirarSaldoProvider = AsyncNotifierProvider<RetirarSaldo, SalidaRetiro?>(
  RetirarSaldo.new,
);
