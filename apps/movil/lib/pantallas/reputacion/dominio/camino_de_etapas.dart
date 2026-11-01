import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:aportaya_diseno/atomos/insignia_de_etapa.dart';
import 'package:aportaya_diseno/dinero/formatear.dart';
import 'package:aportaya_diseno/organismos/camino_de_etapas.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/sesion.dart';
import '../../pasanaku/dominio/consultar_puntaje.dart';
import '../textos_de_etapas.dart';
import 'cifras_de_etapa.dart';

/// El catálogo cerrado de etapas (`GET /reputacion/etapas`), en orden ascendente. Lo
/// publica `transparencia`; la app no tiene una copia propia de los tramos.
final catalogoDeEtapasProvider =
    FutureProvider.autoDispose<List<EtapaDelCatalogo>>((ref) async {
      ref.watch(generacionDeSesionProvider);
      final dio = ref.watch(dioProvider);
      try {
        final r = await DefaultApi(dio).listarEtapas();
        return [...?r.data]..sort((a, b) => a.orden.compareTo(b.orden));
      } catch (e) {
        throw errorDeDominio(e);
      }
    });

/// Lo que necesita la pantalla del camino: el puntaje propio (con su etapa y su
/// avance, calculados por el servidor) y el catálogo entero.
class CaminoDelUsuario {
  const CaminoDelUsuario({required this.puntaje, required this.etapas});
  final PuntajeDelUsuario puntaje;
  final List<EtapaDelCatalogo> etapas;

  /// Vacío de verdad: sin catálogo o sin etapa en el puntaje no hay camino que dibujar.
  bool get vacio => etapas.isEmpty || puntaje.etapa == null;
}

final caminoDeEtapasProvider = FutureProvider.autoDispose
    .family<CaminoDelUsuario, String>((ref, usuarioId) async {
      final puntaje = await ref.watch(puntajeProvider(usuarioId).future);
      final etapas = await ref.watch(catalogoDeEtapasProvider.future);
      return CaminoDelUsuario(puntaje: puntaje, etapas: etapas);
    });

/// Solo un desbloqueo `VIGENTE` se muestra: uno `PROPUESTO` espera confirmación de
/// negocio y no se aplica (regla 97.5.4), así que anunciarlo sería prometer algo falso.
String? beneficioDe(DesbloqueoDeEtapa? d) {
  if (d == null || d.estado != DesbloqueoDeEtapaEstadoEnum.VIGENTE) return null;
  return TextosDeEtapas.beneficio(
    d.cuposMax,
    formatearMonto(monto: d.aporteMax, moneda: d.moneda.value),
  );
}

/// El catálogo traducido a peldaños, con el estado de cada uno respecto de la etapa
/// actual. Puro, para probarlo sin red.
List<PeldanoDeEtapa> peldanosDe(
  List<EtapaDelCatalogo> etapas,
  EtapaDelPuntaje actual,
) => [
  for (final a in etapas)
    _peldano(
      a,
      a.orden == actual.orden
          ? EstadoDeEtapa.actual
          : a.orden < actual.orden
          ? EstadoDeEtapa.alcanzada
          : EstadoDeEtapa.bloqueada,
    ),
];

PeldanoDeEtapa _peldano(EtapaDelCatalogo a, EstadoDeEtapa estado) {
  final puntos = umbralLegible(a.desde);
  final leida = TextosDeEtapas.rotuloAccesible(a.orden, a.nombre);
  return PeldanoDeEtapa(
    nombre: TextosDeEtapas.rotulo(a.orden, a.nombre),
    orden: a.orden,
    estado: estado,
    marca: estado == EstadoDeEtapa.actual ? TextosDeEtapas.tuEtapa : null,
    beneficio: beneficioDe(a.desbloqueo),
    detalle: switch (estado) {
      EstadoDeEtapa.actual => TextosDeEtapas.desde(puntos),
      EstadoDeEtapa.alcanzada => TextosDeEtapas.alcanzada(puntos),
      EstadoDeEtapa.bloqueada => TextosDeEtapas.seAbreCon(puntos),
    },
    etiqueta: switch (estado) {
      EstadoDeEtapa.actual => TextosDeEtapas.etiquetaActual(leida, puntos),
      EstadoDeEtapa.alcanzada => TextosDeEtapas.etiquetaAlcanzada(
        leida,
        puntos,
      ),
      EstadoDeEtapa.bloqueada => TextosDeEtapas.etiquetaBloqueada(
        leida,
        puntos,
      ),
    },
  );
}
