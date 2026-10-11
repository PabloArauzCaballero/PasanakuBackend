import 'package:aportaya_diseno/atomos/casilla.dart';
import 'package:aportaya_diseno/dinero/formatear.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos_invitacion.dart';

// El grupo al que invitan y su reglamento: lo que la persona lee antes de aceptar.

/// El grupo al que invitan: nombre, importe y cada cuánto se aporta.
class TarjetaDelGrupo extends StatelessWidget {
  const TarjetaDelGrupo({super.key, required this.detalle});
  final Map<String, dynamic> detalle;

  static const _cadaCuanto = {
    'SEMANAL': 'por semana',
    'QUINCENAL': 'cada quincena',
    'MENSUAL': 'por mes',
    'BIMENSUAL': 'cada dos meses',
  };

  /// «Bs 100,00»; si el servidor mandara un importe fuera del contrato, se muestra tal
  /// cual en vez de romper la pantalla de una invitación.
  String _importe() {
    final monto = '${detalle['montoAporte']}';
    final moneda = '${detalle['moneda']}';
    try {
      return formatearMonto(monto: monto, moneda: moneda);
    } on FormatException {
      return '$monto $moneda';
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Tarjeta(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            detalle['nombre'] as String? ?? '',
            style: Tipo.titulo2.copyWith(color: t.text),
          ),
          const SizedBox(height: Espacio.s2),
          Text(_importe(), style: Tipo.titulo1.copyWith(color: t.brandTexto)),
          Text(
            _cadaCuanto[detalle['periodicidad']] ??
                '${detalle['periodicidad']}'.toLowerCase(),
            style: Tipo.cuerpoChico.copyWith(color: t.text3),
          ),
        ],
      ),
    );
  }
}

/// El reglamento que la persona tiene que leer, y la casilla con la que lo acepta.
class ReglamentoDelGrupo extends StatelessWidget {
  const ReglamentoDelGrupo({
    super.key,
    required this.reglamento,
    required this.acepta,
    required this.alCambiar,
  });
  final String reglamento;
  final bool acepta;
  final ValueChanged<bool> alCambiar;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Tarjeta(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                TextosInvitacion.reglamentoDelGrupo,
                style: Tipo.etiqueta.copyWith(color: t.text2),
              ),
              const SizedBox(height: Espacio.s2),
              Text(reglamento, style: Tipo.cuerpo.copyWith(color: t.text)),
            ],
          ),
        ),
        const SizedBox(height: Espacio.s3),
        Casilla(
          etiqueta: TextosInvitacion.aceptoReglamento,
          valor: acepta,
          onChanged: alCambiar,
        ),
      ],
    );
  }
}
