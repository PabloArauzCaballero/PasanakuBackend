import 'package:flutter/material.dart';

import '../atomos/monto.dart';
import '../tokens/tokens.dart';

/// El encabezado verde con el saldo: disponible en grande, retenido y en pasanakus
/// como líneas, y **una** acción naranja.
class TarjetaSaldo extends StatelessWidget {
  const TarjetaSaldo({
    super.key,
    required this.disponible,
    required this.moneda,
    this.retenido,
    this.enPasanakus,
    this.accionPrincipal,
    this.accionSecundaria,
  });

  final String disponible;
  final String moneda;
  final String? retenido;
  final String? enPasanakus;
  final Widget? accionPrincipal;
  final Widget? accionSecundaria;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    Widget linea(String etiqueta, String monto) => Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Expanded(
          child: Text(
            etiqueta,
            style: texto.bodySmall?.copyWith(color: t.sobreVerdeSolido),
          ),
        ),
        Monto(
          monto: monto,
          moneda: moneda,
          etiqueta: etiqueta,
          estilo: texto.bodySmall?.copyWith(color: t.sobreVerdeSolido),
        ),
      ],
    );
    return Container(
      width: double.infinity,
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
            'Saldo disponible',
            style: texto.labelLarge?.copyWith(color: t.sobreVerdeSolido),
          ),
          const SizedBox(height: Espacio.s1),
          Monto(
            monto: disponible,
            moneda: moneda,
            etiqueta: 'Saldo disponible',
            estilo: texto.headlineMedium?.copyWith(
              color: t.sobreVerdeSolido,
              fontWeight: FontWeight.w700,
            ),
          ),
          if (retenido != null || enPasanakus != null)
            const SizedBox(height: Espacio.s3),
          if (retenido != null) linea('Retenido', retenido!),
          if (enPasanakus != null) linea('En pasanakus', enPasanakus!),
          if (accionPrincipal != null || accionSecundaria != null) ...[
            const SizedBox(height: Espacio.s4),
            Row(
              children: [
                if (accionPrincipal != null) Expanded(child: accionPrincipal!),
                if (accionPrincipal != null && accionSecundaria != null)
                  const SizedBox(width: Espacio.s3),
                if (accionSecundaria != null)
                  Expanded(child: accionSecundaria!),
              ],
            ),
          ],
        ],
      ),
    );
  }
}
