import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/atomos/esqueleto.dart';
import 'package:aportaya_diseno/atomos/monto_que_sube.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/cu13_consultar_saldo.dart';
import 'textos.dart';

/// «Tu saldo» mientras se paga la recarga, y el mismo saldo subiendo cuando se acredita.
///
/// Antes del pago muestra el saldo leído del servidor; con [saldoDespues] (lo que devolvió la
/// acreditación) cuenta desde ese valor al nuevo con `MontoQueSube`, que nunca pasa por cero
/// y anuncia al lector de pantalla el valor final. Si el saldo todavía no se pudo leer y no hay
/// acreditación, no se inventa un número: queda el esqueleto, o nada si la lectura falló.
class SaldoDeLaRecarga extends ConsumerWidget {
  const SaldoDeLaRecarga({
    super.key,
    required this.cuentaId,
    required this.saldoDespues,
  });
  final String cuentaId;
  final Dinero? saldoDespues;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final t = Tokens.of(context);
    final leido = ref.watch(saldoProvider(cuentaId));
    final monto = saldoDespues ?? leido.value?.disponible;
    if (monto == null && leido.hasError) return const SizedBox.shrink();
    return Column(
      children: [
        Text(
          TextosBilletera.tuSaldo,
          style: Tipo.etiqueta.copyWith(color: t.text2),
        ),
        const SizedBox(height: Espacio.s1),
        if (monto == null)
          const Esqueleto(filas: 1)
        else
          MontoQueSube(
            monto: monto.monto,
            moneda: monto.moneda.value,
            etiqueta: TextosBilletera.tuSaldo,
            estilo: Tipo.cifraGrande.copyWith(color: t.text),
          ),
      ],
    );
  }
}
