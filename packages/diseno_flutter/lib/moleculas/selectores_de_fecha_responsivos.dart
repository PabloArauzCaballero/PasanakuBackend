import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'parte_de_fecha.dart';

/// Presenta Día, Mes y Año en fila o apilados según el ancho disponible.
class SelectoresDeFechaResponsivos extends StatelessWidget {
  const SelectoresDeFechaResponsivos({
    super.key,
    required this.dia,
    required this.mes,
    required this.anio,
    required this.primerAnio,
    required this.ultimoAnio,
    required this.marca,
    required this.completa,
    required this.focoDia,
    required this.ejemplo,
    required this.onDia,
    required this.onMes,
    required this.onAnio,
  });

  final int? dia, mes, anio;
  final int primerAnio, ultimoAnio;
  final String? marca;
  final bool completa;
  final FocusNode? focoDia;
  final String ejemplo;
  final ValueChanged<int> onDia, onMes, onAnio;

  @override
  Widget build(BuildContext context) => LayoutBuilder(
    builder: (context, limites) {
      final enFila = limites.maxWidth >= 480;
      final selectorDia = ParteDeFecha(
        vacio: 'Día',
        flex: 5,
        expandir: enFila,
        valor: dia,
        marca: marca,
        completa: completa,
        foco: focoDia,
        ejemplo: ejemplo,
        opciones: [
          for (var d = 1; d <= 31; d++) (valor: d, texto: '$d', detalle: null),
        ],
        onElegida: onDia,
      );
      final selectorMes = ParteDeFecha(
        vacio: 'Mes',
        flex: 8,
        expandir: enFila,
        valor: mes,
        marca: marca,
        completa: completa,
        numerico: false,
        ejemplo: ejemplo,
        opciones: [
          for (var m = 1; m <= 12; m++)
            (valor: m, texto: mesesDelAnio[m - 1], detalle: null),
        ],
        onElegida: onMes,
      );
      final selectorAnio = ParteDeFecha(
        vacio: 'Año',
        flex: 6,
        expandir: enFila,
        valor: anio,
        marca: marca,
        completa: completa,
        ejemplo: ejemplo,
        opciones: [
          for (var a = ultimoAnio; a >= primerAnio; a--)
            (valor: a, texto: '$a', detalle: null),
        ],
        onElegida: onAnio,
      );
      if (!enFila) {
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            selectorDia,
            const SizedBox(height: Espacio.s2),
            selectorMes,
            const SizedBox(height: Espacio.s2),
            selectorAnio,
          ],
        );
      }
      return Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          selectorDia,
          const SizedBox(width: Espacio.s2),
          selectorMes,
          const SizedBox(width: Espacio.s2),
          // Del más cercano al más lejano: el que se busca suele estar arriba.
          selectorAnio,
        ],
      );
    },
  );
}
