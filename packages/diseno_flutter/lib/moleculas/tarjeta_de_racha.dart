import 'package:flutter/material.dart';

import '../atomos/progreso.dart';
import '../moleculas/tarjeta.dart';
import '../tokens/tokens.dart';

/// Meses corridos sin que se pase ninguna cuota. La racha rota se dice sin regañar y
/// con la salida al lado (D-14).
class TarjetaDeRacha extends StatelessWidget {
  const TarjetaDeRacha({
    super.key,
    required this.meses,
    required this.hito,
    this.rota = false,
    this.salida,
  });
  final int meses;

  /// El hito que mide la insignia («Doce meses sin mora»), no un número redondo elegido a dedo.
  final int hito;
  final bool rota;
  final Widget? salida;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Tarjeta(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                rota ? Icons.refresh : Icons.local_fire_department,
                color: rota ? t.text3 : t.accentTexto,
              ),
              const SizedBox(width: Espacio.s2),
              Expanded(
                child: Text(
                  rota
                      ? 'Venías de $meses meses al día'
                      : '$meses ${meses == 1 ? 'mes' : 'meses'} al día',
                  style: texto.titleMedium?.copyWith(
                    fontFamily: Fuente.display,
                    color: t.text,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: Espacio.s2),
          Text(
            rota
                ? 'Poné al día la cuota y arranca otra.'
                : 'Faltan ${(hito - meses).clamp(0, hito)} para «$hito meses sin mora».',
            style: texto.bodyMedium?.copyWith(color: t.text2),
          ),
          const SizedBox(height: Espacio.s3),
          Progreso(
            valor: (meses / hito).clamp(0, 1),
            etiqueta: 'Avance hacia la insignia',
          ),
          if (rota && salida != null) ...[
            const SizedBox(height: Espacio.s3),
            salida!,
          ],
        ],
      ),
    );
  }
}
