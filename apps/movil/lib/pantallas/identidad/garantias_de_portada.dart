import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// Las tres garantías de la bienvenida, en fila.
///
/// Dicen en tres palabras lo que el tour cuenta en cuatro láminas, porque mucha gente
/// no va a abrir el tour: decide en la bienvenida si esto le merece la plata. No son
/// promesas nuevas —custodia, registro y turno a la vista son las láminas 2, 1 y 3—,
/// son las mismas, arriba.
///
/// Van en `Wrap` y no en `Row`: con el texto en 200 % una fila fija se sale de la
/// pantalla, y tres pastillas que bajan a dos renglones se siguen leyendo.
class GarantiasDePortada extends StatelessWidget {
  const GarantiasDePortada({super.key});

  static const _garantias = <({IconData icono, String texto})>[
    (icono: Icons.lock_outline, texto: TextosIdentidad.portadaGarantiaCustodia),
    (
      icono: Icons.receipt_long_outlined,
      texto: TextosIdentidad.portadaGarantiaRegistro,
    ),
    (
      icono: Icons.event_available_outlined,
      texto: TextosIdentidad.portadaGarantiaTurno,
    ),
  ];

  @override
  Widget build(BuildContext context) => Wrap(
    alignment: WrapAlignment.center,
    spacing: Espacio.s2,
    runSpacing: Espacio.s2,
    children: [
      for (final g in _garantias) _Pastilla(icono: g.icono, texto: g.texto),
    ],
  );
}

class _Pastilla extends StatelessWidget {
  const _Pastilla({required this.icono, required this.texto});

  final IconData icono;
  final String texto;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return DecoratedBox(
      decoration: BoxDecoration(
        color: t.brandBg,
        borderRadius: BorderRadius.circular(Radios.pill),
        border: Border.all(
          color: t.brand.withValues(alpha: 0.14),
          width: Borde.fino,
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: Espacio.s3,
          vertical: Espacio.s2,
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icono, size: Espacio.s4, color: t.brandTexto),
            const SizedBox(width: Espacio.s2),
            Text(texto, style: Tipo.cuerpoChico.copyWith(color: t.brandTexto)),
          ],
        ),
      ),
    );
  }
}
