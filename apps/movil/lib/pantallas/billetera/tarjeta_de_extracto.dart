import 'package:aportaya_diseno/atomos/fecha.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// El extracto generado como un comprobante: el período arriba, el saldo al cierre como cifra
/// protagonista y la cantidad de movimientos debajo. Solo lo que el servidor devuelve.
class TarjetaDeExtracto extends StatelessWidget {
  const TarjetaDeExtracto({
    super.key,
    required this.desde,
    required this.hasta,
    required this.saldoFinal,
    required this.moneda,
    this.cantidadMovimientos,
  });
  final DateTime desde;
  final DateTime hasta;
  final String saldoFinal;
  final String moneda;
  final int? cantidadMovimientos;

  static String _dia(DateTime d) =>
      Fecha.formatear(d.toUtc().toIso8601String(), conHora: false);

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final periodo = '${_dia(desde)} – ${_dia(hasta)}';
    return Tarjeta(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          MergeSemantics(
            child: Row(
              children: [
                Icon(IconosDeMarca.periodo, size: Espacio.s5, color: t.text2),
                const SizedBox(width: Espacio.s2),
                Expanded(
                  child: Text(
                    '${TextosBilletera.periodo}: $periodo',
                    style: Tipo.cuerpoChico.copyWith(color: t.text2),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: Espacio.s4),
          Text(
            TextosBilletera.saldoFinalDelPeriodo,
            style: Tipo.cuerpoChico.copyWith(color: t.text2),
          ),
          const SizedBox(height: Espacio.s1),
          FittedBox(
            fit: BoxFit.scaleDown,
            alignment: Alignment.centerLeft,
            child: Monto(
              monto: saldoFinal,
              moneda: moneda,
              etiqueta: TextosBilletera.saldoFinalDelPeriodo,
              estilo: Tipo.cifraGrande.copyWith(color: t.text),
            ),
          ),
          if (cantidadMovimientos != null) ...[
            const SizedBox(height: Espacio.s4),
            Divider(height: 1, color: t.border),
            const SizedBox(height: Espacio.s3),
            MergeSemantics(
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      TextosBilletera.movimientosDelPeriodo,
                      style: Tipo.cuerpo.copyWith(color: t.text2),
                    ),
                  ),
                  Text(
                    '$cantidadMovimientos',
                    style: Tipo.cifra.copyWith(color: t.text),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}
