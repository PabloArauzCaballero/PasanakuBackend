import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../textos_de_captura.dart';

/// Lo que se ve en el visor cuando el permiso de cámara no está concedido:
/// "Necesitamos tu cámara" + "Permitir cámara" si se puede volver a pedir, o el
/// aviso de ajustes si quedó bloqueado para siempre — igual que Atlas.
class AvisoDePermisoCamara extends StatelessWidget {
  const AvisoDePermisoCamara({
    super.key,
    required this.bloqueadoParaSiempre,
    required this.onAccion,
  });

  final bool bloqueadoParaSiempre;
  final VoidCallback onAccion;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(Espacio.s5),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              bloqueadoParaSiempre
                  ? TextosDeCaptura.permisoBloqueado
                  : TextosDeCaptura.necesitamosTuCamara,
              textAlign: TextAlign.center,
              style: const TextStyle(color: Paleta.white),
            ),
            const SizedBox(height: Espacio.s4),
            Boton(
              texto: bloqueadoParaSiempre
                  ? TextosDeCaptura.abrirAjustes
                  : TextosDeCaptura.permitirCamara,
              variante: BotonVariante.primario,
              onPressed: onAccion,
            ),
          ],
        ),
      ),
    );
  }
}
