import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Los 14 requisitos como cumplidos y faltantes, con tu valor al lado del umbral y el
/// código de la fila (D-16). Ningún umbral vive en la app: llega por contrato.
class ListaDeRequisitos extends StatelessWidget {
  const ListaDeRequisitos({super.key, required this.requisitos});
  final List<
    ({
      String codigo,
      String nombre,
      String umbral,
      String tuValor,
      bool cumplido,
    })
  >
  requisitos;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    final cumplidos = requisitos.where((r) => r.cumplido).length;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '$cumplidos de ${requisitos.length} requisitos cumplidos',
          style: texto.titleMedium?.copyWith(color: t.text),
        ),
        const SizedBox(height: Espacio.s3),
        for (final r in requisitos)
          Semantics(
            label:
                '${r.nombre}: ${r.cumplido ? 'cumplido' : 'falta'}. Tu valor ${r.tuValor}, umbral ${r.umbral}. Código ${r.codigo}',
            excludeSemantics: true,
            child: Container(
              margin: const EdgeInsets.only(bottom: Espacio.s2),
              padding: const EdgeInsets.all(Espacio.s3),
              decoration: BoxDecoration(
                color: r.cumplido ? t.okBg : t.surface,
                borderRadius: BorderRadius.circular(Radios.md),
                border: Border.all(
                  color: r.cumplido ? t.ok : t.border,
                  width: Borde.fino,
                ),
              ),
              child: Row(
                children: [
                  Icon(
                    r.cumplido
                        ? Icons.check_circle
                        : Icons.radio_button_unchecked,
                    color: r.cumplido ? t.okTexto : t.text3,
                  ),
                  const SizedBox(width: Espacio.s3),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          r.nombre,
                          style: texto.bodyLarge?.copyWith(color: t.text),
                        ),
                        Text(
                          'Tenés ${r.tuValor} · se pide ${r.umbral}',
                          style: texto.bodySmall?.copyWith(color: t.text2),
                        ),
                        Text(
                          r.codigo,
                          style: texto.labelSmall?.copyWith(
                            color: t.text3,
                            fontFamily: Fuente.display,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }
}
