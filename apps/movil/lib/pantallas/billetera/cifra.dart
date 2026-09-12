import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter/material.dart';

import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';

/// Una cifra con etiqueta (RETENIDO, PUESTO EN PASANAKUS…) y nota opcional, en su
/// propio límite de semántica para que un lector de pantalla no la fusione con el
/// resto de la tarjeta de saldo.
class Cifra extends StatelessWidget {
  const Cifra({
    super.key,
    required this.etiqueta,
    required this.monto,
    this.nota,
  });
  final String etiqueta;
  final Dinero monto;
  final String? nota;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return MergeSemantics(
      child: Semantics(
        container: true,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            ExcludeSemantics(
              child: Text(
                etiqueta.toUpperCase(),
                style: texto.labelSmall?.copyWith(
                  color: t.sobreVerdeSolido.withValues(alpha: 0.8),
                  letterSpacing: 0.4,
                ),
              ),
            ),
            const SizedBox(height: Espacio.s1),
            Monto(
              monto: monto.monto,
              moneda: monto.moneda.value,
              etiqueta: etiqueta,
              estilo: texto.titleMedium?.copyWith(
                color: t.sobreVerdeSolido,
                fontWeight: FontWeight.w600,
              ),
            ),
            if (nota != null) ...[
              const SizedBox(height: Espacio.s1),
              Text(
                nota!,
                style: texto.bodySmall?.copyWith(
                  color: t.sobreVerdeSolido.withValues(alpha: 0.8),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
