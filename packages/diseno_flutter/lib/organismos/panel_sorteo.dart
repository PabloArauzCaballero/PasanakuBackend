import 'package:flutter/material.dart';

import '../atomos/boton.dart';
import '../atomos/boton_variante.dart';
import '../atomos/chip_estado.dart';
import '../atomos/tono.dart';
import '../tokens/tokens.dart';

/// El sorteo como evento guardado: los cinco pasos con su hash, y la comparación
/// entre el orden reproducido y el guardado (D-5).
class PanelSorteo extends StatelessWidget {
  const PanelSorteo({
    super.key,
    required this.pasos,
    required this.coincide,
    this.onReproducir,
    this.onVerificacionPublica,
  });
  final List<({String nombre, String hash, String cuando})> pasos;
  final bool? coincide;
  final VoidCallback? onReproducir;
  final VoidCallback? onVerificacionPublica;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        for (var i = 0; i < pasos.length; i += 1)
          Padding(
            padding: const EdgeInsets.only(bottom: Espacio.s2),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                CircleAvatar(
                  radius: Espacio.s3,
                  backgroundColor: t.verdeSolido,
                  child: Text(
                    '${i + 1}',
                    style: texto.labelSmall?.copyWith(
                      color: t.sobreVerdeSolido,
                    ),
                  ),
                ),
                const SizedBox(width: Espacio.s3),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        pasos[i].nombre,
                        style: texto.bodyLarge?.copyWith(color: t.text),
                      ),
                      Text(
                        pasos[i].cuando,
                        style: texto.bodySmall?.copyWith(color: t.text3),
                      ),
                      Text(
                        pasos[i].hash,
                        style: texto.labelSmall?.copyWith(
                          color: t.text2,
                          fontFamily: Fuente.display,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        if (coincide != null)
          ChipEstado(
            texto: coincide!
                ? 'El orden reproducido coincide con el guardado'
                : 'El orden NO coincide',
            tono: coincide! ? Tono.ok : Tono.error,
            icono: coincide! ? Icons.verified : Icons.error_outline,
          ),
        const SizedBox(height: Espacio.s3),
        Row(
          children: [
            if (onReproducir != null)
              Expanded(
                child: Boton(
                  texto: 'Reproducir con la semilla',
                  variante: BotonVariante.secundario,
                  onPressed: onReproducir,
                ),
              ),
            if (onReproducir != null && onVerificacionPublica != null)
              const SizedBox(width: Espacio.s3),
            if (onVerificacionPublica != null)
              Expanded(
                child: Boton(
                  texto: 'Verificar afuera',
                  variante: BotonVariante.fantasma,
                  onPressed: onVerificacionPublica,
                ),
              ),
          ],
        ),
      ],
    );
  }
}
