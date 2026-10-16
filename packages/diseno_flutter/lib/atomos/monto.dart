import 'package:flutter/material.dart';

import '../dinero/formatear.dart';
import '../tokens/tokens.dart';

/// El único lugar de la app donde se dibuja dinero: `tabular-nums`, prefijo `Bs`,
/// coma decimal. Recibe el importe **como cadena del contrato** y no lo convierte.
class Monto extends StatelessWidget {
  const Monto({
    super.key,
    required this.monto,
    required this.moneda,
    this.etiqueta,
    this.estilo,
  });

  final String monto;
  final String moneda;

  /// Lo que el lector de pantalla dice antes de la cifra («Saldo disponible»).
  final String? etiqueta;
  final TextStyle? estilo;

  @override
  Widget build(BuildContext context) {
    final texto = formatearMonto(monto: monto, moneda: moneda);
    final base = estilo ?? Theme.of(context).textTheme.titleLarge!;
    return Semantics(
      label: montoParaLectura(monto: monto, moneda: moneda, etiqueta: etiqueta),
      excludeSemantics: true,
      child: Text(
        texto,
        style: base.copyWith(
          fontFamily: Fuente.display,
          fontFeatures: const [FontFeature.tabularFigures()],
        ),
      ),
    );
  }
}
