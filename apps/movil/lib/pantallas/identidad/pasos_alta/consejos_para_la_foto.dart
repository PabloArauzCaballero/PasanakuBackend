import 'package:aportaya_diseno/moleculas/acordeon.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../dominio/capturas_del_expediente.dart';
import '../textos_de_captura.dart';

/// El panel plegable de consejos sobre el visor, comportamiento de Atlas: cerrado
/// por omisión, con un consejo por línea y un ícono que nombra de qué se trata
/// (luz, reflejo, nitidez, encuadre). Los ejemplos dibujados viven en la pantalla
/// «Pautas para tus fotos».
class ConsejosParaLaFoto extends StatelessWidget {
  const ConsejosParaLaFoto({
    super.key,
    required this.cara,
    this.abierto = false,
  });
  final CaraDelCarril cara;
  final bool abierto;

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
          abierto: abierto,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              for (final (icono, texto) in consejos)
                _FilaDeConsejo(icono: icono, texto: texto),
            ],
          ),
        ),
      ),
    );
  }

  static const _consejosCarnet = [
    (Icons.wb_sunny_outlined, 'Buena luz, sin sombras sobre el carnet.'),
    (Icons.flash_off, 'Sin reflejos — no uses la linterna.'),
    (Icons.center_focus_strong, 'Imagen nítida: mantené el teléfono quieto.'),
    (Icons.crop_free, 'Los cuatro bordes del carnet, de frente.'),
  ];

  static const _consejosRostro = [
    (Icons.face_outlined, 'De frente y centrado en el encuadre.'),
    (Icons.face_retouching_off, 'Sin lentes, gorro ni barbijo.'),
    (Icons.wb_sunny_outlined, 'Luz de frente, no a contraluz.'),
  ];
}

class _FilaDeConsejo extends StatelessWidget {
  const _FilaDeConsejo({required this.icono, required this.texto});
  final IconData icono;
  final String texto;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: Espacio.s1),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icono, size: Espacio.s5, color: t.text2),
          const SizedBox(width: Espacio.s2),
          Expanded(
            child: Text(texto, style: Theme.of(context).textTheme.bodySmall),
          ),
        ],
      ),
    );
  }
}
