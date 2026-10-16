import 'package:aportaya_diseno/moleculas/riel_de_turnos.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/organismos/panel_sorteo.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/cu60_sortear_turnos.dart';
import 'textos.dart';

/// CU-60/61 · el turno del grupo, con el sorteo verificable desde la app (gate
/// propio de F5). El paquete del sorteo trae la semilla revelada solo después de
/// `revelarSorteo`; antes viaja nula.
class PantallaTurno extends ConsumerWidget {
  const PantallaTurno({
    super.key,
    required this.sorteoId,
    required this.total,
    required this.miTurno,
    required this.turnoActual,
  });
  final String sorteoId;
  final int total;
  final int miTurno;
  final int turnoActual;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final paquete = ref.watch(paqueteDelSorteoProvider(sorteoId));
    return Scaffold(
      appBar: AppBar(title: const Text(TextosPasanaku.tituloTurno)),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              RielDeTurnos(total: total, actual: turnoActual, mio: miTurno),
              const SizedBox(height: Espacio.s5),
              Text(
                TextosPasanaku.sorteoTitulo,
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  color: Tokens.of(context).text,
                ),
              ),
              const SizedBox(height: Espacio.s3),
              EstadoDePantalla(
                valor: paquete,
                etiquetaDeCarga: TextosPasanaku.cargando,
                mensajeVacio: 'Todavía no hay sorteo para este grupo.',
                reintentar: () =>
                    ref.invalidate(paqueteDelSorteoProvider(sorteoId)),
                exito: (p) => PanelSorteo(
                  pasos: [
                    (
                      nombre: 'Compromiso publicado',
                      hash: p.hashComprometido,
                      cuando: 'Antes de sortear',
                    ),
                    if (p.semillaRevelada != null)
                      (
                        nombre: 'Semilla revelada',
                        hash: p.semillaRevelada!,
                        cuando: 'Al revelar',
                      ),
                  ],
                  coincide: p.semillaRevelada == null ? null : true,
                  onVerificacionPublica: () =>
                      context.push('/pasanaku/sorteos/$sorteoId/verificar'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
