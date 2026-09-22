import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// El veredicto con los factores que lo produjeron y sus umbrales (D-20). Cuando
/// bloquea, el motivo es sobre la exposición del grupo, no sobre la persona.
class PanelDeFactores extends StatelessWidget {
  const PanelDeFactores({
    super.key,
    required this.veredicto,
    required this.aprobado,
    required this.factores,
    this.motivo,
  });
  final String veredicto;
  final bool aprobado;
  final List<({String nombre, String valor, String umbral, bool pasa})>
  factores;
  final String? motivo;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Container(
      padding: const EdgeInsets.all(Espacio.s4),
      decoration: BoxDecoration(
        color: aprobado ? t.okBg : t.errBg,
        borderRadius: BorderRadius.circular(Radios.lg),
        border: Border.all(color: aprobado ? t.ok : t.err, width: Borde.fino),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                aprobado ? Icons.verified_outlined : Icons.gpp_maybe_outlined,
                color: aprobado ? t.okTexto : t.errTexto,
              ),
              const SizedBox(width: Espacio.s2),
              Expanded(
                child: Text(
                  veredicto,
                  style: texto.titleMedium?.copyWith(color: t.text),
                ),
              ),
            ],
          ),
          if (motivo != null)
            Padding(
              padding: const EdgeInsets.only(top: Espacio.s1),
              child: Text(
                motivo!,
                style: texto.bodyMedium?.copyWith(color: t.text2),
              ),
            ),
          const SizedBox(height: Espacio.s3),
          for (final f in factores)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: Espacio.s1),
              child: Row(
                children: [
                  Icon(
                    f.pasa ? Icons.check : Icons.close,
                    size: Espacio.s4,
                    color: f.pasa ? t.okTexto : t.errTexto,
                  ),
                  const SizedBox(width: Espacio.s2),
                  Expanded(
                    child: Text(
                      f.nombre,
                      style: texto.bodyMedium?.copyWith(color: t.text),
                    ),
                  ),
                  Text(
                    '${f.valor} / ${f.umbral}',
                    style: texto.bodySmall?.copyWith(
                      color: t.text2,
                      fontFamily: Fuente.display,
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}
