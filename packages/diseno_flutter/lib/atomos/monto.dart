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
    this.permitirCorteEntreGrupos = false,
  });

  final String monto;
  final String moneda;

  /// Lo que el lector de pantalla dice antes de la cifra («Saldo disponible»).
  final String? etiqueta;
  final TextStyle? estilo;

  /// En cifras protagonistas muy largas permite envolver tras un separador de
  /// miles, sin partir un grupo de digitos ni alterar la lectura accesible.
  final bool permitirCorteEntreGrupos;

  @override
  Widget build(BuildContext context) {
    final texto = formatearMonto(monto: monto, moneda: moneda);
    final base = estilo ?? Theme.of(context).textTheme.titleLarge!;
    final estiloDeCifra = base.copyWith(
      fontFamily: Fuente.display,
      fontFeatures: const [FontFeature.tabularFigures()],
    );
    return Semantics(
      label: montoParaLectura(monto: monto, moneda: moneda, etiqueta: etiqueta),
      excludeSemantics: true,
      child: permitirCorteEntreGrupos
          ? _MontoConCortes(texto: texto, estilo: estiloDeCifra)
          : Text(texto, style: estiloDeCifra),
    );
  }
}

class _MontoConCortes extends StatelessWidget {
  const _MontoConCortes({required this.texto, required this.estilo});

  final String texto;
  final TextStyle estilo;

  @override
  Widget build(BuildContext context) {
    final separador = texto.indexOf(' ');
    if (separador < 0) return Text(texto, style: estilo);
    final moneda = texto.substring(0, separador);
    final cifra = texto.substring(separador + 1);
    return LayoutBuilder(
      builder: (context, limites) {
        final medidorCompleto = TextPainter(
          text: TextSpan(text: texto, style: estilo),
          textDirection: Directionality.of(context),
          textScaler: MediaQuery.textScalerOf(context),
          maxLines: 1,
        )..layout();
        final cabeTodo = medidorCompleto.width <= limites.maxWidth;
        medidorCompleto.dispose();
        if (cabeTodo) return Text(texto, style: estilo, softWrap: false);

        final medidor = TextPainter(
          text: TextSpan(text: cifra, style: estilo),
          textDirection: Directionality.of(context),
          textScaler: MediaQuery.textScalerOf(context),
          maxLines: 1,
        )..layout();
        final cabeEntera = medidor.width <= limites.maxWidth;
        medidor.dispose();
        final grupos = cifra.split('.');
        return Wrap(
          crossAxisAlignment: WrapCrossAlignment.end,
          children: [
            Text('$moneda ', style: estilo, softWrap: false),
            if (cabeEntera || grupos.length == 1)
              Text(cifra, style: estilo, softWrap: false)
            else
              for (var i = 0; i < grupos.length; i++)
                Text(
                  '${grupos[i]}${i < grupos.length - 1 ? '.' : ''}',
                  style: estilo,
                  softWrap: false,
                ),
          ],
        );
      },
    );
  }
}
