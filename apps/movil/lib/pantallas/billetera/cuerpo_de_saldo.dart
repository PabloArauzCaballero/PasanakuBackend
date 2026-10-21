import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/moleculas/seccion.dart';
import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';
import 'package:aportaya_diseno/organismos/encabezado_de_saldo.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

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
            acciones: MarcaDeTutorial(
              id: 'billetera.acciones',
              hijo: AccionesDeSaldo(cuentaId: saldo.cuentaId),
            ),
          ),
        ),
        const Padding(
          padding: EdgeInsets.only(bottom: Espacio.s4),
          child: _NotaDeCustodia(),
        ),
        const TituloDeSeccion(titulo: TextosBilletera.tusPasanakus),
        const ResumenDePasanakus(),
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

class _NotaDeCustodia extends StatelessWidget {
  const _NotaDeCustodia();

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Text(
      TextosBilletera.custodia,
      style: Tipo.ayuda.copyWith(color: t.text3),
    );
  }
}
