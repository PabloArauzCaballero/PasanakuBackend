import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../textos.dart';

/// **Por dónde llega la verificación.** No son dos formas de lo mismo: el SMS
/// confirma el celular y el correo confirma el correo, con propósitos distintos en el
/// catálogo de tokens. Lo que se elige acá es cuál de los dos contactos se confirma
/// primero; el otro queda para después, desde el perfil.
class CanalDeVerificacion extends StatelessWidget {
  const CanalDeVerificacion({
    super.key,
    required this.valor,
    required this.onElegido,
  });

  final String valor;
  final ValueChanged<String> onElegido;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          TextosIdentidad.canalVerificacion,
          style: texto.labelLarge?.copyWith(color: t.text2),
        ),
        const SizedBox(height: Espacio.s2),
        SelectorSegmentado<String>(
          valor: valor,
          opciones: const {
            'SMS': TextosIdentidad.canalSms,
            'CORREO': TextosIdentidad.canalCorreo,
          },
          onChanged: onElegido,
        ),
        const SizedBox(height: Espacio.s2),
        Text(
          TextosIdentidad.canalAyuda,
          style: Tipo.ayuda.copyWith(color: t.text3),
        ),
      ],
    );
  }
}
