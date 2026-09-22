import 'package:flutter/material.dart';

import '../atomos/monto.dart';
import '../moleculas/fila_de_movimiento.dart';
import '../organismos/movimiento.dart';
import '../tokens/tokens.dart';

/// El extracto: **agrupado por día**, con el neto del día en la cabecera y **un saldo
/// por día** (en un celular el saldo corrido en cada fila es ruido). Perezoso.
class ListaDeMovimientos extends StatelessWidget {
  const ListaDeMovimientos({
    super.key,
    required this.movimientos,
    required this.moneda,
    this.onTap,
  });
  final List<Movimiento> movimientos;
  final String moneda;
  final void Function(Movimiento m)? onTap;

  /// Suma de cadenas del contrato sin pasar por `double`: centavos enteros.
  static String neto(Iterable<String> montos) {
    var centavos = BigInt.zero;
    for (final m in montos) {
      final negativo = m.startsWith('-');
      final partes = (negativo ? m.substring(1) : m).split('.');
      final valor =
          BigInt.parse(partes[0]) * BigInt.from(100) + BigInt.parse(partes[1]);
      centavos += negativo ? -valor : valor;
    }
    final signo = centavos.isNegative ? '-' : '';
    final abs = centavos.abs();
    final enteros = abs ~/ BigInt.from(100);
    final resto = (abs % BigInt.from(100)).toString().padLeft(2, '0');
    return '$signo$enteros.$resto';
  }

  List<(String dia, List<Movimiento>)> get _porDia {
    final grupos = <String, List<Movimiento>>{};
    for (final m in movimientos) {
      grupos.putIfAbsent(m.fechaIso.substring(0, 10), () => []).add(m);
    }
    final dias = grupos.keys.toList()..sort((a, b) => b.compareTo(a));
    return [for (final d in dias) (d, grupos[d]!)];
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    final grupos = _porDia;
    return ListView.builder(
      itemCount: grupos.length,
      itemBuilder: (context, i) {
        final (dia, lista) = grupos[i];
        final saldoDelDia = lista.first.saldoCorrido;
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.symmetric(
                vertical: Espacio.s2,
                horizontal: Espacio.s1,
              ),
              child: Wrap(
                alignment: WrapAlignment.spaceBetween,
                crossAxisAlignment: WrapCrossAlignment.center,
                spacing: Espacio.s2,
                runSpacing: Espacio.s1,
                children: [
                  Text(
                    _diaLegible(dia),
                    style: texto.labelLarge?.copyWith(color: t.text2),
                  ),
                  Wrap(
                    crossAxisAlignment: WrapCrossAlignment.center,
                    children: [
                      Text(
                        'Neto ',
                        style: texto.bodySmall?.copyWith(color: t.text3),
                      ),
                      Monto(
                        monto: neto(lista.map((m) => m.monto)),
                        moneda: moneda,
                        etiqueta: 'Neto del día',
                        estilo: texto.bodySmall?.copyWith(color: t.text2),
                      ),
                      if (saldoDelDia != null) ...[
                        Text(
                          '  · Saldo ',
                          style: texto.bodySmall?.copyWith(color: t.text3),
                        ),
                        Monto(
                          monto: saldoDelDia,
                          moneda: moneda,
                          etiqueta: 'Saldo al cierre del día',
                          estilo: texto.bodySmall?.copyWith(
                            color: t.brandTexto,
                          ),
                        ),
                      ],
                    ],
                  ),
                ],
              ),
            ),
            for (final m in lista)
              FilaDeMovimiento(
                tipo: m.tipo,
                concepto: m.concepto,
                monto: m.monto,
                moneda: moneda,
                pendiente: m.pendiente,
                onTap: onTap == null ? null : () => onTap!(m),
              ),
            Divider(color: t.border, height: Espacio.s3),
          ],
        );
      },
    );
  }

  static const _meses = [
    'ene',
    'feb',
    'mar',
    'abr',
    'may',
    'jun',
    'jul',
    'ago',
    'sep',
    'oct',
    'nov',
    'dic',
  ];
  static String _diaLegible(String d) {
    final f = DateTime.parse(d);
    return '${f.day} ${_meses[f.month - 1]} ${f.year}';
  }
}
