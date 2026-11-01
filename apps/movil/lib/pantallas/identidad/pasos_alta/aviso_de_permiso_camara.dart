import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../textos.dart';
import '../textos_de_captura.dart';

/// Lo que se ve en el visor cuando el permiso de cámara no está concedido:
/// "Necesitamos tu cámara" + "Permitir cámara" si se puede volver a pedir, o el
/// aviso de ajustes si quedó bloqueado para siempre — igual que Atlas.
class AvisoDePermisoCamara extends StatelessWidget {
  const AvisoDePermisoCamara({
    super.key,
    required this.bloqueadoParaSiempre,
    required this.onAccion,
    this.onElegirDeLasFotos,
    this.soloPruebaDeVida = false,
  });

  final bool bloqueadoParaSiempre;
  final VoidCallback onAccion;
  final VoidCallback? onElegirDeLasFotos;
  final bool soloPruebaDeVida;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(Espacio.s5),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              bloqueadoParaSiempre && !soloPruebaDeVida
                  ? TextosDeCaptura.permisoBloqueado
                  : TextosDeCaptura.necesitamosTuCamara,
              textAlign: TextAlign.center,
              style: Tipo.titulo2.copyWith(color: Paleta.white),
            ),
            if (!soloPruebaDeVida && !bloqueadoParaSiempre) ...[
              const SizedBox(height: Espacio.s2),
              Text(
                onElegirDeLasFotos == null
                    ? TextosDeCaptura.soloParaTuCarnet
                    : '${TextosDeCaptura.soloParaTuCarnet} '
                          '${TextosDeCaptura.oDeLaGaleria}',
                textAlign: TextAlign.center,
                style: Tipo.cuerpoChico.copyWith(color: Paleta.white),
              ),
            ],
            if (soloPruebaDeVida) ...[
              const SizedBox(height: Espacio.s2),
              Text(
                TextosDeCaptura.soloParaTuPrueba,
                textAlign: TextAlign.center,
                style: Tipo.cuerpoChico.copyWith(color: Paleta.white),
              ),
              const SizedBox(height: Espacio.s2),
              Text(
                TextosDeCaptura.privacidadDeVida,
                textAlign: TextAlign.center,
                style: Tipo.ayuda.copyWith(
                  color: Paleta.white.withValues(alpha: 0.8),
                ),
              ),
            ],
            if (bloqueadoParaSiempre && soloPruebaDeVida) ...[
              const SizedBox(height: Espacio.s3),
              Text(
                TextosDeCaptura.permisoBloqueado,
                textAlign: TextAlign.center,
                style: Tipo.cuerpoChico.copyWith(color: Paleta.white),
              ),
            ],
            const SizedBox(height: Espacio.s4),
            Boton(
              texto: bloqueadoParaSiempre
                  ? TextosDeCaptura.abrirAjustes
                  : TextosDeCaptura.permitirCamara,
              variante: BotonVariante.primario,
              onPressed: onAccion,
            ),
            if (!soloPruebaDeVida && onElegirDeLasFotos != null) ...[
              const SizedBox(height: Espacio.s2),
              Boton(
                texto: TextosIdentidad.elegirDeLasFotos,
                variante: BotonVariante.fantasma,
                onPressed: onElegirDeLasFotos,
              ),
            ],
          ],
        ),
      ),
    );
  }
}
