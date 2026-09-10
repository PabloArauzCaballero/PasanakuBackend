import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/cliente.dart';

/// CU-13 · consultar el saldo. Un archivo por caso de uso, sobre el cliente generado:
/// el tipo `SaldoBilletera` viene de `clientes/dart`, nunca se reescribe (invariante 2).
///
/// El saldo **no se cachea entre operaciones**: tras recargar o pagar, la pantalla lo
/// relee (`ref.invalidate`) en vez de sumar en memoria (regla §0.2b del flujo).
final saldoProvider = FutureProvider.autoDispose.family<SaldoBilletera, String>(
  (ref, cuentaId) async {
    final dio = ref.watch(dioProvider);
    try {
      final r = await dio.get<Map<String, dynamic>>(
        '/billetera/$cuentaId/saldo',
      );
      return SaldoBilletera.fromJson(r.data!);
    } catch (e) {
      throw errorDeDominio(e);
    }
  },
);

/// Qué es «vacío» para el saldo: una cuenta en cero **no es un error**, es una cuenta nueva.
bool saldoEnCero(SaldoBilletera s) =>
    s.disponible.monto == '0.00' && s.retenido.monto == '0.00';
