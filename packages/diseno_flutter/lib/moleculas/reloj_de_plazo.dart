import 'package:flutter/material.dart';

import '../atomos/chip_estado.dart';
import '../atomos/fecha.dart';
import '../atomos/tono.dart';
import '../tokens/tokens.dart';

/// Un plazo **guardado** (no recalculado), con lo que falta y de qué norma sale.
class RelojDePlazo extends StatelessWidget {
  const RelojDePlazo({
    super.key,
    required this.etiqueta,
    required this.venceIso,
    required this.ahora,
    this.norma,
  });
  final String etiqueta;
  final String venceIso;

  /// Se pasa desde afuera para que la prueba y el golden sean deterministas.
  final DateTime ahora;
  final String? norma;

  static String restante(DateTime vence, DateTime ahora) {
    final d = vence.difference(ahora);
    if (d.isNegative) {
      return 'Vencido';
    }
    if (d.inDays >= 1) {
      return 'Faltan ${d.inDays} ${d.inDays == 1 ? 'día' : 'días'}';
    }
    if (d.inHours >= 1) {
      return 'Faltan ${d.inHours} h';
    }
    return 'Vence hoy';
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    final vence = DateTime.parse(venceIso);
    final resto = restante(vence, ahora);
    final urgente = vence.difference(ahora).inHours < 24;
    final tono = vence.isBefore(ahora)
        ? Tono.error
        : urgente
        ? Tono.aviso
        : Tono.info;
    return Container(
      padding: const EdgeInsets.all(Espacio.s3),
      decoration: BoxDecoration(
        color: t.surface,
        borderRadius: BorderRadius.circular(Radios.md),
        border: Border.all(color: t.border, width: Borde.fino),
      ),
      child: Row(
        children: [
          Icon(Icons.schedule, color: tono.coloresDe(t).$2),
          const SizedBox(width: Espacio.s3),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  etiqueta,
                  style: texto.labelLarge?.copyWith(color: t.text2),
                ),
                Text(
                  '${Fecha.formatear(venceIso, conHora: false)} · $resto',
                  style: texto.bodyMedium?.copyWith(color: t.text),
                ),
                if (norma != null)
                  Text(
                    norma!,
                    style: texto.bodySmall?.copyWith(color: t.text3),
                  ),
              ],
            ),
          ),
          ChipEstado(texto: resto, tono: tono),
        ],
      ),
    );
  }
}
