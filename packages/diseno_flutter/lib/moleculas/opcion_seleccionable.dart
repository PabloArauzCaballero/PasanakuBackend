import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Una opción excluyente como tarjeta elegible, no como radio de fábrica.
///
/// Un `RadioListTile` pone un círculo chiquito al borde de la pantalla y deja el resto
/// de la fila muerto; acá toda la tarjeta es el área de toque, la elegida se marca con
/// borde, fondo y check —tres señales, no solo color— y cabe una línea de detalle que
/// ayuda a decidir («12 al año»).
class OpcionSeleccionable extends StatelessWidget {
  const OpcionSeleccionable({
    super.key,
    required this.titulo,
    required this.elegida,
    required this.onTap,
    this.detalle,
  });

  final String titulo;
  final String? detalle;
  final bool elegida;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      inMutuallyExclusiveGroup: true,
      selected: elegida,
      button: true,
      label: detalle == null ? titulo : '$titulo, $detalle',
      excludeSemantics: true,
      child: Material(
        color: elegida ? t.brandBg : t.surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(Radios.md),
          side: BorderSide(
            color: elegida ? t.brand : t.fieldBorder,
            width: elegida ? Borde.desfase : Borde.fino,
          ),
        ),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(Radios.md),
          child: ConstrainedBox(
            constraints: const BoxConstraints(minHeight: Tactil.minimo + Espacio.s4),
            child: Padding(
              padding: const EdgeInsets.all(Espacio.s3),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          titulo,
                          style: Tipo.boton.copyWith(
                            color: elegida ? t.brandInk : t.text,
                          ),
                        ),
                        if (detalle != null) ...[
                          const SizedBox(height: Espacio.s1 / 2),
                          Text(
                            detalle!,
                            style: Tipo.ayuda.copyWith(color: t.text2),
                          ),
                        ],
                      ],
                    ),
                  ),
                  if (elegida)
                    Container(
                      width: Espacio.s5 - Espacio.s1,
                      height: Espacio.s5 - Espacio.s1,
                      decoration: BoxDecoration(
                        color: t.brand,
                        shape: BoxShape.circle,
                      ),
                      child: Icon(
                        Icons.check,
                        size: 14,
                        color: t.sobreVerdeSolido,
                      ),
                    ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
