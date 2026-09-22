import 'package:flutter/material.dart';

import '../atomos/monto.dart';
import '../tokens/tokens.dart';
import 'celda_de_cuota.dart';
import 'cuota.dart';
import 'estado_de_cuota.dart';

/// Una cuota del día elegido bajo el calendario: grupo, fecha y monto, con el punto
/// del mismo color que la celda. Una futura se ve atenuada: todavía no se debe.
class FilaDeCuota extends StatelessWidget {
  const FilaDeCuota({super.key, required this.cuota, required this.moneda});
  final Cuota cuota;
  final String moneda;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    final futura = cuota.estado == EstadoDeCuota.futura;
    final fecha = cuota.fechaIso.substring(0, 10);
    return ListTile(
      contentPadding: EdgeInsets.zero,
      minTileHeight: Tactil.minimo,
      leading: Container(
        width: Espacio.s3,
        height: Espacio.s3,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: CeldaDeCuota.coloresDe(cuota.estado, t).$2,
        ),
      ),
      title: Text(cuota.grupo, style: texto.bodyLarge?.copyWith(color: t.text)),
      subtitle: Text(
        futura ? 'Todavía no se abre · $fecha' : fecha,
        style: texto.bodySmall?.copyWith(color: t.text3),
      ),
      trailing: Monto(
        monto: cuota.monto,
        moneda: moneda,
        etiqueta: cuota.grupo,
        estilo: texto.bodyLarge?.copyWith(color: futura ? t.text3 : t.text),
      ),
    );
  }
}
