import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/moleculas/seccion.dart';
import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';
import 'package:aportaya_diseno/organismos/encabezado_de_saldo.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../pasanaku/pantalla_subastas.dart';
import '../pasanaku/textos_de_la_subasta.dart';
import 'acciones_de_saldo.dart';
import 'lista_de_movimientos.dart';
import 'resumen_de_pasanakus.dart';
import 'textos.dart';

/// La cifra, las acciones principales y el contexto financiero de la billetera.
class CuerpoDeSaldo extends StatelessWidget {
  const CuerpoDeSaldo({
    super.key,
    required this.saldo,
    required this.movimientos,
    required this.reintentarMovimientos,
  });

  final SaldoBilletera saldo;
  final AsyncValue<ListaMovimientosBilletera> movimientos;
  final VoidCallback reintentarMovimientos;

  @override
  Widget build(BuildContext context) {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.only(top: Espacio.s4, bottom: Espacio.s6),
      children: [
        MarcaDeTutorial(
          id: 'billetera.saldo',
          hijo: EncabezadoDeSaldo(
            etiqueta: TextosBilletera.disponible,
            etiquetaHablada: TextosBilletera.saldoDisponible,
            monto: saldo.disponible.monto,
            moneda: saldo.disponible.moneda.value,
            cifras: [
              (
                etiqueta: TextosBilletera.saldoRetenido,
                monto: saldo.retenido.monto,
              ),
            ],
            distintivo: TextosBilletera.custodia,
            acciones: MarcaDeTutorial(
              id: 'billetera.acciones',
              hijo: AccionesDeSaldo(cuentaId: saldo.cuentaId),
            ),
          ),
        ),
        const SizedBox(height: Espacio.s5),
        const TituloDeSeccion(titulo: TextosBilletera.tusPasanakus),
        const ResumenDePasanakus(),
        const SizedBox(height: Espacio.s5),
        // CU-64 por subasta: a todos les llega el cupo que alguien pone en subasta.
        TituloDeSeccion(
          titulo: TextosDeLaSubasta.cuposEnSubasta,
          accion: TextosDeLaSubasta.verTodos,
          alTocarAccion: () => context.push('/pasanaku/subastas'),
        ),
        const ListaDeSubastas(maximo: 3),
        const SizedBox(height: Espacio.s5),
        TituloDeSeccion(
          titulo: TextosBilletera.movimientos,
          accion: TextosBilletera.todos,
          alTocarAccion: () => context.pushNamed(
            'billetera.extracto',
            queryParameters: {'cuenta': saldo.cuentaId},
          ),
        ),
        ListaDeMovimientos(
          valor: movimientos,
          reintentar: reintentarMovimientos,
        ),
      ],
    );
  }
}
