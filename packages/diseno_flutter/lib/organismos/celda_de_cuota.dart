import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'cuota.dart';
import 'estado_de_cuota.dart';

/// Un día del calendario: relleno **y borde** del estado más urgente, un punto por
/// cuota, y *hoy* con aro (`box-shadow` además del borde, para que gane sin tapar).
class CeldaDeCuota extends StatelessWidget {
  const CeldaDeCuota({
    super.key,
    required this.dia,
    required this.cuotas,
    required this.esHoy,
    required this.elegido,
    this.onTap,
  });

  final int dia;
  final List<Cuota> cuotas;
  final bool esHoy;
  final bool elegido;
  final VoidCallback? onTap;

  static (Color relleno, Color borde) coloresDe(EstadoDeCuota e, Tokens t) =>
      switch (e) {
        EstadoDeCuota.pagada => (t.okBg, t.ok),
        EstadoDeCuota.pendiente => (t.warnBg, t.warn),
        EstadoDeCuota.vencida => (t.errBg, t.err),
        EstadoDeCuota.futura => (Colors.transparent, t.border),
      };

  /// La más urgente manda el color del día.
  static EstadoDeCuota masUrgente(Iterable<EstadoDeCuota> es) {
    const orden = [
      EstadoDeCuota.vencida,
      EstadoDeCuota.pendiente,
      EstadoDeCuota.pagada,
      EstadoDeCuota.futura,
    ];
    for (final o in orden) {
      if (es.contains(o)) return o;
    }
    return EstadoDeCuota.futura;
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    final estado = cuotas.isEmpty
        ? null
        : masUrgente(cuotas.map((c) => c.estado));
    final (relleno, borde) = estado == null
        ? (Colors.transparent, Colors.transparent)
        : coloresDe(estado, t);
    final detalle = cuotas.isEmpty
        ? ''
        : ', ${cuotas.length} cuota${cuotas.length == 1 ? '' : 's'}: '
              '${cuotas.map((c) => '${c.grupo} ${c.estado.name}').join(', ')}';
    return Semantics(
      button: cuotas.isNotEmpty,
      label: '$dia${esHoy ? ', hoy' : ''}$detalle',
      excludeSemantics: true,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(Radios.sm),
        child: Container(
          margin: const EdgeInsets.all(Espacio.s1 / 2),
          decoration: BoxDecoration(
            color: relleno,
            borderRadius: BorderRadius.circular(Radios.sm),
            border: Border.all(
              color: borde,
              width: estado == EstadoDeCuota.futura
                  ? Borde.fino
                  : Borde.foco - Borde.fino,
            ),
            boxShadow: esHoy
                ? [
                    BoxShadow(
                      color: t.brand,
                      spreadRadius: Borde.foco - Borde.fino,
                      blurRadius: 0,
                    ),
                  ]
                : null,
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                '$dia',
                style: texto.labelLarge?.copyWith(
                  color: t.text,
                  fontWeight: elegido ? FontWeight.w700 : FontWeight.w500,
                ),
              ),
              if (cuotas.isNotEmpty)
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    for (final c in cuotas)
                      Container(
                        width: Espacio.s1 + Espacio.s1 / 2,
                        height: Espacio.s1 + Espacio.s1 / 2,
                        margin: const EdgeInsets.symmetric(
                          horizontal: Espacio.s1 / 4,
                        ),
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: coloresDe(c.estado, t).$2,
                        ),
                      ),
                  ],
                ),
            ],
          ),
        ),
      ),
    );
  }
}
