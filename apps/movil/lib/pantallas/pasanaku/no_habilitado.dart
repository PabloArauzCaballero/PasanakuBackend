import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// Piezas de `PantallaCrearGrupo` separadas en su propio archivo (arquitectura
/// atómica: la pantalla queda como composición, no como un formulario de 250+
/// líneas en un solo archivo).
class NoHabilitado extends StatelessWidget {
  const NoHabilitado({super.key, required this.nivel});
  final String nivel;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            'Todavía no podés organizar un grupo',
            style: Tipo.titulo2.copyWith(color: t.text),
          ),
          const SizedBox(height: Espacio.s2),
          Text(
            'Tu nivel actual es $nivel. ${TextosPasanaku.capacitacionVencidaAviso}',
            style: Tipo.cuerpo.copyWith(color: t.text2),
          ),
          const SizedBox(height: Espacio.s2),
          Text(
            TextosPasanaku.requisitosAyuda,
            style: Tipo.ayuda.copyWith(color: t.text3),
          ),
        ],
      ),
    );
  }
}
