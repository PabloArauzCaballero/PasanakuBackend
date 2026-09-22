import 'package:aportaya_diseno/atomos/progreso.dart';
import 'package:aportaya_diseno/moleculas/seccion.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// Cuánto llevás hecho, arriba de todo del centro de ayuda.
///
/// Dice el número además de pintar la barra: una barra sola no se puede leer en voz
/// alta ni comparar de un día para otro.
class ResumenDeAvance extends StatelessWidget {
  const ResumenDeAvance({
    super.key,
    required this.hechos,
    required this.total,
    required this.fraccion,
  });

  final int hechos;
  final int total;
  final double fraccion;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Panel(
      destacado: true,
      hijo: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            TextosSoporte.avanceTitulo,
            style: Tipo.titulo3.copyWith(color: t.brandTexto),
          ),
          const SizedBox(height: Espacio.s1),
          Text(
            TextosSoporte.avanceDe(hechos, total),
            style: Tipo.cuerpo.copyWith(color: t.text2),
          ),
          const SizedBox(height: Espacio.s2),
          Progreso(valor: fraccion, etiqueta: TextosSoporte.avanceEtiqueta),
        ],
      ),
    );
  }
}
