import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:aportaya_diseno/organismos/pantalla_con_cabecera.dart';

import 'dominio/detalle_del_grupo.dart';
import 'seccion_del_grupo.dart';

/// La pantalla de un grupo (2026-10-20): lo que antes era solo «mi estado» ahora
/// muestra el grupo entero —resumen, integrantes, firma y sorteo—. Deslizar hacia
/// abajo vuelve a preguntar.
class PantallaDelGrupo extends ConsumerWidget {
  const PantallaDelGrupo({
    super.key,
    required this.grupoId,
    this.nombre,
    this.participanteId,
    this.usuarioId,
  });
  final String grupoId;
  final String? nombre;
  final String? participanteId;
  final String? usuarioId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final titulo = nombre;
    return Scaffold(
      // Cabecera grande que se compacta al desplazar (ADR-053, H4.S2.M6).
      body: PantallaConCabecera(
        titulo: titulo == null || titulo.isEmpty ? 'Grupo' : titulo,
        cuerpo: SafeArea(
          top: false,
          child: RefreshIndicator(
            onRefresh: () =>
                ref.refresh(detalleDelGrupoProvider(grupoId).future),
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(Espacio.s4),
              children: [SeccionDelGrupo(grupoId: grupoId)],
            ),
          ),
        ),
      ),
    );
  }
}
