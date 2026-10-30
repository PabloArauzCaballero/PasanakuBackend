import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/sesion.dart';
import 'consultar_puntaje.dart';

/// Lo que el formulario de crear grupo puede ofrecer como arena mínima: las arenas
/// del catálogo hasta la propia, incluida, y cuál es la propia (la que vale si la
/// persona no elige nada).
class OpcionesDeArena {
  const OpcionesDeArena({required this.elegibles, required this.propia});
  final List<ArenaDelCatalogo> elegibles;
  final ArenaDelCatalogo? propia;
}

/// Regla de la pantalla, pura para probarla sin red: nadie pide una arena más alta que
/// la suya (`AP-CU20-08`). Sin arena propia conocida no se ofrece nada: no se puede
/// saber el techo, y el servidor usa la arena actual de quien organiza.
OpcionesDeArena opcionesDeArena(
  List<ArenaDelCatalogo> catalogo,
  String? codigoPropio,
) {
  final ordenadas = [...catalogo]..sort((a, b) => a.orden.compareTo(b.orden));
  final propia = ordenadas.where((a) => a.codigo == codigoPropio).firstOrNull;
  if (propia == null) {
    return const OpcionesDeArena(elegibles: [], propia: null);
  }
  return OpcionesDeArena(
    elegibles: [
      for (final a in ordenadas)
        if (a.orden <= propia.orden) a,
    ],
    propia: propia,
  );
}

/// El catálogo cerrado (`GET /reputacion/arenas`) y la arena propia, que viene en el
/// puntaje (`consultarPuntaje.arena`; sin historial es la del puntaje base, nunca la
/// del cero). Lo calcula `transparencia`; la app solo recorta por encima de la propia.
final opcionesDeArenaProvider = FutureProvider.autoDispose<OpcionesDeArena>((
  ref,
) async {
  ref.watch(generacionDeSesionProvider);
  final usuarioId = await ref.watch(sesionProvider).usuarioId();
  try {
    final catalogo = await DefaultApi(ref.read(dioProvider)).listarArenas();
    final puntaje = usuarioId == null
        ? null
        : await ref.watch(puntajeProvider(usuarioId).future);
    return opcionesDeArena(catalogo.data ?? const [], puntaje?.arena?.codigo);
  } catch (e) {
    throw errorDeDominio(e);
  }
});

/// La arena que la persona eligió en el formulario; `null` mientras no toque nada, y
/// entonces el servidor usa la suya (contrato de `crearGrupo.arenaMinima`). La mantiene
/// viva `CrearGrupo` mientras la pantalla exista; al salir se olvida (`autoDispose`).
class ArenaMinimaElegida extends Notifier<String?> {
  @override
  String? build() => null;

  void elegir(String codigo) => state = codigo;
}

final arenaMinimaElegidaProvider =
    NotifierProvider.autoDispose<ArenaMinimaElegida, String?>(
      ArenaMinimaElegida.new,
    );
