import 'package:flutter/material.dart';

import '../atomos/monto.dart';
import '../tokens/tokens.dart';
import 'celda_de_cuota.dart';
import 'cuota.dart';
import 'estado_de_cuota.dart';
import 'fila_de_cuota.dart';
import 'leyenda_de_cuotas.dart';

/// El mes de los aportes por estado de pago (D-12, D-22): la semana empieza el
/// lunes, se navega entre meses, hay «Hoy», dos grupos en un día se ven los dos y el
/// total del mes **cuenta solo lo exigible**. Las celdas y la leyenda son piezas
/// aparte, con las mismas reglas de color.
class CalendarioDeCuotas extends StatelessWidget {
  const CalendarioDeCuotas({
    super.key,
    required this.mes,
    required this.cuotas,
    required this.hoy,
    required this.moneda,
    required this.onMes,
    this.posicion,
    this.diaElegido,
    this.onDia,
  });

  /// Primer día del mes mostrado.
  final DateTime mes;
  final List<Cuota> cuotas;
  final DateTime hoy;
  final String moneda;
  final ValueChanged<DateTime> onMes;

  /// «Mes 6 de 14, hasta que se cierre el último pasanaku».
  final String? posicion;
  final DateTime? diaElegido;
  final ValueChanged<DateTime?>? onDia;

  static const _meses = [
    'enero',
    'febrero',
    'marzo',
    'abril',
    'mayo',
    'junio',
    'julio',
    'agosto',
    'septiembre',
    'octubre',
    'noviembre',
    'diciembre',
  ];

  /// Suma solo lo exigible: una cuota futura no se debe y sumarla infla la deuda.
  static String totalExigible(Iterable<Cuota> cs) {
    var centavos = BigInt.zero;
    for (final c in cs.where(
      (c) =>
          c.estado == EstadoDeCuota.pendiente ||
          c.estado == EstadoDeCuota.vencida,
    )) {
      final p = c.monto.split('.');
      centavos += BigInt.parse(p[0]) * BigInt.from(100) + BigInt.parse(p[1]);
    }
    final resto = (centavos % BigInt.from(100)).toString().padLeft(2, '0');
    return '${centavos ~/ BigInt.from(100)}.$resto';
  }

  /// La más urgente manda el color del día (delegado a la celda; se expone para pruebas).
  static EstadoDeCuota masUrgente(Iterable<EstadoDeCuota> es) =>
      CeldaDeCuota.masUrgente(es);

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    final prefijo = '${mes.year}-${mes.month.toString().padLeft(2, '0')}';
    final delMes = cuotas.where((c) => c.fechaIso.startsWith(prefijo)).toList();
    final porDia = <int, List<Cuota>>{};
    for (final c in delMes) {
      porDia.putIfAbsent(DateTime.parse(c.fechaIso).day, () => []).add(c);
    }
    final primerDiaSemana = (mes.weekday + 6) % 7; // lunes = 0
    final diasDelMes = DateTime(mes.year, mes.month + 1, 0).day;
    final cuenta = {
      for (final e in EstadoDeCuota.values)
        e: delMes.where((c) => c.estado == e).length,
    };
    final elegidas = diaElegido == null
        ? delMes
        : (porDia[diaElegido!.day] ?? const <Cuota>[]);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            IconButton(
              tooltip: 'Mes anterior',
              onPressed: () => onMes(DateTime(mes.year, mes.month - 1)),
              icon: const Icon(Icons.chevron_left),
            ),
            Expanded(
              child: Text(
                '${_meses[mes.month - 1]} ${mes.year}',
                textAlign: TextAlign.center,
                style: texto.titleMedium?.copyWith(
                  fontFamily: Fuente.display,
                  color: t.text,
                ),
              ),
            ),
            IconButton(
              tooltip: 'Mes siguiente',
              onPressed: () => onMes(DateTime(mes.year, mes.month + 1)),
              icon: const Icon(Icons.chevron_right),
            ),
            TextButton(
              onPressed: () => onMes(DateTime(hoy.year, hoy.month)),
              child: const Text('Hoy'),
            ),
          ],
        ),
        if (posicion != null)
          Text(posicion!, style: texto.bodySmall?.copyWith(color: t.text3)),
        const SizedBox(height: Espacio.s2),
        Row(
          children: [
            Expanded(
              child: Text(
                'A pagar este mes',
                style: texto.labelLarge?.copyWith(color: t.text2),
              ),
            ),
            Monto(
              monto: totalExigible(delMes),
              moneda: moneda,
              etiqueta: 'Total exigible del mes',
              estilo: texto.titleMedium?.copyWith(color: t.text),
            ),
          ],
        ),
        const SizedBox(height: Espacio.s2),
        Row(
          children: [
            for (final d in const ['L', 'M', 'M', 'J', 'V', 'S', 'D'])
              Expanded(
                child: Center(
                  child: Text(
                    d,
                    style: texto.labelSmall?.copyWith(color: t.text3),
                  ),
                ),
              ),
          ],
        ),
        GridView.count(
          crossAxisCount: 7,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          childAspectRatio: 1,
          children: [
            for (var i = 0; i < primerDiaSemana; i += 1) const SizedBox(),
            for (var d = 1; d <= diasDelMes; d += 1)
              CeldaDeCuota(
                dia: d,
                cuotas: porDia[d] ?? const [],
                esHoy:
                    hoy.year == mes.year &&
                    hoy.month == mes.month &&
                    hoy.day == d,
                elegido: diaElegido?.day == d && diaElegido?.month == mes.month,
                onTap: (porDia[d] ?? const []).isEmpty || onDia == null
                    ? null
                    : () => onDia!(
                        diaElegido?.day == d
                            ? null
                            : DateTime(mes.year, mes.month, d),
                      ),
              ),
          ],
        ),
        const SizedBox(height: Espacio.s2),
        LeyendaDeCuotas(cuenta: cuenta),
        const SizedBox(height: Espacio.s3),
        for (final c in elegidas) FilaDeCuota(cuota: c, moneda: moneda),
      ],
    );
  }
}
