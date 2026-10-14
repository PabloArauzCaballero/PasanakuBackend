import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../textos_de_captura.dart';

/// El pie de la prueba de vida: qué hacer ahora (lo que dice el juez de pose), una
/// barra que se llena mientras la pose se sostiene, y «Tomar foto» solo como
/// respaldo cuando la captura automática no anda.
class PieDePruebaDeVida extends StatelessWidget {
  const PieDePruebaDeVida({
    super.key,
    required this.pista,
    required this.progreso,
    required this.rechazo,
    required this.manual,
    required this.tomando,
    required this.tomada,
    required this.finalizada,
    required this.onTomarFoto,
    required this.onContinuar,
    required this.onCancelar,
  });

  /// `null` mientras la cámara no está lista.
  final String? pista;
  final double progreso;
  final String? rechazo;
  final bool manual;
  final bool tomando;
  final bool tomada;
  final bool finalizada;
  final VoidCallback onTomarFoto;
  final VoidCallback onContinuar;
  final VoidCallback onCancelar;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.all(Espacio.s4),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (finalizada) ...[
          Semantics(
            liveRegion: true,
            child: const Text(
              TextosDeCaptura.vidaLista,
              textAlign: TextAlign.center,
              style: TextStyle(color: Paleta.white),
            ),
          ),
          const SizedBox(height: Espacio.s3),
          Boton(
            texto: TextosDeCaptura.continuar,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: onContinuar,
          ),
        ] else if (tomada) ...[
          Semantics(
            liveRegion: true,
            child: const Column(
              children: [
                Text(
                  TextosDeCaptura.fotoTomada,
                  style: TextStyle(color: Paleta.white),
                ),
                Text(
                  TextosDeCaptura.seguirSiguiente,
                  style: TextStyle(color: Paleta.white),
                ),
              ],
            ),
          ),
        ] else if (pista != null) ...[
          // Se anuncia a medida que cambia: quien usa lector de pantalla también
          // tiene que saber hacia dónde girar.
          Semantics(
            liveRegion: true,
            child: Text(
              pista!,
              textAlign: TextAlign.center,
              style: Tipo.cuerpoFuerte.copyWith(color: Paleta.white),
            ),
          ),
          const SizedBox(height: Espacio.s2),
          ClipRRect(
            borderRadius: BorderRadius.circular(Radios.pill),
            child: LinearProgressIndicator(
              value: progreso,
              minHeight: Espacio.s1,
              backgroundColor: Paleta.white.withValues(alpha: 0.2),
              color: Tokens.of(context).brand,
            ),
          ),
          const SizedBox(height: Espacio.s2),
        ],
        if (rechazo != null) ...[
          Alerta(titulo: rechazo!, tono: Tono.aviso),
          const SizedBox(height: Espacio.s3),
        ],
        if (!finalizada && !tomada) ...[
          Boton(
            texto: TextosDeCaptura.tomarAhora,
            variante: BotonVariante.fantasma,
            expandido: true,
            cargando: tomando,
            onPressed: !manual || tomando ? null : onTomarFoto,
          ),
          const SizedBox(height: Espacio.s2),
        ],
        Boton(
          texto: TextosDeCaptura.cancelar,
          variante: BotonVariante.enlace,
          expandido: true,
          onPressed: onCancelar,
        ),
      ],
    ),
  );
}
