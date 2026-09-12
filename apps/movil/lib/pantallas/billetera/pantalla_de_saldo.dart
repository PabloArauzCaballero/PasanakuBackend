import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../dominio/cu13_consultar_saldo.dart';
import '../../pantallas/billetera/textos.dart';
import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';

/// La pantalla real de la fase F0: compone organismos, sin lógica. Sus cuatro estados
/// los pinta `EstadoDePantalla`; el importe lo pinta `Monto`.
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
            Text(
              TextosBilletera.saldoDisponible,
              style: texto.labelLarge?.copyWith(color: t.sobreVerdeSolido),
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
            const SizedBox(height: Espacio.s3),
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
              ],
            ),
            const SizedBox(height: Espacio.s4),
            SizedBox(
              height: Tactil.minimo,
              child: FilledButton(
                onPressed: () => context.pushNamed(
                  'billetera.recargar',
                  queryParameters: {'cuenta': saldo.cuentaId},
                ),
                style: FilledButton.styleFrom(
                  backgroundColor: t.accent,
                  foregroundColor: t.accentInk,
                ),
                child: const Text(TextosBilletera.recargar),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
