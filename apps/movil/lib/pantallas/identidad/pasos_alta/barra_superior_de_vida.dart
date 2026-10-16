import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../textos_de_captura.dart';

class BarraSuperiorDeVida extends StatelessWidget {
  const BarraSuperiorDeVida({
    super.key,
    required this.indice,
    required this.total,
    required this.titulo,
    required this.instruccion,
    required this.onSalir,
  });

  final int indice;
  final int total;
  final String titulo;
  final String instruccion;
  final VoidCallback onSalir;

  @override
  Widget build(BuildContext context) => RepaintBoundary(
    child: Padding(
      padding: const EdgeInsets.fromLTRB(Espacio.s3, Espacio.s2, Espacio.s3, 0),
      child: Row(
        children: [
          IconButton(
            tooltip: 'Salir de la prueba de vida',
            onPressed: onSalir,
            icon: const Icon(Icons.close_rounded, color: Paleta.white),
          ),
          Expanded(
            child: Semantics(
              liveRegion: true,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    TextosDeCaptura.fotoDeVida(indice, total),
                    style: Tipo.etiqueta.copyWith(color: Paleta.white),
                  ),
                  Text(
                    titulo,
                    style: Tipo.titulo2.copyWith(color: Paleta.white),
                  ),
                  Text(
                    instruccion,
                    style: Tipo.ayuda.copyWith(
                      color: Paleta.white.withValues(alpha: 0.8),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    ),
  );
}
