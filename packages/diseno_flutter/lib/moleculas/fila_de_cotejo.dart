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

  bool get pendiente => declarado.trim().isEmpty || leido.trim().isEmpty;

  bool get coincide =>
      !pendiente &&
      declarado.trim().toLowerCase() == leido.trim().toLowerCase();

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    final estaPendiente = pendiente;
    final estaCotejada = coincide;
    final colorDeEstado = estaPendiente
        ? t.text2
        : estaCotejada
        ? t.okTexto
        : t.avisoTexto;
    return Semantics(
      label:
          '$campo: ${switch ((estaPendiente, estaCotejada)) {
            (true, _) => 'pendiente de completar',
            (_, true) => 'coincide',
            _ => 'no coincide. Declaraste $declarado, el documento dice $leido',
          }}',
      child: Container(
        padding: const EdgeInsets.all(Espacio.s3),
        decoration: BoxDecoration(
          color: estaCotejada || estaPendiente ? t.surface : t.warnBg,
          borderRadius: BorderRadius.circular(Radios.md),
          border: Border.all(
            color: estaCotejada || estaPendiente ? t.border : t.warn,
            width: Borde.fino,
          ),
        ),
        child: Row(
          children: [
            Icon(
              estaPendiente
                  ? Icons.radio_button_unchecked
                  : estaCotejada
                  ? Icons.check_circle_outline
                  : Icons.difference_outlined,
              color: colorDeEstado,
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
                  Text(switch ((estaPendiente, estaCotejada)) {
                    (true, _) => 'Pendiente de completar',
                    (_, true) => declarado,
                    _ => 'Declaraste: $declarado',
                  }, style: texto.bodyMedium?.copyWith(color: t.text)),
                  if (!estaPendiente && !estaCotejada)
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
