import 'package:aportaya_cliente_aportes/aportaya_cliente_aportes.dart';
import 'package:aportaya_diseno/errores.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../proveedores/conexion.dart';
import '../../../dominio/cliente.dart';
import '../../../dominio/error_de_operacion_de_dinero.dart';
import 'clave_persistida_de_aporte.dart';

bool montoDeAporteValido(String monto) {
  if (!RegExp(r'^\d+\.\d{2}$').hasMatch(monto)) return false;
  // Solo se comprueba positividad: convertir a double perdería precisión.
  return RegExp(r'[1-9]').hasMatch(monto);
}

class ErrorDeMontoNoVerificable implements Exception, ErrorPresentable {
  const ErrorDeMontoNoVerificable();

  static const texto =
      'No podemos registrar este aporte: falta un monto válido. '
      'No hagas un pago desde este enlace; consultá a soporte antes de continuar.';

  @override
  String get mensaje => texto;

  @override
  String? get trazaId => null;

  @override
  bool get sinConexion => false;
}

/// CU-21 · cobrar el aporte del período. El dinero lo mueve `nucleo-financiero`
/// (invariante 12); esta pantalla solo aplica el pago a la obligación. `esNuevo` en
/// `false` es un reintento con la MISMA clave — no un cobro duplicado.
class CobrarAporte extends AsyncNotifier<SalidaCobro?> {
  CobrarAporte(this.obligacionId);
  final String obligacionId;

  @override
  Future<SalidaCobro?> build() async => null;

  Future<void> enviar({
    required String monto,
    required String canal,
    required String referenciaProveedor,
  }) async {
    if (state.isLoading) return;
    state = const AsyncLoading<SalidaCobro?>();
    try {
      if (!montoDeAporteValido(monto)) {
        throw const ErrorDeMontoNoVerificable();
      }
      exigirConexionConfirmada(ref);
      final dio = ref.read(dioProvider);
      final claves = ref.read(clavePersistidaDeAporteProvider);
      final clave = await claves.abrir(
        obligacionId: obligacionId,
        monto: monto,
        canal: canal,
        referenciaProveedor: referenciaProveedor,
      );
      exigirConexionConfirmada(ref);
      final r = await DefaultApi(dio).cobrarAporte(
        obligacionId: obligacionId,
        idempotencyKey: clave,
        entradaCobro: EntradaCobro(
          monto: Dinero(monto: monto, moneda: DineroMonedaEnum.BOB),
          canal: EntradaCobroCanalEnum.values.byName(canal),
          referenciaProveedor: referenciaProveedor,
        ),
      );
      final salida = r.data;
      if (salida == null) {
        throw StateError('El servidor no devolvió el estado del aporte.');
      }
      state = AsyncData(salida);
      try {
        await claves.confirmar(obligacionId);
      } on Object {
        // El servidor ya respondió: no convertir el éxito en error por un fallo
        // local de limpieza. La clave duradera queda para deduplicar otro intento.
      }
    } catch (e) {
      state = AsyncError(errorDeOperacionDeDinero(e), StackTrace.current);
    }
  }
}

final cobrarAporteProvider =
    AsyncNotifierProvider.family<CobrarAporte, SalidaCobro?, String>(
      CobrarAporte.new,
    );
