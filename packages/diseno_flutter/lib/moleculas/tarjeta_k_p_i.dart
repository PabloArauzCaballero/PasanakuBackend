import 'package:flutter/material.dart';

import '../atomos/chip_estado.dart';
import '../atomos/tono.dart';
import '../moleculas/tarjeta.dart';
import '../tokens/tokens.dart';

/// Un indicador con su valor, su variación y —si el dato es provisorio— dicho así.
class TarjetaKPI extends StatelessWidget {
  const TarjetaKPI({
    super.key,
    required this.titulo,
    required this.valor,
    this.variacion,
    this.provisorio = false,
    this.tono = Tono.neutro,
  });
  final String titulo;
  final Widget valor;
  final String? variacion;
  final bool provisorio;
  final Tono tono;

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
              Expanded(
                child: Text(
                  titulo,
                  style: texto.labelLarge?.copyWith(color: t.text2),
                ),
              ),
              if (provisorio)
                const ChipEstado(texto: 'Provisorio', tono: Tono.aviso),
            ],
          ),
          const SizedBox(height: Espacio.s2),
          DefaultTextStyle(
            style: texto.headlineSmall!.copyWith(
              fontFamily: Fuente.display,
              color: t.text,
            ),
            child: valor,
          ),
          if (variacion != null) ...[
            const SizedBox(height: Espacio.s1),
            Text(
              variacion!,
              style: texto.bodySmall?.copyWith(color: tono.coloresDe(t).$2),
            ),
          ],
        ],
      ),
    );
  }
}
