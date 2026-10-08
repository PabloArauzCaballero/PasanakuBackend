import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/foundation.dart' show kReleaseMode;
import 'package:flutter/material.dart';

import '../dominio/capturas_del_expediente.dart';
import '../textos_de_captura.dart';

/// El pie del paso de capturas: «Enviar documento», el motivo de qué falta mientras
/// está apagado, y el atajo del carnet de prueba (solo fuera de release).
class PieDeCapturas extends StatelessWidget {
  const PieDeCapturas({
    super.key,
    required this.capturas,
    required this.onEnviar,
    required this.onUsarCarnetDePrueba,
  });

  final CapturasDelExpediente capturas;
  final VoidCallback onEnviar;
  final VoidCallback onUsarCarnetDePrueba;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Boton(
          texto: TextosDeCaptura.enviarDocumento,
          variante: BotonVariante.secundario,
          expandido: true,
          onPressed: capturas.completo ? onEnviar : null,
        ),
        // El motivo va debajo del botón apagado, como en Atlas: dice qué falta sin
        // alarmar — no es un error, es lo que queda por hacer.
        if (!capturas.completo) ...[
          const SizedBox(height: Espacio.s2),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                Icons.info_outline_rounded,
                size: Espacio.s4,
                color: t.text3,
              ),
              const SizedBox(width: Espacio.s1 + Borde.desfase),
              Flexible(
                child: Text(
                  capturas.primeraFaltante!,
                  style: Tipo.cuerpoChico.copyWith(color: t.text2),
                ),
              ),
            ],
          ),
        ],
        // Solo en desarrollo, y con el peso de un enlace: no compite con la acción.
        if (!kReleaseMode && !capturas.completo) ...[
          const SizedBox(height: Espacio.s3),
          Center(
            child: Boton(
              texto: TextosDeCaptura.usarCarnetDePrueba,
              icono: Icons.science_outlined,
              variante: BotonVariante.enlace,
              onPressed: onUsarCarnetDePrueba,
            ),
          ),
        ],
      ],
    );
  }
}
