import 'package:aportaya_diseno/atomos/progreso.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import 'textos_del_alta.dart';

/// Cuánto del contrato se leyó, como barra. Reemplaza el párrafo «deslizá hasta el
/// final»: la persona ve cuánto le falta en vez de tener que adivinarlo.
class BarraDeLectura extends StatelessWidget {
  const BarraDeLectura({
    super.key,
    required this.leido,
    required this.completo,
  });

  final ValueListenable<double> leido;
  final bool completo;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.fromLTRB(
      Espacio.s4,
      Espacio.s3,
      Espacio.s4,
      Espacio.s3,
    ),
    child: ValueListenableBuilder(
      valueListenable: leido,
      builder: (context, valor, _) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Progreso(
            valor: completo ? 1 : valor,
            etiqueta: TextosDelAlta.avisoLeerContrato,
          ),
          const SizedBox(height: Espacio.s2),
          Text(
            completo
                ? TextosDelAlta.contratoLeido
                : TextosDelAlta.avisoLeerContrato,
            style: Tipo.ayuda.copyWith(color: Tokens.of(context).text3),
          ),
        ],
      ),
    ),
  );
}
