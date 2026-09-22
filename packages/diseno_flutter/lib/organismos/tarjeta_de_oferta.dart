import 'package:flutter/material.dart';

import '../atomos/boton.dart';
import '../atomos/boton_variante.dart';
import '../atomos/chip_estado.dart';
import '../atomos/monto.dart';
import '../atomos/tono.dart';
import '../moleculas/tarjeta.dart';
import '../tokens/tokens.dart';

/// La permuta: lo que te dan, lo que te piden y la compensación (D-20).
class TarjetaDeOferta extends StatelessWidget {
  const TarjetaDeOferta({
    super.key,
    required this.quien,
    required this.turnoQueCede,
    required this.turnoQuePide,
    required this.compensacion,
    required this.moneda,
    required this.estado,
    this.onAceptar,
  });
  final String quien;
  final int turnoQueCede;
  final int turnoQuePide;
  final String compensacion;
  final String moneda;
  final String estado;
  final VoidCallback? onAceptar;

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
                  quien,
                  style: texto.titleMedium?.copyWith(color: t.text),
                ),
              ),
              ChipEstado(
                texto: estado,
                tono: estado == 'ACEPTADA' || estado == 'EJECUTADA'
                    ? Tono.ok
                    : estado == 'EN_VALIDACION'
                    ? Tono.aviso
                    : Tono.neutro,
              ),
            ],
          ),
          const SizedBox(height: Espacio.s2),
          Text(
            'Cede el turno $turnoQueCede y pide el $turnoQuePide',
            style: texto.bodyMedium?.copyWith(color: t.text2),
          ),
          Row(
            children: [
              Text(
                'Compensación: ',
                style: texto.bodyMedium?.copyWith(color: t.text2),
              ),
              Monto(
                monto: compensacion,
                moneda: moneda,
                etiqueta: 'Compensación',
                estilo: texto.titleMedium?.copyWith(color: t.brandTexto),
              ),
            ],
          ),
          if (onAceptar != null) ...[
            const SizedBox(height: Espacio.s3),
            Boton(
              texto: 'Aceptar la permuta',
              variante: BotonVariante.primario,
              onPressed: onAceptar,
              expandido: true,
            ),
          ],
        ],
      ),
    );
  }
}
