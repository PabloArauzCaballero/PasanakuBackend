import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'acciones_de_grupo.dart';
import 'aviso_de_revision_en_grupos.dart';
import 'dominio/mis_grupos.dart';
import 'dominio/mis_limites.dart';
import 'secciones_de_mis_grupos.dart';
import 'textos.dart';

/// La pestaña «Grupos»: una tarjeta por cada grupo en el que participo (`TarjetaDeMiGrupo`).
/// Tocar una abre el estado del aporte de ESE grupo.
class PantallaMisGrupos extends ConsumerWidget {
  const PantallaMisGrupos({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final grupos = ref.watch(misGruposProvider);
    final limites = ref.watch(misLimitesProvider).value;
    return Scaffold(
      body: SafeArea(
        // Deslizar hacia abajo vuelve a pedir los grupos: después de crear uno o de que
        // te acepten en otro, la lista se pone al día sin salir de la pestaña.
        child: RefreshIndicator(
          onRefresh: () => ref.refresh(misGruposProvider.future),
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(Espacio.s4),
            children: [
              // Título grande como Inicio (ADR-053): las pestañas de primer nivel no
              // llevan barra de fábrica.
              const CabeceraDeInicio(
                saludo: TextosPasanaku.saludoMisGrupos,
                titulo: TextosPasanaku.tituloMisGrupos,
              ),
              const SizedBox(height: Espacio.s4),
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
