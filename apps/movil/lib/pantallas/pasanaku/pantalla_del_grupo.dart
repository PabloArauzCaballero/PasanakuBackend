import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/detalle_del_grupo.dart';
import 'seccion_del_grupo.dart';

/// La pantalla de un grupo (2026-10-20): lo que antes era solo «mi estado» ahora
/// muestra el grupo entero —resumen, integrantes, firma y sorteo— y deja el estado
/// de mis aportes a un toque. Deslizar hacia abajo vuelve a preguntar.
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

  static const _misAportes = 'Ver el estado de mis aportes';

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final titulo = nombre;
    final p = participanteId;
    final u = usuarioId;
    return Scaffold(
      appBar: AppBar(
        title: Text(titulo == null || titulo.isEmpty ? 'Grupo' : titulo),
      ),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () => ref.refresh(detalleDelGrupoProvider(grupoId).future),
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(Espacio.s4),
            children: [
              SeccionDelGrupo(grupoId: grupoId),
              if (p != null && p.isNotEmpty && u != null && u.isNotEmpty) ...[
                const SizedBox(height: Espacio.s3),
                Boton(
                  texto: _misAportes,
                  variante: BotonVariante.fantasma,
                  expandido: true,
                  onPressed: () => context.push(
                    Uri(
                      path: '/pasanaku/mi-estado',
                      queryParameters: {
                        'participante': p,
                        'usuario': u,
                        'grupo': grupoId,
                        'nombre': ?titulo,
                      },
                    ).toString(),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
