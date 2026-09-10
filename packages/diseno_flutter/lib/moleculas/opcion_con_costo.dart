import 'package:flutter/material.dart';

import '../atomos/tono.dart';
import '../tokens/tokens.dart';

/// Una salida y lo que cuesta elegirla, en la misma tarjeta (D-17). Ninguna se ofrece sin su costo.
class OpcionConCosto extends StatelessWidget {
  const OpcionConCosto({
    super.key,
    required this.titulo,
    required this.queHace,
    required this.costo,
    required this.onElegir,
    this.tono = Tono.neutro,
  });
  final String titulo;
  final String queHace;
  final String costo;
  final VoidCallback onElegir;
  final Tono tono;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Semantics(
      button: true,
      label: '$titulo. $queHace. Costo: $costo',
      child: InkWell(
        onTap: onElegir,
        borderRadius: BorderRadius.circular(Radios.lg),
        child: Container(
          padding: const EdgeInsets.all(Espacio.s4),
          decoration: BoxDecoration(
            color: t.surface,
            borderRadius: BorderRadius.circular(Radios.lg),
            border: Border.all(color: t.border, width: Borde.fino),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(titulo, style: texto.titleMedium?.copyWith(color: t.text)),
              const SizedBox(height: Espacio.s1),
              Text(queHace, style: texto.bodyMedium?.copyWith(color: t.text2)),
              const SizedBox(height: Espacio.s3),
              Row(
                children: [
                  Icon(
                    Icons.info_outline,
                    size: Espacio.s4,
                    color: tono.coloresDe(t).$2,
                  ),
                  const SizedBox(width: Espacio.s1),
                  Expanded(
                    child: Text(
                      costo,
                      style: texto.bodySmall?.copyWith(
                        color: tono.coloresDe(t).$2,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                  Icon(Icons.chevron_right, color: t.text3),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
