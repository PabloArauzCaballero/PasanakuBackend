import 'package:flutter/material.dart';

import '../atomos/monto.dart';
import '../tokens/tokens.dart';

/// Bruto, comisión, descuento y neto en filas, con el total separado (D-19).
class DesgloseDeCobro extends StatelessWidget {
  const DesgloseDeCobro({
    super.key,
    required this.lineas,
    required this.neto,
    required this.moneda,
    this.nota,
  });

  /// `(concepto, monto)`; un descuento va con signo negativo, una comisión también.
  final List<({String concepto, String monto})> lineas;
  final String neto;
  final String moneda;
  final String? nota;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Container(
      padding: const EdgeInsets.all(Espacio.s4),
      decoration: BoxDecoration(
        color: t.surface,
        borderRadius: BorderRadius.circular(Radios.lg),
        border: Border.all(color: t.border, width: Borde.fino),
      ),
      child: Column(
        children: [
          for (final l in lineas)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: Espacio.s1),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Text(
                      l.concepto,
                      style: texto.bodyMedium?.copyWith(color: t.text2),
                    ),
                  ),
                  Monto(
                    monto: l.monto,
                    moneda: moneda,
                    etiqueta: l.concepto,
                    estilo: texto.bodyMedium?.copyWith(color: t.text),
                  ),
                ],
              ),
            ),
          Divider(color: t.border, height: Espacio.s4),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Neto', style: texto.titleMedium?.copyWith(color: t.text)),
              Monto(
                monto: neto,
                moneda: moneda,
                etiqueta: 'Neto',
                estilo: texto.headlineSmall?.copyWith(
                  color: t.brandTexto,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
          if (nota != null) ...[
            const SizedBox(height: Espacio.s2),
            Text(nota!, style: texto.bodySmall?.copyWith(color: t.text3)),
          ],
        ],
      ),
    );
  }
}
