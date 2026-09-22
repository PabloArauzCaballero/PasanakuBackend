import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'celda_de_cuota.dart';
import 'estado_de_cuota.dart';

/// La leyenda se pinta **con las mismas reglas que la celda** (relleno y borde), e
/// incluye la marca de *hoy*. Un cuadrado sólido explicaría un calendario que no es
/// el que está arriba (D-12).
class LeyendaDeCuotas extends StatelessWidget {
  const LeyendaDeCuotas({super.key, required this.cuenta});
  final Map<EstadoDeCuota, int> cuenta;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    Widget muestra(EstadoDeCuota e, String nombre) {
      final (relleno, borde) = CeldaDeCuota.coloresDe(e, t);
      return Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: Espacio.s4,
            height: Espacio.s4,
            decoration: BoxDecoration(
              color: relleno,
              borderRadius: BorderRadius.circular(Radios.sm),
              border: Border.all(
                color: borde,
                width: e == EstadoDeCuota.futura
                    ? Borde.fino
                    : Borde.foco - Borde.fino,
              ),
            ),
          ),
          const SizedBox(width: Espacio.s1),
          Text(
            '$nombre (${cuenta[e] ?? 0})',
            style: texto.labelSmall?.copyWith(color: t.text2),
          ),
        ],
      );
    }

    return Wrap(
      spacing: Espacio.s3,
      runSpacing: Espacio.s1,
      children: [
        muestra(EstadoDeCuota.pagada, 'Pagada'),
        muestra(EstadoDeCuota.pendiente, 'Pendiente'),
        muestra(EstadoDeCuota.vencida, 'Vencida'),
        muestra(EstadoDeCuota.futura, 'Todavía no se abre'),
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: Espacio.s4,
              height: Espacio.s4,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(Radios.sm),
                color: t.surface,
                boxShadow: [
                  BoxShadow(
                    color: t.brand,
                    spreadRadius: Borde.foco - Borde.fino,
                    blurRadius: 0,
                  ),
                ],
              ),
            ),
            const SizedBox(width: Espacio.s1),
            Text('Hoy', style: texto.labelSmall?.copyWith(color: t.text2)),
          ],
        ),
      ],
    );
  }
}
