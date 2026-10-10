import 'package:aportaya_diseno/moleculas/acordeon.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../dominio/capturas_del_expediente.dart';
import '../textos_de_captura.dart';

/// El panel plegable de consejos sobre el visor, comportamiento de Atlas: cerrado
/// por omisión, con un consejo por línea y su ícono de correcto/incorrecto.
///
/// Reutiliza `Acordeon` del sistema de diseño en vez de dibujos a mano: Atlas trae
/// una ilustración por consejo; acá se prioriza terminar el comportamiento completo
/// (plegado, contenido por tipo de cara) con el tiempo disponible, y se deja
/// declarado que el arte bespoke queda pendiente.
class ConsejosParaLaFoto extends StatelessWidget {
  const ConsejosParaLaFoto({super.key, required this.cara});
  final CaraDelCarril cara;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final consejos = cara.esDocumento ? _consejosCarnet : _consejosRostro;
    return Material(
      color: t.surface.withValues(alpha: 0.92),
      borderRadius: BorderRadius.circular(Radios.md),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: Espacio.s3),
        child: Acordeon(
          titulo: TextosDeCaptura.consejosTitulo,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [for (final c in consejos) _FilaDeConsejo(texto: c)],
          ),
        ),
      ),
    );
  }

  static const _consejosCarnet = [
    'Buena luz, sin sombras sobre el carnet.',
    'Sin reflejos — no uses la linterna.',
    'Imagen nítida: mantené el teléfono quieto.',
    'Los cuatro bordes del carnet, de frente.',
  ];

  static const _consejosRostro = [
    'De frente y centrado en el encuadre.',
    'Sin lentes, gorro ni barbijo.',
    'Luz de frente, no a contraluz.',
  ];
}

class _FilaDeConsejo extends StatelessWidget {
  const _FilaDeConsejo({required this.texto});
  final String texto;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: Espacio.s1),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.check_circle, size: Espacio.s4, color: t.brandTexto),
          const SizedBox(width: Espacio.s2),
          Expanded(
            child: Text(texto, style: Theme.of(context).textTheme.bodySmall),
          ),
        ],
      ),
    );
  }
}
