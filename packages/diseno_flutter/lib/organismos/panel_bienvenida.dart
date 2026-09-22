import 'package:flutter/material.dart';

import '../atomos/monto.dart';
import '../moleculas/tarjeta.dart';
import '../tokens/tokens.dart';

/// La portada de una cuenta nueva: el bono y los tres pasos siguientes (D-9).
///
/// [bono] en `null` significa «todavía no se sabe cuánto es», y entonces la frase del
/// bono no se muestra. Antes se le pasaba `'—'` para decir eso mismo y el panel
/// reventaba: `Monto` exige un importe del contrato `Dinero` y un guión no lo es. Una
/// pantalla que no puede decir la cifra tiene que callarse, no romperse.
class PanelBienvenida extends StatelessWidget {
  const PanelBienvenida({
    super.key,
    required this.nombre,
    required this.bono,
    required this.moneda,
    required this.pasos,
  });
  final String nombre;
  final String? bono;
  final String moneda;
  final List<({String texto, bool hecho, VoidCallback onTap})> pasos;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Tarjeta(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Bienvenida, $nombre',
            style: texto.titleLarge?.copyWith(
              fontFamily: Fuente.display,
              color: t.text,
            ),
          ),
          const SizedBox(height: Espacio.s1),
          if (bono != null)
            Row(
              children: [
                Text(
                  'Te acreditamos ',
                  style: texto.bodyMedium?.copyWith(color: t.text2),
                ),
                Monto(
                  monto: bono!,
                  moneda: moneda,
                  etiqueta: 'Bono de bienvenida',
                  estilo: texto.bodyMedium?.copyWith(
                    color: t.okTexto,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                Text(
                  ' de bienvenida.',
                  style: texto.bodyMedium?.copyWith(color: t.text2),
                ),
              ],
            ),
          const SizedBox(height: Espacio.s3),
          // El `Material` transparente va porque `Tarjeta` pinta su fondo con un
          // `DecoratedBox`, y un `ListTile` busca el `Material` más cercano para
          // dibujar su tinta: sin esto la onda del toque queda tapada por la tarjeta
          // —Flutter lo afirma y la pantalla no llega a construirse—.
          for (final p in pasos)
            Material(
              type: MaterialType.transparency,
              child: ListTile(
                contentPadding: EdgeInsets.zero,
                minTileHeight: Tactil.minimo,
                leading: Icon(
                  p.hecho
                      ? Icons.check_circle
                      : Icons.arrow_circle_right_outlined,
                  color: p.hecho ? t.okTexto : t.accentTexto,
                ),
                title: Text(
                  p.texto,
                  style: texto.bodyLarge?.copyWith(
                    color: t.text,
                    decoration: p.hecho ? TextDecoration.lineThrough : null,
                  ),
                ),
                onTap: p.hecho ? null : p.onTap,
              ),
            ),
        ],
      ),
    );
  }
}
