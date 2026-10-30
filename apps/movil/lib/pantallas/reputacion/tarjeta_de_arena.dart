import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/moleculas/barra_de_arena.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'dominio/cifras_de_arena.dart';
import 'textos_de_arenas.dart';

/// La arena actual con su barra hacia la siguiente, tal como la calcula `transparencia`
/// (`consultarPuntaje.arena`). La app no recalcula el avance: lo dibuja.
class TarjetaDeArena extends StatelessWidget {
  const TarjetaDeArena({
    super.key,
    required this.arena,
    required this.puntaje,
    this.sinHistorial = false,
    this.verCamino,
  });

  final ArenaDelPuntaje arena;

  /// El puntaje como lo da el contrato; sin historial no se muestra (nunca un «0»).
  final String puntaje;
  final bool sinHistorial;
  final VoidCallback? verCamino;

  @override
  Widget build(BuildContext context) {
    final siguiente = arena.siguiente;
    final faltan = siguiente == null
        ? null
        : centesimosQueFaltan(puntaje: puntaje, desde: siguiente.desde);
    final enteros = centesimosDe(puntaje);
    return Tarjeta(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          BarraDeArena(
            nombre: arena.nombre,
            orden: arena.orden,
            avance: diezmilesimosDe(arena.progreso) ?? 0,
            subtitulo: sinHistorial || enteros == null
                ? TextosDeArenas.tuArena
                : TextosDeArenas.tuPuntaje(puntosLegibles(enteros)),
            etiquetaDeAvance: siguiente == null
                ? TextosDeArenas.avanceEnLaUltima
                : TextosDeArenas.avanceHacia(siguiente.nombre),
            ordenSiguiente: siguiente == null ? null : arena.orden + 1,
            textoDeAvance: siguiente == null
                ? TextosDeArenas.llegasteArriba
                : sinHistorial
                ? TextosDeArenas.recienEmpezas
                : faltan == null
                ? null
                : TextosDeArenas.faltanPuntos(
                    puntosLegibles(faltan),
                    siguiente.nombre,
                  ),
          ),
          if (verCamino != null) ...[
            const SizedBox(height: Espacio.s3),
            Boton(
              texto: TextosDeArenas.verCamino,
              variante: BotonVariante.secundario,
              expandido: true,
              onPressed: verCamino,
            ),
          ],
        ],
      ),
    );
  }
}
