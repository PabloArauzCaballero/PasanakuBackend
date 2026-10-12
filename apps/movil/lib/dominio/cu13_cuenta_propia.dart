import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'cliente.dart';
import 'errores.dart';

/// La moneda en la que la app abre la billetera de la persona. El contrato la pide
/// (`moneda` es obligatoria en `GET /billetera/cuentas/propia`); la app opera en bolivianos.
const monedaDeLaBilletera = 'BOB';

/// La cuenta de billetera de quien tiene la sesión, tal como la entrega
/// `GET /billetera/cuentas/propia` (`CuentaDeLaSesion` del contrato de nucleo-financiero).
///
/// El cliente Dart generado todavía no trae esta operación (se generó antes de que el
/// contrato la sumara), así que la forma se lee acá, a mano y sin inventar campos: cuando
/// `clientes/dart/nucleo-financiero` se regenere, este tipo se reemplaza por el generado.
class CuentaPropia {
  const CuentaPropia({
    required this.cuentaId,
    required this.moneda,
    required this.estado,
  });

  factory CuentaPropia.fromJson(Map<String, dynamic> json) => CuentaPropia(
    cuentaId: json['cuentaId'] as String,
    moneda: json['moneda'] as String,
    estado: json['estado'] as String,
  );

  final String cuentaId;
  final String moneda;
  final String estado;
}

/// El código con el que el servidor dice «esta persona no tiene una billetera activa en esa
/// moneda» (o tiene más de una y no elige por ella). No es una falla: es un estado.
const codigoSinBilleteraActiva = 'AP-CU13-01';

/// CU-13 · ¿cuál es mi billetera? La cuenta ya no se adivina con un id fijo: la pregunta
/// la responde el servidor con la sesión. Entrega `null` cuando la billetera todavía no está
/// activa (`422 AP-CU13-01`); cualquier otro rechazo o falla sigue siendo un error y se
/// reintenta.
final cuentaPropiaProvider = FutureProvider.autoDispose<CuentaPropia?>((
  ref,
) async {
  final dio = ref.watch(dioProvider);
  try {
    final r = await dio.get<Map<String, dynamic>>(
      '/billetera/cuentas/propia',
      queryParameters: {'moneda': monedaDeLaBilletera},
    );
    return CuentaPropia.fromJson(r.data!);
  } catch (e) {
    final motivo = errorDeDominio(e);
    if (motivo is ErrorDeApi &&
        motivo.estado == 422 &&
        motivo.codigo == codigoSinBilleteraActiva) {
      return null;
    }
    throw motivo;
  }
});
