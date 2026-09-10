import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Declarado contra leído, campo por campo, con la diferencia marcada y editable.
class FilaDeCotejo extends StatelessWidget {
  const FilaDeCotejo({
    super.key,
    required this.campo,
    required this.declarado,
    required this.leido,
    this.onCorregir,
  });
  final String campo;
  final String declarado;
  final String leido;
  final VoidCallback? onCorregir;

  bool get coincide =>
      declarado.trim().toLowerCase() == leido.trim().toLowerCase();

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Semantics(
      label:
          '$campo: ${coincide ? 'coincide' : 'no coincide. Declaraste $declarado, el documento dice $leido'}',
      child: Container(
        padding: const EdgeInsets.all(Espacio.s3),
        decoration: BoxDecoration(
          color: coincide ? t.surface : t.warnBg,
          borderRadius: BorderRadius.circular(Radios.md),
          border: Border.all(
            color: coincide ? t.border : t.warn,
            width: Borde.fino,
          ),
        ),
        child: Row(
          children: [
            Icon(
              coincide ? Icons.check_circle_outline : Icons.difference_outlined,
              color: coincide ? t.okTexto : t.avisoTexto,
            ),
            const SizedBox(width: Espacio.s3),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    campo,
                    style: texto.labelLarge?.copyWith(color: t.text2),
                  ),
                  Text(
                    coincide ? declarado : 'Declaraste: $declarado',
                    style: texto.bodyMedium?.copyWith(color: t.text),
                  ),
                  if (!coincide)
                    Text(
                      'El documento dice: $leido',
                      style: texto.bodyMedium?.copyWith(
                        color: t.text,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                ],
              ),
            ),
            if (!coincide && onCorregir != null)
              TextButton(onPressed: onCorregir, child: const Text('Corregir')),
          ],
        ),
      ),
    );
  }
}
