import 'package:aportaya_diseno/atomos/colores_de_turno.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos_detalle_del_grupo.dart';

/// El orden de cobro que salió del sorteo (2026-10-20): una fila por turno, con el
/// color de esa posición en la rueda del grupo.
class OrdenDeCobro extends StatelessWidget {
  const OrdenDeCobro({super.key, required this.nombres});

  /// Los nombres en el orden de cobro: el primero cobra primero.
  final List<String> nombres;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          TextosDetalleDelGrupo.ordenDeCobro,
          style: Tipo.titulo3.copyWith(color: t.text),
        ),
        const SizedBox(height: Espacio.s2),
        for (var k = 0; k < nombres.length; k++)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: Espacio.s1),
            child: Row(
              children: [
                Container(
                  width: Espacio.s3,
                  height: Espacio.s3,
                  decoration: BoxDecoration(
                    color: ColoresDeTurno.de(k),
                    shape: BoxShape.circle,
                  ),
                ),
                const SizedBox(width: Espacio.s2),
                Text(
                  TextosDetalleDelGrupo.posicion(k + 1, nombres[k]),
                  style: Tipo.cuerpo.copyWith(color: t.text),
                ),
              ],
            ),
          ),
      ],
    );
  }
}
