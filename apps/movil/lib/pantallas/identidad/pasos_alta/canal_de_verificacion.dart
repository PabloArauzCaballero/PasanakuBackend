import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../textos.dart';

/// Muestra el canal de verificación habilitado para el alta: correo.
class CanalDeVerificacion extends StatelessWidget {
  const CanalDeVerificacion({
    super.key,
    required this.canales,
    required this.valor,
    required this.onElegido,
  });

  final List<String> canales;
  final String valor;
  final ValueChanged<String> onElegido;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    final opciones = <String, String>{
      if (canales.contains('SMS')) 'SMS': TextosIdentidad.canalSms,
      if (canales.contains('CORREO')) 'CORREO': TextosIdentidad.canalCorreo,
    };
    final canalUnico = canales.length == 1 ? canales.first : null;
    final mensajeUnico = switch (canalUnico) {
      'CORREO' => 'Te mandamos el código a tu correo.',
      'SMS' => 'Te mandamos el código por SMS.',
      _ => 'No hay un canal de verificación disponible.',
    };
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (opciones.length < 2)
          Text(mensajeUnico, style: Tipo.ayuda.copyWith(color: t.text2))
        else ...[
          Text(
            TextosIdentidad.canalVerificacion,
            style: texto.labelLarge?.copyWith(color: t.text2),
          ),
          const SizedBox(height: Espacio.s2),
          SelectorSegmentado<String>(
            valor: valor,
            opciones: opciones,
            onChanged: onElegido,
          ),
          const SizedBox(height: Espacio.s2),
          Text(
            TextosIdentidad.canalAyuda,
            style: Tipo.ayuda.copyWith(color: t.text3),
          ),
        ],
      ],
    );
  }
}
