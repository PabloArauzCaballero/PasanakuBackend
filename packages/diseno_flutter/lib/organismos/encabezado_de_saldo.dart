import 'package:flutter/material.dart';

import '../atomos/monto.dart';
import '../tokens/tokens.dart';

/// El saldo, como la cifra más grande de la pantalla, en tinta sobre el fondo.
///
/// Antes era una tarjeta verde con degradado. Una tarjeta dice «esto es un objeto
/// aparte»; el saldo no es un objeto aparte, es el tema de la pantalla. Al sacarle la
/// superficie, la superficie queda libre para señalar **lo que vence**, que es lo único
/// que necesita reclamar atención. El verde sigue mandando en la marca, la barra de
/// pestañas y los estados; acá sobra.
class EncabezadoDeSaldo extends StatelessWidget {
  const EncabezadoDeSaldo({
    super.key,
    required this.etiqueta,
    required this.monto,
    required this.moneda,
    this.etiquetaHablada,
    this.cifras = const [],
    this.acciones,
  });

  final String etiqueta;

  /// Lo que anuncia un lector de pantalla, cuando conviene que diga más que lo que
  /// se ve. En pantalla alcanza «Disponible» —el título de arriba ya dijo que es la
  /// billetera—, pero quien escucha llega al importe sin ese contexto delante, así
  /// que ahí se dice «Saldo disponible» entero. Ver y escuchar no piden lo mismo.
  final String? etiquetaHablada;

  final String monto;
  final String moneda;

  /// El desglose de una línea: «Retenido Bs 150,00 · En pasanakus Bs 600,00».
  final List<({String etiqueta, String monto})> cifras;

  final Widget? acciones;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        MergeSemantics(
          child: Semantics(
            container: true,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                ExcludeSemantics(
                  child: Text(
                    etiqueta,
                    style: Tipo.cuerpoChico.copyWith(color: t.text2),
                  ),
                ),
                const SizedBox(height: Espacio.s1),
                Monto(
                  monto: monto,
                  moneda: moneda,
                  etiqueta: etiquetaHablada ?? etiqueta,
                  estilo: Tipo.cifraGrande.copyWith(color: t.text),
                ),
              ],
            ),
          ),
        ),
        if (cifras.isNotEmpty) ...[
          const SizedBox(height: Espacio.s2),
          Wrap(
            spacing: Espacio.s4,
            runSpacing: Espacio.s1,
            children: [
              for (final c in cifras)
                MergeSemantics(
                  child: Semantics(
                    container: true,
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        ExcludeSemantics(
                          child: Text(
                            '${c.etiqueta} ',
                            style: Tipo.cuerpoChico.copyWith(color: t.text2),
                          ),
                        ),
                        Monto(
                          monto: c.monto,
                          moneda: moneda,
                          etiqueta: c.etiqueta,
                          estilo: Tipo.cuerpoChico.copyWith(
                            color: t.text,
                            fontFamily: Fuente.display,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
            ],
          ),
        ],
        if (acciones != null) ...[
          const SizedBox(height: Espacio.s4),
          acciones!,
        ],
      ],
    );
  }
}
