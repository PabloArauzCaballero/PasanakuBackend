import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/dinero/formatear.dart';
import 'package:aportaya_diseno/moleculas/aparicion_escalonada.dart';
import 'package:aportaya_diseno/moleculas/seccion.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'textos.dart';

class ListaDeMovimientos extends StatelessWidget {
  const ListaDeMovimientos({
    super.key,
    required this.valor,
    required this.reintentar,
  });

  final AsyncValue<ListaMovimientosBilletera> valor;
  final VoidCallback reintentar;

  @override
  Widget build(BuildContext context) => valor.when(
    loading: () => Panel(
      hijo: Semantics(
        label: TextosBilletera.cargandoMovimientos,
        child: const LinearProgressIndicator(minHeight: 2),
      ),
    ),
    error: (_, _) => Panel(
      hijo: Row(
        children: [
          const Expanded(child: Text(TextosBilletera.errorMovimientos)),
          TextButton(
            onPressed: reintentar,
            child: const Text(TextosBilletera.reintentar),
          ),
        ],
      ),
    ),
    data: (respuesta) {
      final filas = respuesta.movimientos;
      if (filas.isEmpty) {
        return Panel(
          hijo: Text(
            TextosBilletera.sinMovimientos,
            style: Tipo.cuerpoChico.copyWith(color: Tokens.of(context).text2),
          ),
        );
      }
      final animadas = filas.take(8).toList(growable: false);
      final restantes = filas.skip(8).toList(growable: false);
      return Panel(
        hijo: AparicionEscalonada(
          escalon: const Duration(milliseconds: 30),
          duracionPorHijo: const Duration(milliseconds: 220),
          children: [
            for (final fila in animadas) FilaDeMovimiento(movimiento: fila),
            if (restantes.isNotEmpty)
              Column(
                children: [
                  for (final fila in restantes)
                    FilaDeMovimiento(movimiento: fila),
                ],
              ),
          ],
        ),
      );
    },
  );
}

class FilaDeMovimiento extends StatelessWidget {
  const FilaDeMovimiento({super.key, required this.movimiento});

  final MovimientoBilletera movimiento;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final esCredito =
        movimiento.sentido == MovimientoBilleteraSentidoEnum.CREDITO;
    final instante = movimiento.registradoEn.toLocal();
    final fecha =
        '${instante.day.toString().padLeft(2, '0')}/'
        '${instante.month.toString().padLeft(2, '0')} · '
        '${instante.hour.toString().padLeft(2, '0')}:'
        '${instante.minute.toString().padLeft(2, '0')}';
    final importe = formatearMonto(
      monto: movimiento.monto.monto,
      moneda: movimiento.monto.moneda.value,
    );
    return Semantics(
      label:
          '${esCredito ? 'Crédito' : 'Débito'}: ${movimiento.glosa}, $importe, $fecha',
      child: ExcludeSemantics(
        child: ListTile(
          contentPadding: EdgeInsets.zero,
          leading: Icon(
            esCredito ? Icons.south_west : Icons.north_east,
            // El sentido identifica el tipo de movimiento, no su estado.
            color: t.text2,
          ),
          title: Text(
            movimiento.glosa,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
          subtitle: Text(fecha),
          trailing: Text('${esCredito ? '+' : '−'}$importe'),
          minTileHeight: 56,
        ),
      ),
    );
  }
}
