import 'package:aportaya_diseno/atomos/avatar.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/dinero/formatear.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'abrir_mi_grupo.dart';
import 'acciones_de_grupo.dart';
import 'aviso_de_revision_en_grupos.dart';
import 'dominio/mis_grupos.dart';
import 'dominio/mis_limites.dart';
import 'secciones_de_mis_grupos.dart';
import 'textos.dart';

/// La pestaña «Grupos»: una tarjeta por cada grupo en el que participo, con su nombre.
/// Tocar una abre el estado del aporte de ESE grupo.
class PantallaMisGrupos extends ConsumerWidget {
  const PantallaMisGrupos({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final grupos = ref.watch(misGruposProvider);
    final limites = ref.watch(misLimitesProvider).value;
    return Scaffold(
      appBar: AppBar(title: const Text(TextosPasanaku.tituloMisGrupos)),
      body: SafeArea(
        // Deslizar hacia abajo vuelve a pedir los grupos: después de crear uno o de que
        // te acepten en otro, la lista se pone al día sin salir de la pestaña.
        child: RefreshIndicator(
          onRefresh: () => ref.refresh(misGruposProvider.future),
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(Espacio.s4),
            children: [
              const AvisoDeRevisionEnGrupos(),
              MarcaDeTutorial(
                id: 'pasanaku.mi-estado',
                hijo: EstadoDePantalla<List<GrupoMio>>(
                  valor: grupos,
                  etiquetaDeCarga: TextosPasanaku.cargando,
                  mensajeVacio: TextosPasanaku.sinParticipacion,
                  vacio: (l) => l.isEmpty,
                  reintentar: () => ref.invalidate(misGruposProvider),
                  exito: (lista) =>
                      SeccionesDeMisGrupos(grupos: lista, limites: limites),
                ),
              ),
              const SizedBox(height: Espacio.s4),
              AccionesDeGrupo(limites: limites),
            ],
          ),
        ),
      ),
    );
  }
}

class TarjetaDeMiGrupo extends ConsumerWidget {
  const TarjetaDeMiGrupo({super.key, required this.grupo});
  final GrupoMio grupo;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    final nombre = grupo.nombre ?? TextosPasanaku.grupoSinNombre;
    final (etiqueta, tono) = _estado(grupo.estado);
    final monto = grupo.montoAporte;
    return Tarjeta(
      etiqueta: 'Grupo $nombre, $etiqueta',
      onTap: () => abrirMiGrupo(context, ref, grupo, nombre: nombre),
      child: Row(
        children: [
          Avatar(nombre: nombre),
          const SizedBox(width: Espacio.s3),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  nombre,
                  style: texto.titleMedium?.copyWith(color: t.text),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                Text(
                  grupo.esOrganizador
                      ? TextosPasanaku.organizasEsteGrupo
                      : TextosPasanaku.participasEnEsteGrupo,
                  style: texto.bodySmall?.copyWith(color: t.text3),
                ),
                if (monto != null) ...[
                  const SizedBox(height: Espacio.s1),
                  Text(
                    TextosPasanaku.aportePorPeriodo(
                      formatearMonto(
                        monto: monto,
                        moneda: grupo.moneda ?? 'BOB',
                      ).replaceAll(' ', ' '),
                    ),
                    style: texto.bodySmall?.copyWith(color: t.text2),
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(width: Espacio.s3),
          ChipEstado(texto: etiqueta, tono: tono),
        ],
      ),
    );
  }
}

(String, Tono) _estado(String estado) => switch (estado) {
  'ACTIVO' => ('Activo', Tono.ok),
  'ACEPTADO_PENDIENTE_FIRMA' => ('Falta firmar', Tono.aviso),
  'EN_MORA' => ('Con deuda', Tono.aviso),
  'SUSPENDIDO' => ('Suspendido', Tono.aviso),
  _ => ('En proceso', Tono.info),
};
