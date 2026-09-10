import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Fecha con zona horaria explícita: nada ambiguo en una billetera.
class Fecha extends StatelessWidget {
  const Fecha({super.key, required this.iso, this.conHora = true, this.estilo});
  final String iso;
  final bool conHora;
  final TextStyle? estilo;

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

  /// `2026-09-09T14:30:00Z` → `9 sep 2026, 10:30 (La Paz)`.
  static String formatear(String iso, {bool conHora = true}) {
    final utc = DateTime.parse(iso).toUtc();
    final laPaz = utc.subtract(const Duration(hours: 4));
    final d = '${laPaz.day} ${_meses[laPaz.month - 1]} ${laPaz.year}';
    if (!conHora) return d;
    final hh = laPaz.hour.toString().padLeft(2, '0');
    final mm = laPaz.minute.toString().padLeft(2, '0');
    return '$d, $hh:$mm (La Paz)';
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Text(
      formatear(iso, conHora: conHora),
      style:
          estilo ??
          Theme.of(context).textTheme.bodySmall?.copyWith(color: t.text3),
    );
  }
}
