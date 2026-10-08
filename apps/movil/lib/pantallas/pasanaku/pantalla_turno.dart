import 'package:aportaya_diseno/moleculas/alerta.dart';
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
  const PantallaTurno({super.key, required this.sorteoId});
  final String sorteoId;

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
              const Alerta(
                titulo: TextosPasanaku.turnoSinConfirmarTitulo,
                detalle: TextosPasanaku.turnoSinConfirmarDetalle,
              ),
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
                  // Una semilla revelada no prueba que el orden coincida.
                  // El veredicto solo lo entrega la verificación pública.
                  coincide: null,
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
