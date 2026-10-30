import 'package:aportaya_cliente_entregas/aportaya_cliente_entregas.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/organismos/pantalla_con_cabecera.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/retirar_subasta.dart';
import 'dominio/subasta_del_cupo.dart';
import 'errores_de_la_subasta.dart';
import 'formulario_de_subasta.dart';
import 'tarjeta_de_subasta.dart';
import 'textos_de_la_subasta.dart';

/// «Subastar mi cupo» (CU-64 por subasta, reemplaza el traspaso con votación). Si el
/// cupo ya tiene una subasta viva o vendida, se ve cómo va; si no (o la anterior cerró
/// sin ofertas o se retiró), el formulario para abrir una.
class PantallaSubastarCupo extends ConsumerWidget {
  const PantallaSubastarCupo({
    super.key,
    required this.grupoId,
    required this.cupoId,
  });
  final String grupoId;
  final String cupoId;

  static const _seVe = {
    VistaDeSubastaEstadoEnum.ABIERTA,
    VistaDeSubastaEstadoEnum.ADJUDICADA,
    VistaDeSubastaEstadoEnum.LIQUIDANDO,
    VistaDeSubastaEstadoEnum.VENDIDA,
  };

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final valor = ref.watch(subastaDelCupoProvider(cupoId));
    return Scaffold(
      body: PantallaConCabecera(
        titulo: TextosDeLaSubasta.subastarMiCupo,
        alVolver: () =>
            context.canPop() ? context.pop() : context.go('/pasanaku'),
        cuerpo: SingleChildScrollView(
          padding: const EdgeInsets.all(Espacio.s4),
          child: EstadoDePantalla<VistaDeSubasta?>(
            valor: valor,
            etiquetaDeCarga: TextosDeLaSubasta.cargandoSubasta,
            mensajeVacio: '',
            vacio: (_) => false,
            reintentar: () => ref.invalidate(subastaDelCupoProvider(cupoId)),
            exito: (s) => s != null && _seVe.contains(s.estado)
                ? _MiSubasta(subasta: s, cupoId: cupoId)
                : FormularioDeSubasta(grupoId: grupoId, cupoId: cupoId),
          ),
        ),
      ),
    );
  }
}

class _MiSubasta extends ConsumerWidget {
  const _MiSubasta({required this.subasta, required this.cupoId});
  final VistaDeSubasta subasta;
  final String cupoId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final retiro = ref.watch(retirarSubastaProvider);
    final puedeRetirar =
        subasta.estado == VistaDeSubastaEstadoEnum.ABIERTA &&
        subasta.cantidadDePujas == 0;
    final rechazo = retiro.hasError ? rechazoDeLaSubasta(retiro.error!) : null;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TarjetaDeSubasta(subasta: subasta),
        const SizedBox(height: Espacio.s4),
        if (rechazo != null) ...[
          Alerta(titulo: rechazo.mensaje, tono: Tono.error),
          const SizedBox(height: Espacio.s3),
        ],
        if (puedeRetirar)
          Boton(
            key: const ValueKey('retirar-subasta'),
            texto: TextosDeLaSubasta.retirar,
            variante: BotonVariante.secundario,
            expandido: true,
            cargando: retiro.isLoading,
            onPressed: retiro.isLoading
                ? null
                : () => ref
                      .read(retirarSubastaProvider.notifier)
                      .retirar(subasta.subastaId, cupoId),
          ),
      ],
    );
  }
}
