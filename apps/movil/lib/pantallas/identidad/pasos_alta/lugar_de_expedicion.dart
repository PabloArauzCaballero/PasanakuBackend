import 'package:aportaya_diseno/atomos/chip_elegible.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../textos.dart';

/// **La extensión del carnet**: el departamento que lo emitió.
///
/// El número de cédula boliviana no es único por sí solo —el mismo número existe en
/// La Paz y en Santa Cruz, y son dos personas distintas—, así que sin esto la segunda
/// chocaba contra la unicidad del documento y no podía abrir cuenta.
///
/// Va en chips y no en un desplegable: son nueve opciones fijas y cortas, todas
/// caben a la vista, y elegir una es un toque en vez de tres.
class LugarDeExpedicion extends StatelessWidget {
  const LugarDeExpedicion({
    super.key,
    required this.valor,
    required this.onElegido,
    this.error,
  });

  final String? valor;
  final ValueChanged<String> onElegido;
  final String? error;

  /// Los nueve departamentos, con la sigla que lleva el carnet.
  static const departamentos = <({String sigla, String nombre})>[
    (sigla: 'LP', nombre: 'La Paz'),
    (sigla: 'SC', nombre: 'Santa Cruz'),
    (sigla: 'CB', nombre: 'Cochabamba'),
    (sigla: 'OR', nombre: 'Oruro'),
    (sigla: 'PT', nombre: 'Potosí'),
    (sigla: 'CH', nombre: 'Chuquisaca'),
    (sigla: 'TJ', nombre: 'Tarija'),
    (sigla: 'BE', nombre: 'Beni'),
    (sigla: 'PD', nombre: 'Pando'),
  ];

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          TextosIdentidad.lugarExpedicion,
          style: texto.labelLarge?.copyWith(color: t.text2),
        ),
        const SizedBox(height: Espacio.s2),
        Wrap(
          spacing: Espacio.s2,
          runSpacing: Espacio.s2,
          children: [
            for (final d in departamentos)
              ChipElegible(
                texto: d.nombre,
                seleccionado: valor == d.sigla,
                onTap: () => onElegido(d.sigla),
              ),
          ],
        ),
        const SizedBox(height: Espacio.s2),
        Text(
          error ?? TextosIdentidad.lugarExpedicionAyuda,
          style: texto.bodySmall?.copyWith(
            color: error == null ? t.text3 : t.errTexto,
          ),
        ),
      ],
    );
  }
}
