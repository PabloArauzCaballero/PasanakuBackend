import 'package:flutter/material.dart';

import '../atomos/boton_icono.dart';
import '../tokens/tokens.dart';

/// Avisos que llegan juntos **se encolan**: uno a la vez, con el contador de los que
/// esperan; tocarlo abre la bandeja y limpia la cola (D-11).
class NotificacionEmergente extends StatelessWidget {
  const NotificacionEmergente({
    super.key,
    required this.titulo,
    required this.detalle,
    required this.enEspera,
    required this.onAbrir,
    required this.onCerrar,
  });
  final String titulo;
  final String detalle;
  final int enEspera;
  final VoidCallback onAbrir;
  final VoidCallback onCerrar;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Semantics(
      liveRegion: true,
      button: true,
      label:
          '$titulo. $detalle${enEspera > 0 ? '. $enEspera más esperan' : ''}',
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onAbrir,
          borderRadius: BorderRadius.circular(Radios.lg),
          child: Container(
            padding: const EdgeInsets.all(Espacio.s3),
            decoration: BoxDecoration(
              color: t.surface,
              borderRadius: BorderRadius.circular(Radios.lg),
              border: Border.all(color: t.border, width: Borde.fino),
              boxShadow: [t.sombra2],
            ),
            child: Row(
              children: [
                Icon(Icons.notifications_active_outlined, color: t.brandTexto),
                const SizedBox(width: Espacio.s3),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        titulo,
                        style: texto.titleSmall?.copyWith(color: t.text),
                      ),
                      Text(
                        detalle,
                        style: texto.bodySmall?.copyWith(color: t.text2),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
                if (enEspera > 0)
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: Espacio.s2),
                    child: Text(
                      '+$enEspera',
                      style: texto.labelMedium?.copyWith(color: t.text3),
                    ),
                  ),
                BotonIcono(
                  icono: Icons.close,
                  etiqueta: 'Cerrar aviso',
                  onPressed: onCerrar,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
