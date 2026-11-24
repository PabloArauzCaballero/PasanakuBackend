import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/atomos/esqueleto.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/monto_que_sube.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/cu13_consultar_saldo.dart';
import 'textos.dart';

/// El saldo de la cuenta mientras se paga la recarga, y el mismo saldo subiendo cuando se acredita.
///
/// Mientras se espera es un dato secundario («Saldo actual», en una fila): lo que importa en ese
/// momento es el monto y el QR. Con [saldoDespues] (lo que devolvió la acreditación) pasa a
/// «Tu saldo ahora», destacado, y cuenta desde el valor anterior al nuevo con `MontoQueSube`, que
/// nunca pasa por cero y anuncia al lector de pantalla el valor final. La fila tiene la MISMA
/// forma en los dos momentos: así el `MontoQueSube` es el mismo y sube desde donde estaba.
/// Si el saldo todavía no se pudo leer y no hay acreditación, no se inventa un número: queda el
/// esqueleto, o nada si la lectura falló.
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
    final acreditado = saldoDespues != null;
    final etiqueta = acreditado
        ? TextosBilletera.tuSaldoAhora
        : TextosBilletera.saldoActual;
    return AnimatedContainer(
      duration: duracion(context, Movimiento.entrada),
      padding: const EdgeInsets.symmetric(
        horizontal: Espacio.s4,
        vertical: Espacio.s3,
      ),
      decoration: BoxDecoration(
        color: acreditado ? t.okBg : t.surface2,
        borderRadius: BorderRadius.circular(Radios.md),
        border: Border.all(
          color: acreditado ? t.okTexto.withValues(alpha: 0.35) : t.border,
          width: Borde.fino,
        ),
      ),
      child: Row(
        children: [
          Icon(
            IconosDeMarca.billetera,
            size: Espacio.s5,
            color: acreditado ? t.okTexto : t.text2,
          ),
          const SizedBox(width: Espacio.s3),
          Expanded(
            child: Text(etiqueta, style: Tipo.cuerpo.copyWith(color: t.text2)),
          ),
          const SizedBox(width: Espacio.s3),
          if (monto == null)
            const SizedBox(width: Espacio.s7 * 2, child: Esqueleto(filas: 1))
          else
            MontoQueSube(
              monto: monto.monto,
              moneda: monto.moneda.value,
              etiqueta: etiqueta,
              estilo: (acreditado ? Tipo.cifra : Tipo.cuerpoFuerte).copyWith(
                color: t.text,
              ),
            ),
        ],
      ),
    );
  }
}
