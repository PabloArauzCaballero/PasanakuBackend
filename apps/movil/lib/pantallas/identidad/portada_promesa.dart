import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// Las tres cosas que AportaYa hace distinto, en el orden en que le importan a quien
/// nunca usó un banco: **veo mi rueda**, **el turno no se arregla**, **la plata no la
/// tienen ellos**. Ninguna promete rendimiento ni usa jerga financiera.
class PortadaPromesas extends StatelessWidget {
  const PortadaPromesas({super.key});

  static const _promesas = <({IconData icono, String titulo, String detalle})>[
    (
      icono: Icons.donut_large,
      titulo: TextosIdentidad.portadaRuedaTitulo,
      detalle: TextosIdentidad.portadaRuedaDetalle,
    ),
    (
      icono: Icons.casino_outlined,
      titulo: TextosIdentidad.portadaSorteoTitulo,
      detalle: TextosIdentidad.portadaSorteoDetalle,
    ),
    (
      icono: Icons.account_balance_outlined,
      titulo: TextosIdentidad.portadaCustodiaTitulo,
      detalle: TextosIdentidad.portadaCustodiaDetalle,
    ),
  ];

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      children: [
        for (var i = 0; i < _promesas.length; i++) ...[
          if (i > 0) const SizedBox(height: Espacio.s2),
          _Promesa(promesa: _promesas[i], t: t),
        ],
      ],
    );
  }
}

class _Promesa extends StatelessWidget {
  const _Promesa({required this.promesa, required this.t});

  final ({IconData icono, String titulo, String detalle}) promesa;
  final Tokens t;

  @override
  Widget build(BuildContext context) {
    return MergeSemantics(
      child: Semantics(
        container: true,
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: Tactil.minimo - Espacio.s2,
              height: Tactil.minimo - Espacio.s2,
              decoration: BoxDecoration(
                color: t.brandBg,
                borderRadius: BorderRadius.circular(Radios.md),
              ),
              child: Icon(promesa.icono, size: 20, color: t.brandTexto),
            ),
            const SizedBox(width: Espacio.s3),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    promesa.titulo,
                    style: Tipo.titulo3.copyWith(color: t.text),
                  ),
                  const SizedBox(height: Espacio.s1),
                  Text(
                    promesa.detalle,
                    style: Tipo.cuerpoChico.copyWith(color: t.text2),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
