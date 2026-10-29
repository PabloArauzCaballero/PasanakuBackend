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
/// Van en una fila de tres columnas (2026-10-29): a 360 dp las tres pastillas en
/// `Wrap` caían una por renglón y parecían tres botones apilados, y empujaban fuera
/// de la vista la lista de lo que se pide. Cada columna es `Expanded` y su texto
/// baja de renglón en vez de desbordarse, así que con el texto en 200 % se sigue
/// leyendo.
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
  // `IntrinsicHeight`: las tres tarjetas miden lo mismo aunque una baje a dos
  // renglones (son tres hijos; el costo de medir dos veces es despreciable).
  Widget build(BuildContext context) => IntrinsicHeight(
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (final (i, g) in _garantias.indexed) ...[
          if (i > 0) const SizedBox(width: Espacio.s2),
          Expanded(
            child: _Garantia(icono: g.icono, texto: g.texto),
          ),
        ],
      ],
    ),
  );
}

class _Garantia extends StatelessWidget {
  const _Garantia({required this.icono, required this.texto});

  final IconData icono;
  final String texto;

  // Roles neutros (no de estado ni de marca): son contexto, no una acción.
  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return DecoratedBox(
      decoration: BoxDecoration(
        color: t.surface2,
        borderRadius: BorderRadius.circular(Radios.md),
        border: Border.all(color: t.border, width: Borde.fino),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: Espacio.s1,
          vertical: Espacio.s2,
        ),
        child: Column(
          children: [
            Icon(icono, size: Espacio.s5, color: t.text2),
            const SizedBox(height: Espacio.s1),
            Text(
              texto,
              textAlign: TextAlign.center,
              style: Tipo.cuerpoChico.copyWith(color: t.text2),
            ),
          ],
        ),
      ),
    );
  }
}
