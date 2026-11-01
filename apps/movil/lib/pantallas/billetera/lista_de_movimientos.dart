import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/moleculas/aparicion_escalonada.dart';
import 'package:aportaya_diseno/moleculas/fila_de_movimiento.dart' as diseno;
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
          Expanded(
            child: Text(
              TextosBilletera.errorMovimientos,
              style: Tipo.cuerpoChico.copyWith(color: Tokens.of(context).text2),
            ),
          ),
          Boton(
            texto: TextosBilletera.reintentar,
            variante: BotonVariante.enlace,
            onPressed: reintentar,
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

/// Adapta un movimiento del contrato a la fila del sistema de diseño. El contrato solo dice el
/// sentido (no el tipo), así que la fila muestra entra/sale y no adivina por la glosa.
class FilaDeMovimiento extends StatelessWidget {
  const FilaDeMovimiento({super.key, required this.movimiento});

  final MovimientoBilletera movimiento;

  @override
  Widget build(BuildContext context) {
    final esCredito =
        movimiento.sentido == MovimientoBilleteraSentidoEnum.CREDITO;
    final instante = movimiento.registradoEn.toLocal();
    final fecha =
        '${instante.day.toString().padLeft(2, '0')}/'
        '${instante.month.toString().padLeft(2, '0')} · '
        '${instante.hour.toString().padLeft(2, '0')}:'
        '${instante.minute.toString().padLeft(2, '0')}';
    final crudo = movimiento.monto.monto;
    final absoluto = crudo.startsWith('-') ? crudo.substring(1) : crudo;
    return diseno.FilaDeMovimiento(
      concepto: movimiento.glosa,
      // El signo lo da `sentido`; `Dinero` admite «-» y el contrato no dice si viene, así que
      // se normaliza para no escribir nunca «--».
      monto: esCredito ? absoluto : '-$absoluto',
      moneda: movimiento.monto.moneda.value,
      detalle: fecha,
    );
  }
}
