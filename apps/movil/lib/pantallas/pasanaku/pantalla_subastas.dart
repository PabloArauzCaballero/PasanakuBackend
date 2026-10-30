import 'package:aportaya_cliente_entregas/aportaya_cliente_entregas.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/organismos/pantalla_con_cabecera.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/subasta_del_cupo.dart';
import 'tarjeta_de_subasta.dart';
import 'textos_de_la_subasta.dart';

/// «Cupos en subasta», para cualquier persona con la app: lo que hoy está abierto, la
/// que cierra antes primero. Tocar una abre su detalle para ofrecer.
class PantallaSubastas extends ConsumerWidget {
  const PantallaSubastas({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      body: PantallaConCabecera(
        titulo: TextosDeLaSubasta.cuposEnSubasta,
        alVolver: () => context.canPop() ? context.pop() : context.go('/'),
        cuerpo: RefreshIndicator(
          onRefresh: () => ref.refresh(subastasAbiertasProvider.future),
          child: ListView(
            padding: const EdgeInsets.all(Espacio.s4),
            children: const [ListaDeSubastas(maximo: null)],
          ),
        ),
      ),
    );
  }
}

/// La lista con sus cuatro estados; [maximo] la recorta para el inicio.
class ListaDeSubastas extends ConsumerWidget {
  const ListaDeSubastas({super.key, required this.maximo});
  final int? maximo;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return EstadoDePantalla<List<VistaDeSubasta>>(
      valor: ref.watch(subastasAbiertasProvider),
      etiquetaDeCarga: TextosDeLaSubasta.cargando,
      mensajeVacio: TextosDeLaSubasta.ningunoAhora,
      iconoVacio: IconosDeMarca.grupos,
      vacio: (l) => l.isEmpty,
      reintentar: () => ref.invalidate(subastasAbiertasProvider),
      exito: (lista) => Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          for (final s in maximo == null ? lista : lista.take(maximo!))
            Padding(
              padding: const EdgeInsets.only(bottom: Espacio.s3),
              child: TarjetaDeSubasta(
                subasta: s,
                alTocar: () =>
                    context.push('/pasanaku/subastas/${s.subastaId}'),
              ),
            ),
        ],
      ),
    );
  }
}
