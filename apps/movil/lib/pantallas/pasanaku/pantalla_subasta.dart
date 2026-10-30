import 'package:aportaya_cliente_entregas/aportaya_cliente_entregas.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/organismos/pantalla_con_cabecera.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/subasta_del_cupo.dart';
import 'formulario_de_puja.dart';
import 'tarjeta_de_subasta.dart';
import 'textos_de_la_subasta.dart';

/// Una subasta: arriba cómo va (oferta más alta, cuántas, cuándo cierra) y, si sigue
/// abierta y el cupo no es mío, el formulario para ofrecer. Si mi oferta quedó
/// superada, se dice que el saldo ya volvió.
class PantallaSubasta extends ConsumerWidget {
  const PantallaSubasta({super.key, required this.subastaId});
  final String subastaId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final valor = ref.watch(subastaProvider(subastaId));
    return Scaffold(
      body: PantallaConCabecera(
        titulo: TextosDeLaSubasta.cuposEnSubasta,
        alVolver: () =>
            context.canPop() ? context.pop() : context.go('/pasanaku/subastas'),
        cuerpo: RefreshIndicator(
          onRefresh: () => ref.refresh(subastaProvider(subastaId).future),
          child: ListView(
            padding: const EdgeInsets.all(Espacio.s4),
            children: [
              EstadoDePantalla<VistaDeSubasta>(
                valor: valor,
                etiquetaDeCarga: TextosDeLaSubasta.cargandoSubasta,
                mensajeVacio: '',
                reintentar: () => ref.invalidate(subastaProvider(subastaId)),
                exito: (s) => _Detalle(subasta: s),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Detalle extends StatelessWidget {
  const _Detalle({required this.subasta});
  final VistaDeSubasta subasta;

  @override
  Widget build(BuildContext context) {
    final mia = subasta.miPuja?.estado;
    final abierta = subasta.estado == VistaDeSubastaEstadoEnum.ABIERTA;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TarjetaDeSubasta(subasta: subasta),
        const SizedBox(height: Espacio.s4),
        if (mia == MiPujaEstadoEnum.VIGENTE)
          const Alerta(titulo: TextosDeLaSubasta.ganando, tono: Tono.ok),
        if (mia == MiPujaEstadoEnum.LIBERADA ||
            mia == MiPujaEstadoEnum.SUPERADA)
          const Alerta(titulo: TextosDeLaSubasta.teSuperaron, tono: Tono.aviso),
        if (mia != null) const SizedBox(height: Espacio.s3),
        if (subasta.soyElSaliente)
          const Alerta(titulo: TextosDeLaSubasta.esTuCupo)
        else if (abierta)
          FormularioDePuja(subasta: subasta),
      ],
    );
  }
}
