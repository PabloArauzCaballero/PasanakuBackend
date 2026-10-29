import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/moleculas/riel_de_turnos.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/organismos/panel_sorteo.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/cu60_sortear_turnos.dart';
import 'textos.dart';
import 'textos_de_verificacion_del_grupo.dart';

/// CU-60/61 · el turno del grupo, con el sorteo verificable desde la app (gate
/// propio de F5). El paquete del sorteo trae la semilla revelada solo después de
/// `revelarSorteo`; antes viaja nula.
///
/// Esta pantalla no reproduce el orden: por eso no dice que coincide. La comprobación
/// está en la verificación pública, a un toque (ADR-053).
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
              Tarjeta(
                child: RielDeTurnos(
                  total: total,
                  actual: turnoActual,
                  mio: miTurno,
                ),
              ),
              const SizedBox(height: Espacio.s5),
              Text(
                TextosPasanaku.sorteoTitulo,
                style: Tipo.titulo3.copyWith(color: Tokens.of(context).text),
              ),
              const SizedBox(height: Espacio.s1),
              Text(
                TextosDeVerificacionDelGrupo.turnoSorteoExplicacion,
                style: Tipo.cuerpoChico.copyWith(
                  color: Tokens.of(context).text2,
                ),
              ),
              const SizedBox(height: Espacio.s3),
              EstadoDePantalla(
                valor: paquete,
                etiquetaDeCarga: TextosPasanaku.cargando,
                mensajeVacio: 'Todavía no hay sorteo para este grupo.',
                reintentar: () =>
                    ref.invalidate(paqueteDelSorteoProvider(sorteoId)),
                exito: (p) => Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Tarjeta(
                      child: PanelSorteo(
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
                        coincide: null,
                      ),
                    ),
                    const SizedBox(height: Espacio.s4),
                    Boton(
                      texto: TextosDeVerificacionDelGrupo.comprobarSorteo,
                      variante: BotonVariante.secundario,
                      expandido: true,
                      onPressed: () =>
                          context.push('/pasanaku/sorteos/$sorteoId/verificar'),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
