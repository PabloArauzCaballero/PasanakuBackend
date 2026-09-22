import 'package:flutter/material.dart';

import '../atomos/monto.dart';
import '../tokens/tokens.dart';

/// Entró y salió del período: la cabecera del extracto y de cualquier filtro.
class ResumenDePeriodo extends StatelessWidget {
  const ResumenDePeriodo({
    super.key,
    required this.titulo,
    required this.entro,
    required this.salio,
    required this.moneda,
    this.saldoInicial,
    this.saldoFinal,
  });
  final String titulo;
  final String entro;
  final String salio;
  final String moneda;
  final String? saldoInicial;
  final String? saldoFinal;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    Widget celda(String etiqueta, String monto, Color color) => Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(etiqueta, style: texto.labelMedium?.copyWith(color: t.text3)),
          Monto(
            monto: monto,
            moneda: moneda,
            etiqueta: etiqueta,
            estilo: texto.titleMedium?.copyWith(color: color),
          ),
        ],
      ),
    );
    return Container(
      padding: const EdgeInsets.all(Espacio.s4),
      decoration: BoxDecoration(
        color: t.surface2,
        borderRadius: BorderRadius.circular(Radios.lg),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(titulo, style: texto.labelLarge?.copyWith(color: t.text2)),
          const SizedBox(height: Espacio.s2),
          Row(
            children: [
              celda('Entró', entro, t.okTexto),
              celda('Salió', salio, t.text),
            ],
          ),
          if (saldoInicial != null && saldoFinal != null) ...[
            const SizedBox(height: Espacio.s2),
            Row(
              children: [
                celda('Saldo inicial', saldoInicial!, t.text2),
                celda('Saldo de hoy', saldoFinal!, t.brandTexto),
              ],
            ),
          ],
        ],
      ),
    );
  }
}
