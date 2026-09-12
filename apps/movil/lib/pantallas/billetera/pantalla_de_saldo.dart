import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../dominio/cu13_consultar_saldo.dart';
import '../../pantallas/billetera/textos.dart';
import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';

/// Pantalla de inicio de la billetera (`docs/Views/AportaYa-Maqueta.html`, función
/// `pintarSaldo`): el saldo disponible con su nota de custodia, dos acciones
/// (aportes pendientes primero, movimientos después) y el desglose de retenido.
///
/// **Supuesto declarado.** La maqueta también desglosa «Puesto en pasanakus» (lo
/// aportado a un grupo que todavía no se cobró). Ese número sale de cruzar
/// `grupos`/`aportes` con el turno de cada uno — no es un campo de
/// `GET /billetera/{id}/saldo` (`SaldoBilletera` solo trae `disponible`, `retenido`,
/// `alCorteDe`). Mostrarlo acá sería inventarlo. Se deja pedido al carril que posea
/// esa agregación cruzada (M3, `pasanaku.miEstado`, donde el dato sí existe).
class PantallaDeSaldo extends ConsumerWidget {
  const PantallaDeSaldo({super.key, required this.cuentaId});

  final String cuentaId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final saldo = ref.watch(saldoProvider(cuentaId));
    return Scaffold(
      appBar: AppBar(title: const Text(TextosBilletera.titulo)),
      body: EstadoDePantalla<SaldoBilletera>(
        valor: saldo,
        etiquetaDeCarga: TextosBilletera.cargandoSaldo,
        vacio: saldoEnCero,
        mensajeVacio: TextosBilletera.sinMovimientos,
        reintentar: () => ref.invalidate(saldoProvider(cuentaId)),
        exito: (s) => _TarjetaSaldo(saldo: s),
      ),
    );
  }
}

class _TarjetaSaldo extends StatelessWidget {
  const _TarjetaSaldo({required this.saldo});
  final SaldoBilletera saldo;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    final hayRetenido = saldo.retenido.monto != '0.00';
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Container(
        padding: const EdgeInsets.all(Espacio.s5),
        decoration: BoxDecoration(
          color: t.verdeSolido,
          borderRadius: BorderRadius.circular(Radios.lg),
          boxShadow: [t.sombra2],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  Icons.shield_outlined,
                  color: t.sobreVerdeSolido,
                  size: 18,
                ),
                const SizedBox(width: Espacio.s1),
                Text(
                  TextosBilletera.saldoDisponible,
                  style: texto.labelLarge?.copyWith(color: t.sobreVerdeSolido),
                ),
              ],
            ),
            const SizedBox(height: Espacio.s2),
            Monto(
              monto: saldo.disponible.monto,
              moneda: saldo.disponible.moneda.value,
              etiqueta: TextosBilletera.saldoDisponible,
              estilo: texto.headlineMedium?.copyWith(
                color: t.sobreVerdeSolido,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: Espacio.s2),
            Text(
              TextosBilletera.custodia,
              style: texto.bodySmall?.copyWith(color: t.sobreVerdeSolido),
            ),
            const SizedBox(height: Espacio.s4),
            Row(
              children: [
                Expanded(
                  child: FilledButton(
                    onPressed: () => context.pushNamed('pasanaku.miEstado'),
                    style: FilledButton.styleFrom(
                      backgroundColor: t.accent,
                      foregroundColor: t.accentInk,
                    ),
                    child: const Text(TextosBilletera.verAportesPendientes),
                  ),
                ),
                const SizedBox(width: Espacio.s2),
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => context.pushNamed(
                      'billetera.extracto',
                      queryParameters: {'cuenta': saldo.cuentaId},
                    ),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: t.sobreVerdeSolido,
                      side: BorderSide(color: t.sobreVerdeSolido),
                    ),
                    child: const Text(TextosBilletera.movimientos),
                  ),
                ),
              ],
            ),
            const SizedBox(height: Espacio.s4),
            Row(
              children: [
                Text(
                  '${TextosBilletera.saldoRetenido} ',
                  style: texto.bodySmall?.copyWith(color: t.sobreVerdeSolido),
                ),
                Monto(
                  monto: saldo.retenido.monto,
                  moneda: saldo.retenido.moneda.value,
                  etiqueta: TextosBilletera.saldoRetenido,
                  estilo: texto.bodySmall?.copyWith(color: t.sobreVerdeSolido),
                ),
                if (!hayRetenido) ...[
                  const SizedBox(width: Espacio.s2),
                  Text(
                    '· ${TextosBilletera.nadaTrabado}',
                    style: texto.bodySmall?.copyWith(
                      color: t.sobreVerdeSolido.withValues(alpha: 0.8),
                    ),
                  ),
                ],
              ],
            ),
          ],
        ),
      ),
    );
  }
}
