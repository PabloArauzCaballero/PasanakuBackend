import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/moleculas/seccion.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../pasanaku/abrir_mi_grupo.dart';
import '../pasanaku/dominio/mis_grupos.dart';
import '../pasanaku/textos.dart';
import 'textos.dart';

/// «Tus pasanakus» en el inicio (2026-10-21): no repite la pestaña Grupos, resume
/// lo que pide atención. Un aviso por grupo que pide algo (tocarlo abre el grupo);
/// si nada pide nada, una línea «todo al día»; sin grupos, cómo entrar a uno.
class ResumenDePasanakus extends ConsumerWidget {
  const ResumenDePasanakus({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return EstadoDePantalla<List<GrupoMio>>(
      valor: ref.watch(misGruposProvider),
      etiquetaDeCarga: TextosBilletera.cargandoPasanakus,
      mensajeVacio: TextosBilletera.sinPasanakus,
      vacio: (_) => false,
      reintentar: () => ref.invalidate(misGruposProvider),
      exito: (grupos) => _Resumen(grupos: grupos),
    );
  }
}

/// Lo que un grupo le pide a la persona, o `null` si no le pide nada.
String? avisoDe(String estado) => switch (estado) {
  'ACEPTADO_PENDIENTE_FIRMA' => TextosBilletera.avisoFirmar,
  'EN_MORA' => TextosBilletera.avisoAtrasado,
  'SUSPENDIDO' => TextosBilletera.avisoSuspendido,
  _ => null,
};

class _Resumen extends ConsumerWidget {
  const _Resumen({required this.grupos});

  final List<GrupoMio> grupos;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final t = Tokens.of(context);
    if (grupos.isEmpty) return const _SinGrupos();
    final conAviso = [
      for (final g in grupos)
        if (avisoDe(g.estado) != null) g,
    ];
    if (conAviso.isEmpty) {
      return Panel(
        hijo: Row(
          children: [
            Expanded(
              child: Text(
                TextosBilletera.todoAlDia(grupos.length),
                style: Tipo.cuerpo.copyWith(color: t.text),
              ),
            ),
            Boton(
              texto: TextosBilletera.verMisGrupos,
              variante: BotonVariante.enlace,
              onPressed: () => context.go('/pasanaku'),
            ),
          ],
        ),
      );
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (final g in conAviso) ...[
          _Aviso(grupo: g),
          const SizedBox(height: Espacio.s2),
        ],
      ],
    );
  }
}

class _Aviso extends ConsumerWidget {
  const _Aviso({required this.grupo});

  final GrupoMio grupo;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final t = Tokens.of(context);
    final nombre = grupo.nombre ?? TextosPasanaku.grupoSinNombre;
    final aviso = avisoDe(grupo.estado)!;
    return Tarjeta(
      etiqueta: '$nombre, $aviso',
      onTap: () => abrirMiGrupo(context, ref, grupo, nombre: nombre),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  nombre,
                  style: Tipo.titulo3.copyWith(color: t.text),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: Espacio.s1),
                ChipEstado(texto: aviso, tono: Tono.aviso),
              ],
            ),
          ),
          Icon(Icons.chevron_right, color: t.text3),
        ],
      ),
    );
  }
}

class _SinGrupos extends StatelessWidget {
  const _SinGrupos();

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Panel(
      hijo: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            TextosBilletera.sinPasanakus,
            style: Tipo.cuerpoChico.copyWith(color: t.text2),
          ),
          const SizedBox(height: Espacio.s3),
          Wrap(
            spacing: Espacio.s2,
            runSpacing: Espacio.s2,
            children: [
              Boton(
                texto: TextosBilletera.unirmeAUnGrupo,
                onPressed: () => context.push('/pasanaku/unirse-a-un-grupo'),
              ),
              Boton(
                texto: TextosBilletera.crearUnGrupo,
                variante: BotonVariante.fantasma,
                onPressed: () => context.push('/pasanaku/grupos/nuevo'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
