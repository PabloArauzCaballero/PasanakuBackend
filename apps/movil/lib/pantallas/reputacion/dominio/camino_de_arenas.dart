import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:aportaya_diseno/atomos/insignia_de_arena.dart';
import 'package:aportaya_diseno/dinero/formatear.dart';
import 'package:aportaya_diseno/organismos/camino_de_arenas.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/sesion.dart';
import '../../pasanaku/dominio/consultar_puntaje.dart';
import '../textos_de_arenas.dart';
import 'cifras_de_arena.dart';

/// El catálogo cerrado de arenas (`GET /reputacion/arenas`), en orden ascendente. Lo
/// publica `transparencia`; la app no tiene una copia propia de los tramos.
final catalogoDeArenasProvider =
    FutureProvider.autoDispose<List<ArenaDelCatalogo>>((ref) async {
      ref.watch(generacionDeSesionProvider);
      final dio = ref.watch(dioProvider);
      try {
        final r = await DefaultApi(dio).listarArenas();
        return [...?r.data]..sort((a, b) => a.orden.compareTo(b.orden));
      } catch (e) {
        throw errorDeDominio(e);
      }
    });

/// Lo que necesita la pantalla del camino: el puntaje propio (con su arena y su
/// avance, calculados por el servidor) y el catálogo entero.
class CaminoDelUsuario {
  const CaminoDelUsuario({required this.puntaje, required this.arenas});
  final PuntajeDelUsuario puntaje;
  final List<ArenaDelCatalogo> arenas;

  /// Vacío de verdad: sin catálogo o sin arena en el puntaje no hay camino que dibujar.
  bool get vacio => arenas.isEmpty || puntaje.arena == null;
}

final caminoDeArenasProvider = FutureProvider.autoDispose
    .family<CaminoDelUsuario, String>((ref, usuarioId) async {
      final puntaje = await ref.watch(puntajeProvider(usuarioId).future);
      final arenas = await ref.watch(catalogoDeArenasProvider.future);
      return CaminoDelUsuario(puntaje: puntaje, arenas: arenas);
    });

/// Solo un desbloqueo `VIGENTE` se muestra: uno `PROPUESTO` espera confirmación de
/// negocio y no se aplica (regla 97.5.4), así que anunciarlo sería prometer algo falso.
String? beneficioDe(DesbloqueoDeArena? d) {
  if (d == null || d.estado != DesbloqueoDeArenaEstadoEnum.VIGENTE) return null;
  return TextosDeArenas.beneficio(
    d.cuposMax,
    formatearMonto(monto: d.aporteMax, moneda: d.moneda.value),
  );
}

/// El catálogo traducido a peldaños, con el estado de cada uno respecto de la arena
/// actual. Puro, para probarlo sin red.
List<PeldanoDeArena> peldanosDe(
  List<ArenaDelCatalogo> arenas,
  ArenaDelPuntaje actual,
) => [
  for (final a in arenas)
    _peldano(
      a,
      a.orden == actual.orden
          ? EstadoDeArena.actual
          : a.orden < actual.orden
          ? EstadoDeArena.alcanzada
          : EstadoDeArena.bloqueada,
    ),
];

PeldanoDeArena _peldano(ArenaDelCatalogo a, EstadoDeArena estado) {
  final puntos = umbralLegible(a.desde);
  return PeldanoDeArena(
    nombre: a.nombre,
    orden: a.orden,
    estado: estado,
    marca: estado == EstadoDeArena.actual ? TextosDeArenas.tuArena : null,
    beneficio: beneficioDe(a.desbloqueo),
    detalle: switch (estado) {
      EstadoDeArena.actual => TextosDeArenas.desde(puntos),
      EstadoDeArena.alcanzada => TextosDeArenas.alcanzada(puntos),
      EstadoDeArena.bloqueada => TextosDeArenas.seAbreCon(puntos),
    },
    etiqueta: switch (estado) {
      EstadoDeArena.actual => TextosDeArenas.etiquetaActual(a.nombre, puntos),
      EstadoDeArena.alcanzada => TextosDeArenas.etiquetaAlcanzada(
        a.nombre,
        puntos,
      ),
      EstadoDeArena.bloqueada => TextosDeArenas.etiquetaBloqueada(
        a.nombre,
        puntos,
      ),
    },
  );
}
